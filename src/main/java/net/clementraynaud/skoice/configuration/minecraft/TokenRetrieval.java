/*
 * Copyright 2020, 2021 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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

package net.clementraynaud.skoice.configuration.minecraft;

import net.clementraynaud.skoice.util.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

import static net.clementraynaud.skoice.Skoice.getBot;
import static net.clementraynaud.skoice.Skoice.getPlugin;
import static net.clementraynaud.skoice.bot.Connection.*;

public class TokenRetrieval implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.isOp()) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Lang.Minecraft.NO_TOKEN.print());
            return true;
        }
        if (args[0].length() != 59 || !args[0].matches("[a-zA-Z0-9_.]+")) {
            sender.sendMessage(Lang.Minecraft.INVALID_TOKEN.print());
            return true;
        }
        byte[] tokenBytes = args[0].getBytes();
        for (int i = 0; i < tokenBytes.length; i++) {
            tokenBytes[i]++;
        }
        String base64Token = Base64.getEncoder().encodeToString(tokenBytes);
        getPlugin().getConfigFile().set("token", base64Token);
        getPlugin().saveConfig();
        getPlugin().setTokenBoolean(true);
        if (getJda() == null) {
            getBot().connectBot(false, sender);
        } else {
            sender.sendMessage(Lang.Minecraft.BOT_ALREADY_CONNECTED.print());
        }
        return true;
    }
}
