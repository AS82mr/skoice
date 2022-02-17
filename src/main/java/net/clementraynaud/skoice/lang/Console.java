/*
 * Copyright 2020, 2021, 2022 Clément "carlodrift" Raynaud, Lucas "Lucas_Cdry" Cadiry and contributors
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

package net.clementraynaud.skoice.lang;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.bukkit.ChatColor;

import java.util.Map;

import static net.clementraynaud.skoice.Skoice.getPlugin;

public enum Console {
    PLUGIN_ENABLED_INFO(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, "Plugin enabled.",
            Lang.FR, "Plugin activé."))),

    PLUGIN_DISABLED_INFO(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, "Plugin disabled.",
            Lang.FR, "Plugin désactivé."))),

    CONFIGURATION_COMPLETE_INFO(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, "Configuration complete. Type \"/link\" on your Discord server to link your Discord account to Minecraft.",
            Lang.FR, "Configuration terminée. Tapez \"/link\" sur votre serveur Discord pour relier votre compte Discord à Minecraft."))),

    BOT_CONNECTED_INFO(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, "Your bot is connected.",
            Lang.FR, "Votre bot est connecté."))),

    OUTDATED_VERSION_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "You are using an outdated version ({runningVersion}). Download the latest version ({latestVersion}) here: " + ChatColor.AQUA + "https://www.spigotmc.org/resources/skoice-proximity-voice-chat.82861/" + ChatColor.YELLOW + ".",
            Lang.FR, ChatColor.YELLOW + "Vous utilisez une version obsolète ({runningVersion}). Téléchargez la dernière version ({latestVersion}) ici : " + ChatColor.AQUA + "https://www.spigotmc.org/resources/skoice-proximity-voice-chat.82861/" + ChatColor.YELLOW + "."))),

    NO_TOKEN_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "Token not set. Join your Minecraft server to set up Skoice.",
            Lang.FR, ChatColor.YELLOW + "Token non défini. Rejoignez votre serveur Minecraft pour configurer Skoice."))),

    NO_LANGUAGE_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "Language not set. Type \"/configure\" on your Discord server to set up Skoice."))),

    MULTIPLE_GUILDS_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "Your bot is on multiple Discord servers. Type \"/configure\" on your Discord server to choose one.",
            Lang.FR, ChatColor.YELLOW + "Votre bot est sur plusieurs serveurs Discord. Tapez \"/configure\" sur votre serveur Discord pour en choisir un."))),

    NO_LOBBY_ID_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "Lobby not set. Type \"/configure\" on your Discord server to set up Skoice.",
            Lang.FR, ChatColor.YELLOW + "Salon vocal principal non défini. Tapez \"/configure\" sur votre serveur Discord pour configurer Skoice."))),

    NO_DISTANCES_WARNING(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.YELLOW + "Radius not set. Type \"/configure\" on your Discord server to set up Skoice.",
            Lang.FR, ChatColor.YELLOW + "Rayons non définis. Tapez \"/configure\" sur votre serveur Discord pour configurer Skoice."))),

    MISSING_ACCESS_ERROR(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.RED + "You did not grant your bot the permission to register commands on your Discord server (\"applications.commands\").",
            Lang.FR, ChatColor.RED + "Vous n'avez pas accordé à votre bot la permission de déclarer des commandes sur votre serveur Discord (\"applications.commands\")."))),

    BOT_COULD_NOT_CONNECT_ERROR(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, ChatColor.RED + "Your bot could not connect. To update the token, type \"/token\" followed by the new token.",
            Lang.FR, ChatColor.RED + "Votre bot n'a pas pu se connecter. Pour mettre à jour le token, tapez \"/token\" suivi du nouveau token."))),

    UNEXPECTED_VALUE(Maps.newHashMap(ImmutableMap.of(
            Lang.EN, "Unexpected value: {value}",
            Lang.FR, "Valeur inattendue : {value}")));

    final Map<Lang, String> messages;

    Console(Map<Lang, String> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        String language = getPlugin().getConfig().getString("language");
        return messages.getOrDefault(language == null ? Lang.EN : Lang.valueOf(language), messages.get(Lang.EN));
    }
}
