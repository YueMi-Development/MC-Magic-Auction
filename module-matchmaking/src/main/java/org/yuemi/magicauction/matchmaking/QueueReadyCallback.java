package org.yuemi.magicauction.matchmaking;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called by {@link MatchmakingService} when a queue is ready to start an auction.
 * The implementor ({@code AuctionManager}) is responsible for creating the session.
 */
@FunctionalInterface
public interface QueueReadyCallback {

    /**
     * Invoked when a matchmaking queue has assembled enough players or timed out.
     *
     * @param arenaId  the arena to start
     * @param players  the assembled list of real players (bots are filled by the implementor)
     * @param timedOut true if the timeout triggered (bots may still be needed),
     *                 false if the minimum-player threshold was met naturally
     */
    void onQueueReady(@NotNull String arenaId, @NotNull List<Player> players, boolean timedOut);
}
