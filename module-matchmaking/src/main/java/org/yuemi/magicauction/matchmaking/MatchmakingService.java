package org.yuemi.magicauction.matchmaking;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages per-arena matchmaking queues.
 * <p>
 * State is purely in-memory; no persistence. Each arena has its own queue
 * with configurable minimum player count and timeout.
 */
public interface MatchmakingService {

    /**
     * Join the queue for a given arena with the specified configuration.
     * <p>
     * If the arena already has an active queue, config parameters are ignored
     * (the first join establishes the queue). If the player is already in
     * another arena's queue, the join is rejected.
     *
     * @param player         the player joining
     * @param arenaId        the arena to queue for
     * @param minPlayers     minimum real players before triggering (clamped &ge; 2)
     * @param timeoutSeconds seconds before bots fill remaining slots (clamped &ge; 10)
     * @param allowBots      whether to allow bot fill on timeout
     * @return true if the player joined the queue, false if already in a queue
     */
    boolean joinQueue(@NotNull Player player,
                      @NotNull String arenaId,
                      int minPlayers,
                      int timeoutSeconds,
                      boolean allowBots);

    /**
     * Leave the player's current queue.
     * <p>
     * If {@code arenaId} is non-null, the player is only removed if they are
     * queued for that specific arena.
     *
     * @param player  the player to remove
     * @param arenaId optional arena to constrain the leave, or null to leave any queue
     * @return true if the player was removed from a queue
     */
    boolean leaveQueue(@NotNull Player player, @Nullable String arenaId);

    /**
     * @param player the player to look up
     * @return the arena ID the player is queued for, or null if not in any queue
     */
    @Nullable
    String getPlayerQueue(@NotNull Player player);

    /**
     * @param arenaId the arena to query
     * @return number of real players currently in the queue for the given arena
     */
    int getQueueSize(@NotNull String arenaId);

    /**
     * Forcibly clear all queues and cancel all pending timeout tasks.
     * Called on plugin disable.
     */
    void shutdown();
}
