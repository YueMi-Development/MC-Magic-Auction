package org.yuemi.magicauction.plugin.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RarityRegistry {

    public static final class RarityInfo {
        private final String id;
        private final String name;
        private final String description;
        private final String color;
        private final Material glassPaneMaterial;
        private final List<String> revealSounds;

        public RarityInfo(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull String color, @Nullable List<String> revealSounds) {
            this.id = id.toLowerCase();
            this.name = name;
            this.description = description;
            this.color = color.toLowerCase();
            this.revealSounds = revealSounds != null ? List.copyOf(revealSounds) : null;

            this.glassPaneMaterial = GlassPaneMapper.getMaterial(color);
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

        @Nullable
        public List<String> getRevealSounds() {
            return revealSounds;
        }
    }

    private static final Map<String, RarityInfo> RARITIES = new HashMap<>();

    public static void load(@NotNull File folder) {
        RARITIES.clear();
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replace(".yml", "");
            String name = config.getString("name", id);
            String desc = config.getString("description", "");
            String color = config.getString("color", "gray");
            List<String> revealSounds = config.getStringList("reveal-sounds");
            RARITIES.put(id.toLowerCase(), new RarityInfo(id, name, desc, color,
                    revealSounds.isEmpty() ? null : revealSounds));
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
