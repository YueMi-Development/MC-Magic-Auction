package org.yuemi.magicauction.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class TypeRegistry {

    public static final class TypeInfo {
        private final String id;
        private final String name;
        private final String description;

        public TypeInfo(@NotNull String id, @NotNull String name, @NotNull String description) {
            this.id = id.toLowerCase();
            this.name = name;
            this.description = description;
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
    }

    private static final Map<String, TypeInfo> TYPES = new HashMap<>();

    public static void load(@NotNull File file) {
        TYPES.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec != null) {
                String name = sec.getString("name", key);
                String desc = sec.getString("description", "");
                TYPES.put(key.toLowerCase(), new TypeInfo(key, name, desc));
            }
        }
    }

    @Nullable
    public static TypeInfo get(@NotNull String id) {
        return TYPES.get(id.toLowerCase());
    }

    @NotNull
    public static Map<String, TypeInfo> getTypes() {
        return new HashMap<>(TYPES);
    }
}
