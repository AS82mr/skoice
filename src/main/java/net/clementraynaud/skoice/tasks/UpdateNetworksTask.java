/*
 * Copyright 2020, 2021, 2022, 2023 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
 * Copyright 2016, 2017, 2018, 2019, 2020, 2021 Austin "Scarsz" Shapiro
 *
 * This file is part of Skoice.
 *
 * Skoice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Skoice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Skoice.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.clementraynaud.skoice.tasks;

import net.clementraynaud.skoice.Skoice;
import net.clementraynaud.skoice.storage.config.ConfigField;
import net.clementraynaud.skoice.system.Network;
import net.clementraynaud.skoice.util.DistanceUtil;
import net.clementraynaud.skoice.util.MapUtil;
import net.clementraynaud.skoice.util.PlayerUtil;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class UpdateNetworksTask {

    private static final Set<UUID> eligiblePlayers = new HashSet<>();
    private static final Set<UUID> playersInNetworks = new HashSet<>();
    private static final Map<String, Pair<String, CompletableFuture<Void>>> awaitingMoves = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final Skoice plugin;

    public UpdateNetworksTask(Skoice plugin) {
        this.plugin = plugin;
    }

    public static Set<UUID> getEligiblePlayers() {
        return UpdateNetworksTask.eligiblePlayers;
    }

    public static Set<UUID> getPlayersInNetworks() {
        return UpdateNetworksTask.playersInNetworks;
    }

    public static Map<String, Pair<String, CompletableFuture<Void>>> getAwaitingMoves() {
        return UpdateNetworksTask.awaitingMoves;
    }

    public void run() {
        if (!this.lock.tryLock()) {
            return;
        }
        try {
            VoiceChannel mainVoiceChannel = this.plugin.getConfigYamlFile().getVoiceChannel();
            if (mainVoiceChannel == null) {
                return;
            }
            Network.getNetworks().removeIf(network -> network.getChannel() == null && network.isInitialized());
            Set<UUID> oldEligiblePlayers = new HashSet<>(UpdateNetworksTask.eligiblePlayers);
            UpdateNetworksTask.eligiblePlayers.clear();
            for (UUID minecraftId : oldEligiblePlayers) {
                Player player = this.plugin.getServer().getPlayer(minecraftId);
                if (player != null) {
                    Member member = this.plugin.getLinksYamlFile().getMember(player.getUniqueId());
                    if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                        AudioChannel audioChannel = member.getVoiceState().getChannel();
                        if (audioChannel.getType() == ChannelType.VOICE) {
                            VoiceChannel voiceChannel = (VoiceChannel) audioChannel;
                            boolean isMainVoiceChannel = voiceChannel == mainVoiceChannel;
                            if (!isMainVoiceChannel && (voiceChannel.getParentCategory() == null
                                    || voiceChannel.getParentCategory() != this.plugin.getConfigYamlFile().getCategory())) {
                                Pair<String, CompletableFuture<Void>> pair = UpdateNetworksTask.awaitingMoves.get(member.getId());
                                if (pair != null) {
                                    pair.getRight().cancel(false);
                                }
                                continue;
                            }
                        }
                        this.updateNetworksAroundPlayer(player);
                        if (this.plugin.getConfigYamlFile().getBoolean(ConfigField.ACTION_BAR_ALERT.toString())) {
                            this.sendActionBarAlert(player);
                        }
                        this.createNetworkIfNeeded(player);
                    }
                }
            }
            Set<UUID> players = new HashSet<>(UpdateNetworksTask.playersInNetworks);
            players.addAll(mainVoiceChannel.getMembers().stream()
                    .map(member -> MapUtil.getKeyFromValue(this.plugin.getLinksYamlFile().getLinks(), member.getId()))
                    .filter(Objects::nonNull)
                    .map(UUID::fromString)
                    .collect(Collectors.toSet()));
            for (UUID minecraftId : players) {
                Network playerNetwork = Network.getNetworks().stream()
                        .filter(network -> network.contains(minecraftId))
                        .findAny().orElse(null);
                VoiceChannel shouldBeInChannel;
                if (playerNetwork != null) {
                    if (playerNetwork.getChannel() == null) {
                        continue;
                    }
                    shouldBeInChannel = playerNetwork.getChannel();
                } else {
                    shouldBeInChannel = mainVoiceChannel;
                }
                Member member = this.plugin.getLinksYamlFile().getMember(minecraftId);
                Pair<String, CompletableFuture<Void>> awaitingMove = UpdateNetworksTask.awaitingMoves.get(member.getId());
                if (awaitingMove != null && awaitingMove.getLeft().equals(shouldBeInChannel.getId())) {
                    continue;
                }
                if (awaitingMove != null && !awaitingMove.getLeft().equals(shouldBeInChannel.getId())
                        && !awaitingMove.getRight().cancel(false)) {
                    continue;
                }
                GuildVoiceState voiceState = member.getVoiceState();
                if (voiceState != null && voiceState.getChannel() != shouldBeInChannel) {
                    UpdateNetworksTask.awaitingMoves.put(member.getId(), Pair.of(
                            shouldBeInChannel.getId(),
                            this.plugin.getBot().getGuild().moveVoiceMember(member, shouldBeInChannel)
                                    .submit().whenCompleteAsync((v, t) -> UpdateNetworksTask.awaitingMoves.remove(member.getId()))
                    ));
                }
            }
            this.deleteEmptyNetworks();
        } finally {
            this.lock.unlock();
        }
    }

    private void updateNetworksAroundPlayer(Player player) {
        Network.getNetworks().stream()
                .filter(network -> network.canPlayerBeAdded(player))
                .reduce((network1, network2) -> network1.size() > network2.size()
                        ? network1.engulf(network2)
                        : network2.engulf(network1))
                .filter(network -> !network.contains(player.getUniqueId()))
                .ifPresent(network -> network.add(player.getUniqueId()));
        Network.getNetworks().stream()
                .filter(network -> network.contains(player.getUniqueId()))
                .filter(network -> !network.canPlayerStayConnected(player))
                .forEach(network -> {
                    network.remove(player.getUniqueId());
                    if (network.size() == 1) {
                        network.clear();
                    }
                });
    }

    private void sendActionBarAlert(Player player) {
        Network.getNetworks().stream()
                .filter(network -> network.contains(player.getUniqueId()))
                .filter(network -> network.canPlayerStayConnected(player))
                .filter(network -> !network.canPlayerBeAdded(player))
                .forEach(network -> this.plugin.adventure().player(player).sendActionBar(
                                Component.text(ChatColor.translateAlternateColorCodes('&',
                                                this.plugin.getLang().getMessage("minecraft.action-bar.alert")
                                        )
                                )
                        )
                );
    }

    private void createNetworkIfNeeded(Player player) {
        Set<Player> alivePlayers = PlayerUtil.getOnlinePlayers().stream()
                .filter(p -> !p.isDead())
                .collect(Collectors.toSet());
        Category category = this.plugin.getConfigYamlFile().getCategory();
        Set<UUID> playersWithinRange = alivePlayers.stream()
                .filter(p -> Network.getNetworks().stream().noneMatch(network -> network.contains(p.getUniqueId())))
                .filter(p -> !p.equals(player))
                .filter(p -> p.getWorld().getName().equals(player.getWorld().getName()))
                .filter(p -> DistanceUtil.getHorizontalDistance(p.getLocation(),
                        player.getLocation()) <= this.plugin.getConfigYamlFile().getInt(ConfigField.HORIZONTAL_RADIUS.toString())
                        && DistanceUtil.getVerticalDistance(p.getLocation(),
                        player.getLocation()) <= this.plugin.getConfigYamlFile().getInt(ConfigField.VERTICAL_RADIUS.toString()))
                .filter(p -> {
                    Member member = this.plugin.getLinksYamlFile().getMember(p.getUniqueId());
                    return member != null && member.getVoiceState() != null
                            && member.getVoiceState().getChannel() instanceof VoiceChannel
                            && ((VoiceChannel) member.getVoiceState().getChannel()).getParentCategory() != null
                            && ((VoiceChannel) member.getVoiceState().getChannel()).getParentCategory().equals(category);
                })
                .map(Player::getUniqueId)
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        if (!playersWithinRange.isEmpty() && category.getChannels().size() != 50) {
            playersWithinRange.add(player.getUniqueId());
            Network network = new Network(this.plugin, playersWithinRange);
            network.build();
            Network.getNetworks().add(network);
        }
    }

    private void deleteEmptyNetworks() {
        for (Network network : Network.getNetworks()) {
            if (network.isEmpty()) {
                VoiceChannel voiceChannel = network.getChannel();
                if (voiceChannel != null && voiceChannel.getMembers().isEmpty()) {
                    voiceChannel.delete().reason(this.plugin.getLang().getMessage("discord.communication-lost")).queue();
                    Network.getNetworks().remove(network);
                }
            }
        }
    }
}
