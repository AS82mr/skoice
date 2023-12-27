/*
 * Copyright 2020, 2021, 2022, 2023 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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

package net.clementraynaud.skoice.commands;

import net.clementraynaud.skoice.Skoice;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class InviteCommand extends Command {


    public InviteCommand(Skoice plugin, CommandExecutor executor, SlashCommandInteractionEvent event) {
        super(plugin, executor, CommandInfo.INVITE.isServerManagerRequired(), CommandInfo.INVITE.isBotReadyRequired(), event);
    }

    @Override
    public void run() {
        super.event.reply(super.plugin.getBot().getMenu("skoice-proximity-voice-chat").build())
                .setEphemeral(true).queue();
    }
}
