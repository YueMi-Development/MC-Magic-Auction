package org.yuemi.magicauction.matchs.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Input context for evaluating a single auction round.
 *
 * @param bids           all bids placed this round
 * @param bidOrder       the order in which bids were placed (UUIDs, earliest first)
 * @param basePrice      the arena's base-price floor used in BIN threshold calculation
 * @param multiplier     the overbid multiplier for this round (already clamped to [1.0, 10.0])
 * @param currentRound   the 1-indexed round number
 * @param totalRounds    the total number of configured rounds (multipliers list size)
 * @param hasMultiplierOne whether any configured multiplier equals exactly 1.0
 */
public record RoundContext(
        @NotNull List<Bid> bids,
        @NotNull List<UUID> bidOrder,
        double basePrice,
        double multiplier,
        int currentRound,
        int totalRounds,
        boolean hasMultiplierOne
) {
    public RoundContext {
        Objects.requireNonNull(bids, "bids must not be null");
        Objects.requireNonNull(bidOrder, "bidOrder must not be null");
        if (basePrice < 0) {
            throw new IllegalArgumentException("basePrice must be non-negative, got " + basePrice);
        }
        if (multiplier < 1.0 || multiplier > 10.0) {
            throw new IllegalArgumentException("multiplier must be in [1.0, 10.0], got " + multiplier);
        }
        if (currentRound < 1) {
            throw new IllegalArgumentException("currentRound must be >= 1, got " + currentRound);
        }
        if (totalRounds < 1) {
            throw new IllegalArgumentException("totalRounds must be >= 1, got " + totalRounds);
        }
    }

    /**
     * @return an unmodifiable view of the bids
     */
    @Override
    @NotNull
    public List<Bid> bids() {
        return Collections.unmodifiableList(bids);
    }

    /**
     * @return an unmodifiable view of the bid order
     */
    @Override
    @NotNull
    public List<UUID> bidOrder() {
        return Collections.unmodifiableList(bidOrder);
    }

    /**
     * Returns true if this is the last configured round (no more standard rounds follow).
     */
    public boolean isLastRound() {
        return currentRound >= totalRounds;
    }
}
