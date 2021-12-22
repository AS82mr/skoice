// Copyright 2020, 2021 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
// Copyright 2016, 2017, 2018, 2019, 2020, 2021 Austin "Scarsz" Shapiro

// This file is part of Skoice.

// Skoice is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Skoice is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Skoice.  If not, see <https://www.gnu.org/licenses/>.


package net.clementraynaud;

import net.clementraynaud.configuration.discord.LobbySelection;
import net.clementraynaud.configuration.discord.MessageManagement;
import net.clementraynaud.link.Link;
import net.clementraynaud.link.Unlink;
import net.clementraynaud.system.ChannelManagement;
import net.clementraynaud.system.MarkPlayersDirty;
import net.clementraynaud.system.Network;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import javax.security.auth.login.LoginException;
import java.util.*;

import static net.clementraynaud.Skoice.getPlugin;
import static net.clementraynaud.configuration.discord.MessageManagement.deleteConfigurationMessage;
import static net.clementraynaud.configuration.discord.MessageManagement.initializeDiscordIDDistanceMap;
import static net.clementraynaud.link.Link.*;
import static net.clementraynaud.system.ChannelManagement.networks;
import static net.clementraynaud.util.DataGetters.*;

public class Bot extends ListenerAdapter {

    private static final int TICKS_BETWEEN_VERSION_CHECKING = 720000;

    private static JDA jda;

    public static void setJda(JDA jda) {
        Bot.jda = jda;
    }

    public static JDA getJda() {
        return jda;
    }

    public Bot() {
        if (getPlugin().isTokenSet()) {
            connectBot(null);
        }
    }

    public void connectBot(CommandSender sender) {
        initializeDiscordIDCodeMap();
        initializeDiscordIDDistanceMap();
        byte[] base64TokenBytes = Base64.getDecoder().decode(getPlugin().getConfigFile().getString("token"));
        for (int i = 0; i < base64TokenBytes.length; i++) {
            base64TokenBytes[i]--;
        }
        try {
            setJda(JDABuilder.createDefault(new String(base64TokenBytes))
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build()
                    .awaitReady());
            getPlugin().getLogger().info("Your Discord bot is connected!");
            if (sender != null) {
                getPlugin().updateConfigurationStatus(false);
                if (getPlugin().isBotConfigured()) {
                    sender.sendMessage("§dSkoice §8• §7Your bot is §anow connected§7.");
                } else {
                    sender.sendMessage("§dSkoice §8• §7Your bot is §anow connected§7. Type \"§e/configure§7\" on your Discord server to set it up.");
                }
            }
        } catch (LoginException | InterruptedException e) {
            if (sender == null) {
                getPlugin().getLogger().severe("Your Discord bot could not connect. To update the token, type \"/token\" followed by the new token.");
            } else {
                sender.sendMessage("§dSkoice §8• §7The connection §cfailed§7. Try again with a valid token.");
                getPlugin().getConfigFile().set("token", null);
                getPlugin().saveConfig();
            }
            getPlugin().updateConfigurationStatus(false);
        }
        if (jda != null) {
            deleteConfigurationMessage();
            checkForValidLobby();
            jda.getGuilds().forEach(guild -> {
                if (guild.retrieveCommands().complete().size() < 3) {
                    guild.upsertCommand("configure", "Configure Skoice.").queue();
                    guild.upsertCommand("link", "Link your Discord account to Minecraft.").queue();
                    guild.upsertCommand("unlink", "Unlink your Discord account from Minecraft.").queue();
                }
            });
            jda.addEventListener(this, new LobbySelection(), new MessageManagement(), new Link(), new Unlink());
            if (getPlugin().isBotConfigured()) {
                jda.addEventListener(new ChannelManagement(), new MarkPlayersDirty());
                getJda().getPresence().setActivity(Activity.listening("/link"));
            } else {
                getJda().getPresence().setActivity(Activity.listening("/configure"));
            }
            Bukkit.getScheduler().runTaskLater(getPlugin(), () ->
                            Bukkit.getScheduler().runTaskTimerAsynchronously(
                                    getPlugin(),
                                    ChannelManagement::tick,
                                    0,
                                    5
                            ),
                    0
            );
            Bukkit.getScheduler().runTaskLater(getPlugin(), () ->
                            Bukkit.getScheduler().runTaskTimerAsynchronously(
                                    getPlugin(),
                                    getPlugin()::checkVersion,
                                    TICKS_BETWEEN_VERSION_CHECKING, // Delay before first run
                                    TICKS_BETWEEN_VERSION_CHECKING // Delay between every run
                            ),
                    0
            );
            if (getPlugin().getConfigFile().getString("lobby-id") != null) {
                Category category = getDedicatedCategory();
                if (category != null) {
                    category.getVoiceChannels().stream()
                            .filter(channel -> {
                                try {
                                    //noinspection ResultOfMethodCallIgnored
                                    UUID.fromString(channel.getName());
                                    return true;
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .forEach(channel -> {
                                // temporarily add it as a network so it can be emptied and deleted
                                networks.add(new Network(channel.getId()));
                            });
                }
            }
        }
    }

    private void checkForValidLobby() {
        if (getLobby() == null && getPlugin().getConfigFile().getString("lobby-id") != null) {
            getPlugin().getConfigFile().set("lobby-id", null);
            getPlugin().saveConfig();
            getPlugin().updateConfigurationStatus(false);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Guild guild = event.getGuild();
        if (guild.retrieveCommands().complete().size() < 3) {
            guild.upsertCommand("configure", "Configure Skoice.").queue();
            guild.upsertCommand("link", "Link your Discord account to Minecraft.")
                    .addOption(OptionType.STRING, "minecraft_username", "The username of the Minecraft account you want to link.", true).queue();
            guild.upsertCommand("unlink", "Unlink your Discord account from Minecraft.").queue();
        }
    }
}
