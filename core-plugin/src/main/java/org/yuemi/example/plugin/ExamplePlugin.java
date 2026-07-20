package org.yuemi.example.plugin;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.example.api.ExampleApi;
import org.yuemi.example.plugin.bstats.BStatsService;
import org.yuemi.config.api.ConfigManager;

public final class ExamplePlugin extends JavaPlugin {

    private ExampleApi api;

    @Override
    public void onEnable() {
        new ConfigManager(this, "org.yuemi.example.plugin.config.migrations").loadAndMigrate(this);
        BStatsService.initialize(this);
        this.api = new ExampleApiImpl();

        getServer().getServicesManager().register(
                ExampleApi.class,
                api,
                this,
                ServicePriority.Normal
        );
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(ExampleApi.class, api);
    }
}
