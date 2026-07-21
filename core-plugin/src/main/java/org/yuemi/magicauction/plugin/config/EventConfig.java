package org.yuemi.magicauction.plugin.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class EventConfig {

    public static final class ActionEntry {
        private final String type; // "type", "rarity", "size", "full"
        private final String selection; // "random", "all"
        private final int count;
        private final String rarity; // optional rarity condition
        private final int minTotalSize; // optional min total size condition (width * height)

        public ActionEntry(
                @NotNull String type,
                @NotNull String selection,
                int count,
                @Nullable String rarity,
                int minTotalSize
        ) {
            this.type = type.toLowerCase();
            this.selection = selection.toLowerCase();
            this.count = Math.max(1, count);
            this.rarity = rarity != null ? rarity.toLowerCase() : null;
            this.minTotalSize = Math.max(0, minTotalSize);
        }

        @NotNull
        public String getType() {
            return type;
        }

        @NotNull
        public String getSelection() {
            return selection;
        }

        public int getCount() {
            return count;
        }

        @Nullable
        public String getRarity() {
            return rarity;
        }

        public int getMinTotalSize() {
            return minTotalSize;
        }
    }

    private final String id;
    private final String name;
    private final String desc;
    private final boolean onlyOnce;
    private final int minRounds;
    private final List<ActionEntry> actions;

    public EventConfig(
            @NotNull String id,
            @NotNull String name,
            @NotNull String desc,
            boolean onlyOnce,
            int minRounds,
            @NotNull List<ActionEntry> actions
    ) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.onlyOnce = onlyOnce;
        this.minRounds = Math.max(1, minRounds);
        this.actions = List.copyOf(actions);
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
    public String getDesc() {
        return desc;
    }

    public boolean isOnlyOnce() {
        return onlyOnce;
    }

    public int getMinRounds() {
        return minRounds;
    }

    @NotNull
    public List<ActionEntry> getActions() {
        return actions;
    }
}
