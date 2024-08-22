/*
 * Copyright 2020, 2021, 2022, 2023, 2024 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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
import net.clementraynaud.skoice.system.Network;
import net.clementraynaud.skoice.system.Networks;
import net.clementraynaud.skoice.system.ProximityChannel;
import net.clementraynaud.skoice.system.ProximityChannels;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.concurrent.CompletableFuture;

public class InterruptSystemTask {

    private final Skoice plugin;

    public InterruptSystemTask(Skoice plugin) {
        this.plugin = plugin;
    }

    public void run() {
        for (Pair<String, CompletableFuture<Void>> value : UpdateNetworksTask.getAwaitingMoves().values()) {
            value.getRight().cancel(true);
        }

        VoiceChannel voiceChannel = this.plugin.getConfigYamlFile().getVoiceChannel();

        for (ProximityChannel proximityChannel : ProximityChannels.getInitialized()) {
            if (voiceChannel != null) {
                for (int i = 0; i < proximityChannel.getChannel().getMembers().size(); i++) {
                    Member member = proximityChannel.getChannel().getMembers().get(i);
                    if (i + 1 < proximityChannel.getChannel().getMembers().size()) {
                        member.getGuild()
                                .moveVoiceMember(member, voiceChannel)
                                .queue();
                    } else {
                        member.getGuild()
                                .moveVoiceMember(member, voiceChannel)
                                .complete();
                    }
                }
            }
            proximityChannel.delete("system-interrupted");
        }

        Networks.getAll().forEach(Network::clear);
        Networks.clear();
    }
}
