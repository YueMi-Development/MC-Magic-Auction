package org.yuemi.magicauction.plugin.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class RarityRegistry {

    public static final class RarityInfo {
        private final String id;
        private final String name;
        private final String description;
        private final String color;
        private final Material glassPaneMaterial;

        public RarityInfo(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String color) {
            this.id = id.toLowerCase();
            this.name = name;
            this.description = description;
            this.color = color.toLowerCase();
            
            Material mat = Material.matchMaterial(color.toUpperCase() + "_STAINED_GLASS_PANE");
            if (mat == null) {
                // Try alternate name mapping (e.g. orange -> orange)
                mat = Material.matchMaterial(color.toUpperCase() + "_STAINED_GLASS_PANE");
            }
            this.glassPaneMaterial = mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
        }

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getDescription() {
            return description;
        }

        @NotNull
        public String getColor() {
            return color;
        }

        @NotNull
        public Material getGlassPaneMaterial() {
            return glassPaneMaterial;
        }
    }

    private static final Map<String, RarityInfo> RARITIES = new HashMap<>();

    public static void load(@NotNull File file) {
        RARITIES.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec != null) {
                String name = sec.getString("name", key);
                String desc = sec.getString("description", "");
                String color = sec.getString("color", "gray");
                RARITIES.put(key.toLowerCase(), new RarityInfo(key, name, desc, color));
            }
        }
    }

    @Nullable
    public static RarityInfo get(@NotNull String id) {
        return RARITIES.get(id.toLowerCase());
    }

    @NotNull
    public static Map<String, RarityInfo> getRarities() {
        return new HashMap<>(RARITIES);
    }
}
