package org.yuemi.magicauction.matchmaking;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single arena's matchmaking queue.
 * <p>
 * Holds waiting real players (insertion-ordered via {@link LinkedHashMap})
 * and manages a timeout task. When the queue reaches {@code minPlayers}
 * or the timeout fires, the {@link QueueReadyCallback} is invoked exactly once.
 * After that the queue is considered "started" and no new players may join.
 */
final class ArenaQueue {

    private final String arenaId;
    private final int minPlayers;
    private final int timeoutSeconds;
    private final boolean allowBots;
    private final QueueReadyCallback callback;
    private final JavaPlugin plugin;

    /** Real players waiting, insertion-ordered keyed by UUID. */
    private final Map<UUID, Player> waiting = new LinkedHashMap<>();

    private @Nullable BukkitTask timeoutTask;
    private boolean started;

    ArenaQueue(
            @NotNull String arenaId,
            int minPlayers,
            int timeoutSeconds,
            boolean allowBots,
            @NotNull QueueReadyCallback callback,
            @NotNull JavaPlugin plugin
    ) {
        this.arenaId = arenaId;
        this.minPlayers = Math.max(2, minPlayers);
        this.timeoutSeconds = Math.max(10, timeoutSeconds);
        this.allowBots = allowBots;
        this.callback = callback;
        this.plugin = plugin;
    }

    /**
     * Add a player to the queue.
     *
     * @return true if the player was added; false if already in the queue or the queue has started
     */
    synchronized boolean addPlayer(@NotNull Player player) {
        if (started) return false;
        if (waiting.containsKey(player.getUniqueId())) return false;

        waiting.put(player.getUniqueId(), player);

        if (waiting.size() >= minPlayers) {
            fire(false);
        } else if (timeoutTask == null) {
            // Start the timeout timer on the first player entering the queue
            timeoutTask = plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    this::onTimeout,
                    timeoutSeconds * 20L
            );
        }
        return true;
    }

    /**
     * Remove a player from the queue.
     *
     * @return true if the player was in the queue and was removed
     */
    synchronized boolean removePlayer(@NotNull UUID playerId) {
        boolean removed = waiting.remove(playerId) != null;

        // Cancel the timeout if the queue becomes empty
        if (waiting.isEmpty() && timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        return removed;
    }

    synchronized boolean isEmpty() {
        return waiting.isEmpty();
    }

    synchronized boolean isStarted() {
        return started;
    }

    @NotNull
    String getArenaId() {
        return arenaId;
    }

    synchronized int getQueueSize() {
        return waiting.size();
    }

    @Nullable
    synchronized String getPlayerQueue(@NotNull UUID playerId) {
        return waiting.containsKey(playerId) ? arenaId : null;
    }

    /** Reset the queue entirely — cancels timeout, clears players, resets started flag. */
    synchronized void reset() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        waiting.clear();
        started = false;
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private synchronized void onTimeout() {
        timeoutTask = null;
        if (!started && !waiting.isEmpty()) {
            fire(true);
        }
    }

    private void fire(boolean timedOut) {
        started = true;
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        callback.onQueueReady(arenaId, new ArrayList<>(waiting.values()), timedOut);
    }
}
