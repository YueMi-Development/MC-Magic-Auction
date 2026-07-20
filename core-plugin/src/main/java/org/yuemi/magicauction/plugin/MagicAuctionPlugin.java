package org.yuemi.magicauction.plugin;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.magicauction.api.MagicAuctionApi;
import org.yuemi.magicauction.plugin.bstats.BStatsService;
import org.yuemi.config.api.ConfigManager;

public final class MagicAuctionPlugin extends JavaPlugin {

    private MagicAuctionApi api;

    @Override
    public void onEnable() {
        new ConfigManager(this, "org.yuemi.magicauction.plugin.config.migrations").loadAndMigrate(this);
        BStatsService.initialize(this);
        this.api = new MagicAuctionApiImpl();

        getServer().getServicesManager().register(
                MagicAuctionApi.class,
                api,
                this,
                ServicePriority.Normal
        );
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(MagicAuctionApi.class, api);
    }
}
