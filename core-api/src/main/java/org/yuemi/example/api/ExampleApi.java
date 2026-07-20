package org.yuemi.example.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ExampleApi {

    void sendMessage(
            @NotNull Player player,
            @NotNull String message
    );

    boolean isFeatureEnabled(@NotNull Player player);
}
