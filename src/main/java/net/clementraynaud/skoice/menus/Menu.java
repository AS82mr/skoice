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

package net.clementraynaud.skoice.menus;

import net.clementraynaud.skoice.Skoice;
import net.clementraynaud.skoice.bot.BotStatus;
import net.clementraynaud.skoice.menus.selectmenus.SelectMenu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Menu {

    private static final int MAX_BUTTON_ROW_LENGTH = 5;

    private final Skoice plugin;
    private final String menuId;
    private final String section;
    private final String footer;
    private final MenuEmoji emoji;
    private final MenuType type;
    private final MenuStyle style;
    private final String parent;
    private final String[] fields;
    private Button[] buttons;

    public Menu(Skoice plugin, ConfigurationSection menu) {
        this.plugin = plugin;
        this.menuId = menu.getName();
        this.section = !menu.getParent().equals(menu.getRoot()) ? menu.getParent().getName() : this.menuId;
        this.footer = !menu.getParent().equals(menu.getRoot())
                ? menu.getParent().getString("footer")
                : menu.getString("footer");
        this.emoji = MenuEmoji.valueOf(!menu.getParent().equals(menu.getRoot())
                ? menu.getParent().getString("emoji").toUpperCase()
                : menu.getString("emoji").toUpperCase());
        this.type = menu.contains("type") ? MenuType.valueOf(menu.getString("type").toUpperCase()) : null;
        this.style = menu.contains("style") ? MenuStyle.valueOf(menu.getString("style").toUpperCase()) : null;
        this.parent = menu.contains("parent") ? menu.getString("parent") : null;
        this.fields = menu.getStringList("fields").toArray(new String[0]);

        this.buttons = new Button[0];
    }

    public MessageCreateData build(String... args) {
        return new MessageCreateBuilder().setEmbeds(this.getEmbed(args))
                .setComponents(this.getActionRows()).build();
    }

    private String getTitle(boolean withEmoji) {
        return withEmoji ? this.emoji + this.plugin.getBot().getLang().getMessage("menu." + this.section + ".title") :
                this.plugin.getBot().getLang().getMessage("menu." + this.section + ".title");
    }

    private String getDescription(boolean shortened) {
        if (shortened && this.plugin.getBot().getLang().contains("menu." + this.section + ".shortened-description")) {
            return this.plugin.getBot().getLang().getMessage("menu." + this.section + ".shortened-description");
        } else if (this.plugin.getBot().getLang().contains("menu." + this.section + ".description")) {
            return this.plugin.getBot().getLang().getMessage("menu." + this.section + ".description");
        }
        return null;
    }

    private MessageEmbed getEmbed(String... args) {
        EmbedBuilder embed = new EmbedBuilder().setTitle(this.getTitle(true))
                .setColor(this.type.getColor());

        if (this.footer != null) {
            embed.setFooter(this.plugin.getBot().getLang().getMessage("menu." + this.footer + "-footer"),
                    "https://clementraynaud.net/Skoice.jpeg");
        }

        if (this.getDescription(false) != null) {
            embed.setDescription(this.getDescription(false));
        }

        if (this.plugin.getBot().getStatus() == BotStatus.READY) {
            StringBuilder author = new StringBuilder();
            String parentMenu = this.parent;
            while (parentMenu != null) {
                Menu menuParent = this.plugin.getBot().getMenuFactory().getMenu(parentMenu);
                author.insert(0, menuParent.getTitle(false) + " › ");
                parentMenu = menuParent.parent;
            }
            embed.setAuthor(author.toString());
        }

        List<Menu> children = this.getChildren();
        for (Menu child : children) {
            String description = child.getDescription(true);
            if (description == null) {
                description = child.getChildren().stream()
                        .map(menu -> "> " + menu.getTitle(true))
                        .collect(Collectors.joining("\n"));
            }
            embed.addField(child.getTitle(true), description, true);
        }

        int startIndex = 0;
        for (String field : this.fields) {
            MenuField menuField = this.plugin.getBot().getMenuFactory().getField(field);
            int endIndex = this.plugin.getBot().getLang().getAmountOfArgsRequired(menuField.getDescription());
            embed.addField(menuField.build(Arrays.copyOfRange(args, startIndex, endIndex)));
            startIndex = endIndex;
        }
        return embed.build();
    }

    private List<ActionRow> getActionRows() {
        List<ActionRow> actionRows = new ArrayList<>();

        ActionRow selectMenuActionRow = this.getSelectMenuActionRow();
        if (selectMenuActionRow != null) {
            actionRows.add(selectMenuActionRow);
        }

        actionRows.addAll(this.getMainActionRows());
        actionRows.add(this.getSecondaryActionRow());

        return actionRows;
    }

    private List<ActionRow> getMainActionRows() {
        List<ActionRow> mainActionRows = new ArrayList<>();
        List<Button> mainButtons = new ArrayList<>(Arrays.asList(this.buttons));
        for (Menu menu : this.plugin.getBot().getMenuFactory().getMenus().values()) {
            if (menu.parent != null && menu.parent.equals(this.menuId)) {
                mainButtons.add(menu.style == MenuStyle.PRIMARY
                        ? Button.primary(menu.menuId, menu.getTitle(false))
                        .withEmoji(menu.emoji.get())
                        : Button.secondary(menu.menuId, menu.getTitle(false))
                        .withEmoji(menu.emoji.get()));
            }
        }

        for (int i = 0; i < mainButtons.size(); i += Menu.MAX_BUTTON_ROW_LENGTH) {
            mainActionRows.add(ActionRow.of(mainButtons.subList(i, Math.min(i + 5, mainButtons.size()))));
        }

        return mainActionRows;
    }

    private ActionRow getSelectMenuActionRow() {
        SelectMenu selectMenu = this.plugin.getBot()
                .getMenuFactory()
                .getSelectMenuFactory()
                .getSelectMenu(this.plugin, this.menuId);
        if (selectMenu == null) {
            return null;
        }
        return ActionRow.of(selectMenu.get());
    }

    private ActionRow getSecondaryActionRow() {
        List<Button> secondaryButtons = new ArrayList<>();
        String root = this.getRoot();

        if (root.equals("settings")
                && (this.plugin.getBot().getStatus() == BotStatus.READY || "language".equals(this.menuId))) {
            String backButtonId = this.parent == null ? "unreachable" : this.parent;
            Button backButton = Button.secondary(backButtonId, "← " + this.plugin.getBot().getLang().getMessage("button-label.back"));
            if (this.parent == null) {
                backButton = backButton.withId("unreachable").asDisabled();
            }
            secondaryButtons.add(backButton);
        }

        secondaryButtons.add(Button.secondary("display-issues",
                        this.plugin.getBot().getLang().getMessage("button-label.display-issues"))
                .withEmoji(MenuEmoji.QUESTION.get()));
        secondaryButtons.add(Button.link("https://discord.gg/skoice-proximity-voice-chat-741375523275407461",
                        this.plugin.getBot().getLang().getMessage("button-label.support-server"))
                .withEmoji(MenuEmoji.SCREWDRIVER.get()));
        if (root.equals("settings")
                && this.plugin.getBot().getStatus() != BotStatus.READY
                && !"language".equals(this.menuId)) {
            Menu languageMenu = this.plugin.getBot().getMenuFactory().getMenu("language");
            secondaryButtons.add(Button.secondary(languageMenu.menuId, languageMenu.getTitle(false))
                    .withEmoji(MenuEmoji.GLOBE_WITH_MERIDIANS.get()));
        }

        return ActionRow.of(secondaryButtons);
    }

    public void setButtons(Button... buttons) {
        this.buttons = buttons;
    }

    private String getRoot() {
        Menu root = this;
        while (root.parent != null) {
            root = this.plugin.getBot().getMenuFactory().getMenu(root.parent);
        }
        return root.menuId;
    }

    private List<Menu> getChildren() {
        return this.plugin.getBot().getMenuFactory().getMenus().values().stream()
                .filter(menu -> menu.parent != null)
                .filter(menu -> menu.parent.equals(this.menuId))
                .collect(Collectors.toList());
    }

    public String getId() {
        return this.menuId;
    }
}
