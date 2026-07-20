package org.yuemi.magicauction.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface MagicAuctionApi {

    void sendMessage(
            @NotNull Player player,
            @NotNull String message
    );

    boolean isFeatureEnabled(@NotNull Player player);
}
