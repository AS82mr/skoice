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

package net.clementraynaud.skoice.menus;

import net.clementraynaud.skoice.Skoice;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.configuration.ConfigurationSection;

public class MenuField {

    private final Skoice plugin;
    private final String name;
    private final MenuEmoji emoji;
    private final boolean inline;
    private final String value;

    public MenuField(Skoice plugin, String path, String value) {
        this.plugin = plugin;
        ConfigurationSection field = this.plugin.getBot().getFieldsYaml().getConfigurationSection(path);
        this.name = field.getName();
        this.emoji = MenuEmoji.valueOf(field.getString("emoji").toUpperCase());
        this.inline = field.getBoolean("inline");
        this.value = value;
    }

    public MenuField(Skoice plugin, String path) {
        this.plugin = plugin;
        ConfigurationSection field = this.plugin.getBot().getFieldsYaml().getConfigurationSection(path);
        this.name = field.getName();
        this.emoji = MenuEmoji.valueOf(field.getString("emoji").toUpperCase());
        this.inline = field.getBoolean("inline");
        this.value = null;
    }

    public MessageEmbed.Field toField() {
        return new MessageEmbed.Field(this.emoji + this.getTitle(), this.getDescription(), this.inline);
    }

    private String getTitle() {
        return this.plugin.getLang().getMessage("discord.field." + this.name + ".title");
    }

    private String getDescription() {
        if (this.value != null) {
            return this.plugin.getLang().getMessage("discord.field." + this.name + ".description", this.value);
        }
        return this.plugin.getLang().getMessage("discord.field." + this.name + ".description");
    }
}
