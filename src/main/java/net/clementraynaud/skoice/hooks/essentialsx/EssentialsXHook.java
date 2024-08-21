package net.clementraynaud.skoice.hooks.essentialsx;

import net.clementraynaud.skoice.Skoice;
import net.essentialsx.api.v2.services.discord.DiscordService;
import net.essentialsx.api.v2.services.discordlink.DiscordLinkService;
import net.essentialsx.discordlink.EssentialsDiscordLink;

public class EssentialsXHook {

    private final Skoice plugin;
    private EssentialsXHookImpl essentialsXHookImpl;

    public EssentialsXHook(Skoice plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (this.essentialsXHookImpl != null) {
            return;
        }
        try {
            this.plugin.getServer().getServicesManager().load(DiscordLinkService.class).hashCode();
            this.plugin.getServer().getServicesManager().load(DiscordService.class).hashCode();
            ((EssentialsDiscordLink) this.plugin.getServer().getPluginManager().getPlugin("EssentialsDiscordLink")).getAccountStorage().hashCode();
            EssentialsXHookImpl essentialsXHookImpl = new EssentialsXHookImpl(this.plugin);
            essentialsXHookImpl.initialize();
            this.essentialsXHookImpl = essentialsXHookImpl;
        } catch (Throwable ignored) {
        }
    }

    public void linkUserEssentialsX(String minecraftId, String discordId) {
        if (this.essentialsXHookImpl != null) {
            this.essentialsXHookImpl.linkUserEssentialsX(minecraftId, discordId);
        }
    }

    public void unlinkUserEssentialsX(String minecraftId) {
        if (this.essentialsXHookImpl != null) {
            this.essentialsXHookImpl.unlinkUserEssentialsX(minecraftId);
        }
    }
}
