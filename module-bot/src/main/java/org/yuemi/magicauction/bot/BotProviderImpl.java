package org.yuemi.magicauction.bot;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BotProviderImpl implements BotHandler {

    private final Set<UUID> botUuids = new HashSet<>();

    @Override
    @NotNull
    public Player createBot(@NotNull String name) {
        UUID uuid = UUID.randomUUID();
        botUuids.add(uuid);
        return BotPlayerProxy.create(name, uuid);
    }

    @Override
    public boolean isBot(@NotNull Player player) {
        return botUuids.contains(player.getUniqueId());
    }
}
