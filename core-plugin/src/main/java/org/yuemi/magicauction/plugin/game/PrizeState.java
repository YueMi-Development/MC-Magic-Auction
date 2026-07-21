package org.yuemi.magicauction.plugin.game;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.magicauction.plugin.config.ItemConfig;

public final class PrizeState {

    private final ItemStack originalStack;
    private final ItemConfig config;
    private final int[] position; // [row, col]
    private boolean typeRevealed = false;
    private boolean rarityRevealed = false;
    private boolean sizeRevealed = false;
    private boolean fullyRevealed = false;

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
        return typeRevealed;
    }

    public void setTypeRevealed(boolean typeRevealed) {
        this.typeRevealed = typeRevealed;
    }

    public boolean isRarityRevealed() {
        return rarityRevealed;
    }

    public void setRarityRevealed(boolean rarityRevealed) {
        this.rarityRevealed = rarityRevealed;
    }

    public boolean isSizeRevealed() {
        return sizeRevealed;
    }

    public void setSizeRevealed(boolean sizeRevealed) {
        this.sizeRevealed = sizeRevealed;
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
        }
    }
}
