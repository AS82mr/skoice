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

package net.clementraynaud.skoice.storage.config;

import net.clementraynaud.skoice.Skoice;
import net.clementraynaud.skoice.storage.LinksYamlFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OutdatedConfig {

    private final FileConfiguration oldData = new YamlConfiguration();

    private final Skoice plugin;

    public OutdatedConfig(Skoice plugin) {
        this.plugin = plugin;
    }

    public void update() {
        File outdatedConfig = new File(this.plugin.getDataFolder(), "data.yml");
        if (outdatedConfig.exists()) {
            try {
                this.oldData.load(outdatedConfig);
            } catch (IOException | InvalidConfigurationException e) {
                return;
            }
            this.convertOldToken();
            this.convertOldData("mainVoiceChannelID", ConfigField.VOICE_CHANNEL_ID.toString());
            this.convertOldRadius();
            this.convertOldLinks();
            try {
                this.plugin.getLinksYamlFile().loadFromString(this.plugin.getLinksYamlFile().saveToString());
            } catch (InvalidConfigurationException ignored) {
            }
            try {
                Files.delete(outdatedConfig.toPath());
            } catch (IOException ignored) {
            }
            this.plugin.getLogger().info(this.plugin.getLang().getMessage("logger.info.skoice-3"));
        }
    }

    private void convertOldToken() {
        String oldToken = this.oldData.getString("token");
        if (oldToken != null
                && !oldToken.isEmpty()
                && !this.plugin.getConfigYamlFile().contains(ConfigField.TOKEN.toString())) {
            this.plugin.getConfigYamlFile().setToken(oldToken);
        }
    }

    private void convertOldRadius() {
        if (this.oldData.contains("distance.type")
                && "custom".equals(this.oldData.getString("distance.type"))) {
            this.convertOldData("distance.horizontalStrength", ConfigField.HORIZONTAL_RADIUS.toString());
            this.convertOldData("distance.verticalStrength", ConfigField.VERTICAL_RADIUS.toString());
        } else {
            if (!this.plugin.getConfigYamlFile().contains(ConfigField.HORIZONTAL_RADIUS.toString())) {
                this.plugin.getConfigYamlFile().set(ConfigField.HORIZONTAL_RADIUS.toString(), 80);
            }
            if (!this.plugin.getConfigYamlFile().contains(ConfigField.VERTICAL_RADIUS.toString())) {
                this.plugin.getConfigYamlFile().set(ConfigField.VERTICAL_RADIUS.toString(), 40);
            }
        }
    }

    private void convertOldLinks() {
        ConfigurationSection dataSection = this.oldData.getConfigurationSection("Data");
        if (dataSection != null) {
            Map<String, String> links = new HashMap<>();
            Set<String> subkeys = dataSection.getKeys(false);
            Iterator<String> iterator = subkeys.iterator();
            for (int i = 0; i < subkeys.size(); i += 2) {
                links.put(iterator.next(), iterator.next());
            }
            links.putAll(this.plugin.getLinksYamlFile().getLinks());
            this.plugin.getLinksYamlFile().set(LinksYamlFile.LINKS_FIELD, links);
        }
    }

    private void convertOldData(String oldField, String newField) {
        String oldFieldValue = this.oldData.getString(oldField);
        if (oldFieldValue != null
                && !oldFieldValue.isEmpty()
                && !this.plugin.getConfigYamlFile().contains(newField)) {
            this.plugin.getConfigYamlFile().set(newField, this.oldData.get(oldField));
        }
    }
}
