package org.yuemi.magicauction.matchmaking;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard implementation of {@link MatchmakingService}.
 * <p>
 * Maintains per-arena queues and a cross-reference from player UUID to
 * arena ID for fast lookups during leave operations.
 */
public final class MatchmakingServiceImpl implements MatchmakingService {

    private final JavaPlugin plugin;
    private final QueueReadyCallback callback;

    /** Arena ID (lowercase) → active queue. */
    private final Map<String, ArenaQueue> queues = new ConcurrentHashMap<>();

    /** Player UUID → arena ID (lowercase) the player is queued for. */
    private final Map<UUID, String> playerQueues = new ConcurrentHashMap<>();

    public MatchmakingServiceImpl(@NotNull JavaPlugin plugin, @NotNull QueueReadyCallback callback) {
        this.plugin = plugin;
        this.callback = callback;
    }

    @Override
    public boolean joinQueue(
            @NotNull Player player,
            @NotNull String arenaId,
            int minPlayers,
            int timeoutSeconds,
            boolean allowBots
    ) {
        // Check if already in any queue
        String existing = playerQueues.get(player.getUniqueId());
        if (existing != null) return false;

        String key = arenaId.toLowerCase();
        ArenaQueue queue = queues.computeIfAbsent(key, id ->
                new ArenaQueue(id, minPlayers, timeoutSeconds, allowBots, callback, plugin)
        );

        boolean joined = queue.addPlayer(player);
        if (joined) {
            playerQueues.put(player.getUniqueId(), key);
        }
        return joined;
    }

    @Override
    public boolean leaveQueue(@NotNull Player player, @Nullable String arenaId) {
        UUID uuid = player.getUniqueId();
        String queuedArena = playerQueues.get(uuid);
        if (queuedArena == null) return false;

        // If a specific arena was given, verify it matches
        if (arenaId != null && !queuedArena.equalsIgnoreCase(arenaId)) return false;

        ArenaQueue queue = queues.get(queuedArena);
        if (queue == null) {
            playerQueues.remove(uuid);
            return false;
        }

        boolean removed = queue.removePlayer(uuid);
        if (removed) {
            playerQueues.remove(uuid);
        }

        // Clean up empty or already-fired queues
        if (queue.isEmpty() || queue.isStarted()) {
            queues.remove(queuedArena);
        }
        return removed;
    }

    @Override
    @Nullable
    public String getPlayerQueue(@NotNull Player player) {
        return playerQueues.get(player.getUniqueId());
    }

    @Override
    public int getQueueSize(@NotNull String arenaId) {
        ArenaQueue queue = queues.get(arenaId.toLowerCase());
        return queue != null ? queue.getQueueSize() : 0;
    }

    @Override
    public void shutdown() {
        for (ArenaQueue queue : queues.values()) {
            queue.reset();
        }
        queues.clear();
        playerQueues.clear();
    }
}
