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

package net.clementraynaud.skoice.listeners.guild;

import net.clementraynaud.skoice.lang.DiscordLang;
import net.clementraynaud.skoice.lang.MinecraftLang;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.UUID;

import static net.clementraynaud.skoice.config.Config.*;
import static net.clementraynaud.skoice.listeners.player.DirtyPlayerListeners.markDirty;
import static net.clementraynaud.skoice.networks.NetworkManager.updateMutedUsers;

public class GuildVoiceJoinListener extends ListenerAdapter {

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        updateMutedUsers(event.getChannelJoined(), event.getMember());
        if (!event.getChannelJoined().equals(getLobby())) return;
        String minecraftID = getKeyFromValue(getLinkMap(), event.getMember().getId());
        if (minecraftID == null) {
            try {
                event.getMember().getUser().openPrivateChannel().complete()
                        .sendMessageEmbeds(new EmbedBuilder().setTitle(":link: " + DiscordLang.LINKING_PROCESS_EMBED_TITLE)
                                .addField(":warning: " + DiscordLang.ACCOUNT_NOT_LINKED_FIELD_TITLE, DiscordLang.ACCOUNT_NOT_LINKED_FIELD_ALTERNATIVE_DESCRIPTION.toString().replace("{discordServer}", event.getGuild().getName()), false)
                                .setColor(Color.RED).build()).queue(success -> {
                        }, failure -> {
                        });
            } catch (ErrorResponseException ignored) {
            }
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(minecraftID));
            if (player.isOnline()) {
                markDirty(player.getPlayer());
                player.getPlayer().sendMessage(MinecraftLang.CONNECTED_TO_PROXIMITY_VOICE_CHAT.toString());
            }
        }
    }
}
