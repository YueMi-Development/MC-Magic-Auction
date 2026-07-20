package org.yuemi.example.plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.yuemi.example.api.ExampleApi;

final class ExampleApiImpl implements ExampleApi {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void sendMessage(
            @NotNull Player player,
            @NotNull String message
    ) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    @Override
    public boolean isFeatureEnabled(@NotNull Player player) {
        return player.hasPermission("example.feature");
    }
}
