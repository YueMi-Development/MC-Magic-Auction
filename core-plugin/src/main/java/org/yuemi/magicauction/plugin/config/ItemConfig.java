package org.yuemi.magicauction.plugin.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.libs.api.YueMiLibsProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemConfig {

    public static final class RewardEntry {
        private final String type; // "item" or "command"
        private final String value; // command value
        private final String itemId; // item ID
        private final int amount; // item quantity

        public RewardEntry(@NotNull String type, @Nullable String value, @Nullable String itemId, int amount) {
            this.type = type.toLowerCase();
            this.value = value;
            this.itemId = itemId;
            this.amount = Math.max(1, amount);
        }

        @NotNull
        public String getType() {
            return type;
        }

        @Nullable
        public String getValue() {
            return value;
        }

        @Nullable
        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }
    }

    private final String id;
    private final String baseItem;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int width;
    private final int height;
    private final int customModelData;
    private final double worth;
    private final boolean virtualItem;
    private final List<String> commands;
    private final List<RewardEntry> rewards;

    private final boolean hasDisplayNameOverride;
    private final boolean hasLoreOverride;
    private final boolean hasCustomModelDataOverride;

    public ItemConfig(
            @NotNull String id,
            @Nullable String baseItem,
            @Nullable Material material,
            @Nullable String displayName,
            @Nullable List<String> lore,
            int width,
            int height,
            int customModelData,
            double worth,
            boolean virtualItem,
            @NotNull List<String> commands,
            @NotNull List<RewardEntry> rewards,
            boolean hasDisplayNameOverride,
            boolean hasLoreOverride,
            boolean hasCustomModelDataOverride
    ) {
        this.id = id;
        this.baseItem = baseItem;
        this.material = material != null ? material : Material.STONE;
        this.displayName = displayName;
        this.lore = lore;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.customModelData = customModelData;
        this.worth = Math.max(0.0, worth);
        this.virtualItem = virtualItem;
        this.commands = commands;
        this.rewards = rewards;
        this.hasDisplayNameOverride = hasDisplayNameOverride;
        this.hasLoreOverride = hasLoreOverride;
        this.hasCustomModelDataOverride = hasCustomModelDataOverride;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Nullable
    public String getBaseItem() {
        return baseItem;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public double getWorth() {
        return worth;
    }

    public boolean isVirtualItem() {
        return virtualItem;
    }

    @NotNull
    public List<String> getCommands() {
        return commands;
    }

    @NotNull
    public List<RewardEntry> getRewards() {
        return rewards;
    }

    @NotNull
    public ItemStack createItemStack(int amount) {
        ItemStack item = null;

        if (baseItem != null && !baseItem.isEmpty()) {
            var libs = YueMiLibsProvider.getApi();
            if (libs != null) {
                try {
                    ItemStack base = libs.getItems().getItem(baseItem, amount);
                    if (base != null) {
                        item = base.clone();
                    }
                } catch (Exception ignored) {}
            }
        }

        if (item == null) {
            item = new ItemStack(material, amount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var mm = MiniMessage.miniMessage();
            boolean modified = false;

            if (hasDisplayNameOverride && displayName != null) {
                meta.displayName(mm.deserialize(displayName));
                modified = true;
            }
            if (hasLoreOverride && lore != null) {
                List<net.kyori.adventure.text.Component> adventureLore = new ArrayList<>();
                for (String line : lore) {
                    adventureLore.add(mm.deserialize(line));
                }
                meta.lore(adventureLore);
                modified = true;
            }
            if (hasCustomModelDataOverride && customModelData > 0) {
                meta.setCustomModelData(customModelData);
                modified = true;
            }

            if (modified) {
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    @NotNull
    public static ItemConfig load(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("id", file.getName().replace(".yml", ""));
        String baseItem = config.getString("base-item", null);
        
        Material material = null;
        String matStr = config.getString("material");
        if (matStr != null) {
            try {
                material = Material.valueOf(matStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String displayName = config.getString("display-name", null);
        List<String> lore = config.contains("lore") ? config.getStringList("lore") : null;
        int width = config.getInt("width", 1);
        int height = config.getInt("height", 1);
        int customModelData = config.getInt("custom-model-data", 0);
        double worth = config.getDouble("worth", 0.0);
        boolean virtualItem = config.getBoolean("virtual-item", false);
        List<String> commands = config.getStringList("commands");
        List<RewardEntry> rewards = new ArrayList<>();

        List<?> contentList = config.getList("rewards");
        if (contentList != null) {
            for (Object obj : contentList) {
                if (obj instanceof Map<?, ?> map) {
                    String type = String.valueOf(map.get("type"));
                    String value = map.containsKey("value") ? String.valueOf(map.get("value")) : null;
                    String itemId = map.containsKey("id") ? String.valueOf(map.get("id")) : null;
                    int amount = 1;
                    if (map.containsKey("amount")) {
                        amount = ((Number) map.get("amount")).intValue();
                    }
                    rewards.add(new RewardEntry(type, value, itemId, amount));
                }
            }
        }

        boolean hasDisplayNameOverride = config.contains("display-name");
        boolean hasLoreOverride = config.contains("lore");
        boolean hasCustomModelDataOverride = config.contains("custom-model-data");

        if (!virtualItem && rewards.isEmpty()) {
            throw new IllegalArgumentException("Non-virtual item configuration '" + id + "' must have a 'rewards' section!");
        }

        return new ItemConfig(
                id, baseItem, material, displayName, lore, width, height, 
                customModelData, worth, virtualItem, commands, rewards,
                hasDisplayNameOverride, hasLoreOverride, hasCustomModelDataOverride
        );
    }
}
