/*
 * Copyright 2020, 2021, 2022 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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

package net.clementraynaud.skoice.system;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.clementraynaud.skoice.Skoice.getPlugin;
import static net.clementraynaud.skoice.config.Config.*;
import static net.clementraynaud.skoice.util.DistanceUtil.*;

public class Network {

    private static final double FALLOFF = 2.5;

    public static final Set<Network> networks = ConcurrentHashMap.newKeySet();
    public static final Set<String> mutedUsers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> players;
    private String channel;
    private boolean initialized = false;

    public Network(String channel) {
        this.players = Collections.emptySet();
        this.channel = channel;
    }

    public static Set<Network> getNetworks() {
        return networks;
    }

    public Network(Set<UUID> players) {
        this.players = players;
        Guild guild = getGuild();
        List<Permission> deniedPermissions = getPlugin().getConfig().getBoolean(CHANNEL_VISIBILITY_FIELD)
                ? Arrays.asList(Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
                : Arrays.asList(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS);
        getCategory().createVoiceChannel(UUID.randomUUID().toString())
                .addPermissionOverride(guild.getPublicRole(),
                        Arrays.asList(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                        deniedPermissions)
                .addPermissionOverride(guild.getSelfMember(),
                        Arrays.asList(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS),
                        Collections.emptyList())
                .setBitrate(guild.getMaxBitrate())
                .queue(channel -> {
                    this.channel = channel.getId();
                    initialized = true;
                }, e -> getNetworks().remove(this));
    }

    public boolean canPlayerBeAdded(Player player) {
        return players.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(p -> !p.equals(player))
                .filter(p -> p.getWorld().getName().equals(player.getWorld().getName()))
                .anyMatch(p -> getVerticalDistance(p.getLocation(), player.getLocation()) <= getVerticalRadius()
                        && getHorizontalDistance(p.getLocation(), player.getLocation()) <= getHorizontalRadius());
    }

    public boolean canPlayerStayConnected(Player player) {
        List<Player> matches = Arrays.asList(players.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(p -> p.getWorld().getName().equals(player.getWorld().getName()))
                .filter(p -> getVerticalDistance(p.getLocation(), player.getLocation()) <= getVerticalRadius() + FALLOFF
                        && getHorizontalDistance(p.getLocation(), player.getLocation()) <= getHorizontalRadius() + FALLOFF)
                .toArray(Player[]::new));
        if (players.size() > matches.size()) {
            Player[] otherPlayers = players.stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .filter(p -> !matches.contains(p))
                    .toArray(Player[]::new);
            for (Player otherPlayer : otherPlayers)
                if (matches.stream()
                        .anyMatch(p -> getVerticalDistance(p.getLocation(), otherPlayer.getLocation()) <= getVerticalRadius() + FALLOFF
                        && getHorizontalDistance(p.getLocation(), otherPlayer.getLocation()) <= getHorizontalRadius() + FALLOFF))
                    return true;
            return false;
        }
        return matches.size() != 1;
    }

    public Network engulf(Network network) {
        players.addAll(network.players);
        network.players.clear();
        return this;
    }

    public void clear() {
        players.clear();
    }

    public void add(UUID uuid) {
        players.add(uuid);
    }

    public void remove(Player player) {
        players.remove(player.getUniqueId());
    }

    public void remove(UUID uuid) {
        players.remove(uuid);
    }

    public boolean contains(Player player) {
        return players.contains(player.getUniqueId());
    }

    public boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    public int size() {
        return players.size();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public VoiceChannel getChannel() {
        if (channel == null || channel.isEmpty())
            return null;
        Guild guild = getGuild();
        if (guild != null)
            return guild.getVoiceChannelById(channel);
        return null;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
