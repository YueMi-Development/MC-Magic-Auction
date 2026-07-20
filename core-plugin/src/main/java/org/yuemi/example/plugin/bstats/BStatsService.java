package org.yuemi.example.plugin.bstats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class BStatsService {

    private BStatsService() {
        // Private constructor to prevent instantiation
    }

    public static void initialize(JavaPlugin plugin) {
        try (InputStream input = plugin.getResource("plugin.yml")) {
            if (input == null) {
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
            String rawId = config.getString("bstats-id");
            if (rawId == null || rawId.trim().isEmpty() || rawId.contains("bstatsPluginId")) {
                return;
            }
            try {
                int pluginId = Integer.parseInt(rawId.trim());
                new Metrics(plugin, pluginId);
                plugin.getLogger().info("bStats metrics initialized successfully with ID: " + pluginId);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid bStats plugin ID: " + rawId);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize bStats metrics", e);
        }
    }
}
