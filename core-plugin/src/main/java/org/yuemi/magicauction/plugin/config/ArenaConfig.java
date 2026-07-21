package org.yuemi.magicauction.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArenaConfig {

    public static final class PrizeEntry {
        private final String itemId;
        private final int amount;

        public PrizeEntry(@NotNull String itemId, int amount) {
            this.itemId = itemId;
            this.amount = Math.max(1, amount);
        }

        @NotNull
        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }
    }

    private final String id;
    private final String name;
    private final int thinkingTime;
    private final int bidDuration;
    private final double basePrice;
    private final List<Double> multipliers;
    private final List<PrizeEntry> rewards;
    private final List<String> events;

    public ArenaConfig(
            @NotNull String id,
            @NotNull String name,
            int thinkingTime,
            int bidDuration,
            double basePrice,
            @NotNull List<Double> multipliers,
            @NotNull List<PrizeEntry> rewards,
            @NotNull List<String> events
    ) {
        this.id = id;
        this.name = name;
        this.thinkingTime = Math.max(1, thinkingTime);
        this.bidDuration = Math.max(5, bidDuration);
        this.basePrice = Math.max(0.0, basePrice);
        this.multipliers = multipliers.isEmpty() ? List.of(2.0, 1.5, 1.3, 1.1, 1.0) : multipliers;
        this.rewards = rewards;
        this.events = List.copyOf(events);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public int getThinkingTime() {
        return thinkingTime;
    }

    public int getBidDuration() {
        return bidDuration;
    }

    public double getBasePrice() {
        return basePrice;
    }

    @NotNull
    public List<Double> getMultipliers() {
        return multipliers;
    }

    @NotNull
    public List<PrizeEntry> getRewards() {
        return rewards;
    }

    @NotNull
    public List<String> getEvents() {
        return events;
    }

    @NotNull
    public static ArenaConfig load(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("id", file.getName().replace(".yml", ""));
        String name = config.getString("name", id);
        int thinkingTime = config.getInt("thinking-time", 15);
        int bidDuration = config.getInt("bid-time", 30);
        double basePrice = config.getDouble("base-price", 100.0);
        List<Double> multipliers = config.getDoubleList("multipliers");
        List<PrizeEntry> rewards = new ArrayList<>();

        List<?> contentList = config.getList("rewards");
        if (contentList != null) {
            for (Object obj : contentList) {
                if (obj instanceof Map<?, ?> map) {
                    String itemId = String.valueOf(map.get("id"));
                    int amount = 1;
                    if (map.containsKey("amount")) {
                        amount = ((Number) map.get("amount")).intValue();
                    }
                    rewards.add(new PrizeEntry(itemId, amount));
                }
            }
        }

        List<String> events = config.getStringList("events");

        return new ArenaConfig(id, name, thinkingTime, bidDuration, basePrice, multipliers, rewards, events);
    }
}
