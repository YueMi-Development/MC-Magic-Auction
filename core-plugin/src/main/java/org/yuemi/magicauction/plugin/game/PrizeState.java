package org.yuemi.magicauction.plugin.game;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.magicauction.plugin.config.ItemConfig;

public final class PrizeState {

    private final java.util.UUID uniqueId = java.util.UUID.randomUUID();
    private final ItemStack originalStack;
    private final ItemConfig config;
    private final int[] position; // [row, col]
    private boolean typeRevealed = false;
    private boolean rarityRevealed = false;
    private boolean sizeRevealed = false;
    private boolean fullyRevealed = false;
    private boolean hide = true;

    public PrizeState(
            @NotNull ItemStack originalStack,
            @NotNull ItemConfig config,
            @NotNull int[] position
    ) {
        this.originalStack = originalStack;
        this.config = config;
        this.position = position;
    }

    @NotNull
    public java.util.UUID getUniqueId() {
        return uniqueId;
    }

    @NotNull
    public ItemStack getOriginalStack() {
        return originalStack;
    }

    @NotNull
    public ItemConfig getConfig() {
        return config;
    }

    @NotNull
    public int[] getPosition() {
        return position;
    }

    public boolean isTypeRevealed() {
        return typeRevealed || fullyRevealed;
    }

    public void setTypeRevealed(boolean typeRevealed) {
        this.typeRevealed = typeRevealed;
        if (typeRevealed) {
            this.hide = false;
        }
    }

    public boolean isRarityRevealed() {
        return rarityRevealed || fullyRevealed;
    }

    public void setRarityRevealed(boolean rarityRevealed) {
        this.rarityRevealed = rarityRevealed;
        if (rarityRevealed) {
            this.hide = false;
        }
    }

    public boolean isSizeRevealed() {
        return sizeRevealed || fullyRevealed;
    }

    public void setSizeRevealed(boolean sizeRevealed) {
        this.sizeRevealed = sizeRevealed;
        if (sizeRevealed) {
            this.hide = false;
        }
    }

    public boolean isFullyRevealed() {
        return fullyRevealed;
    }

    public void setFullyRevealed(boolean fullyRevealed) {
        this.fullyRevealed = fullyRevealed;
        if (fullyRevealed) {
            this.typeRevealed = true;
            this.rarityRevealed = true;
            this.sizeRevealed = true;
            this.hide = false;
        }
    }

    public boolean isHide() {
        return hide && !fullyRevealed && !typeRevealed && !rarityRevealed && !sizeRevealed;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }
}
