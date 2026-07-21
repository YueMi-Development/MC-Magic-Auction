package org.yuemi.magicauction.matchs.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * A single bid entry in an auction round.
 *
 * @param playerId the UUID of the player who placed this bid
 * @param amount   the monetary amount of the bid
 */
public record Bid(@NotNull UUID playerId, double amount) {

    public Bid {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (amount < 0) {
            throw new IllegalArgumentException("Bid amount must be non-negative, got " + amount);
        }
    }
}
