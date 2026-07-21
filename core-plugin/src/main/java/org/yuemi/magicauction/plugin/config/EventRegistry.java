package org.yuemi.magicauction.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventRegistry {

    private static final Map<String, EventConfig> EVENTS = new HashMap<>();

    public static void load(@NotNull File folder) {
        EVENTS.clear();
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");
                String name = config.getString("name");
                String desc = config.getString("desc");
                if (desc == null) {
                    desc = config.getString("description");
                }
                if (name == null || desc == null) {
                    throw new IllegalArgumentException("Event file '" + file.getName() + "' must contain both 'name' and 'desc' (or 'description') fields!");
                }

                List<EventConfig.ActionEntry> actions = new ArrayList<>();

                List<?> actionList = config.getList("actions");
                if (actionList != null) {
                    for (Object obj : actionList) {
                        if (obj instanceof Map<?, ?> map) {
                            String type = String.valueOf(map.get("type"));
                            String selection = map.containsKey("selection") ? String.valueOf(map.get("selection")) : "all";
                            int count = map.containsKey("count") ? ((Number) map.get("count")).intValue() : 1;
                            
                            String rarity = null;
                            int minTotalSize = 0;
                            if (map.containsKey("conditions")) {
                                Object condObj = map.get("conditions");
                                if (condObj instanceof Map<?, ?> condMap) {
                                    if (condMap.containsKey("rarity")) {
                                        rarity = String.valueOf(condMap.get("rarity"));
                                    }
                                    if (condMap.containsKey("min-total-size")) {
                                        minTotalSize = ((Number) condMap.get("min-total-size")).intValue();
                                    }
                                }
                            }
                            actions.add(new EventConfig.ActionEntry(type, selection, count, rarity, minTotalSize));
                        }
                    }
                }
                EVENTS.put(id.toLowerCase(), new EventConfig(id, name, desc, actions));
            } catch (Exception ignored) {
                // Ignore config loading errors for individual files to avoid crashing the registry
            }
        }
    }

    @Nullable
    public static EventConfig get(@NotNull String id) {
        return EVENTS.get(id.toLowerCase());
    }

    @NotNull
    public static Map<String, EventConfig> getEvents() {
        return new HashMap<>(EVENTS);
    }
}
