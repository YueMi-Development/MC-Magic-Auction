package org.yuemi.magicauction.bot;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface BotHandler {

    @NotNull
    Player createBot(@NotNull String name);

    boolean isBot(@NotNull Player player);
}
