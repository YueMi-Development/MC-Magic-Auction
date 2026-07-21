package org.yuemi.magicauction.matchs.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The outcome of evaluating an auction round.
 */
public final class RoundResult {

    public enum Outcome {
        /** A player met the BIN threshold and won the auction. */
        PLAYER_WON,
        /** No one met the BIN threshold; proceed to the next standard round. */
        NO_WINNER_CONTINUE,
        /**
         * No winner after the final configured round, and no multiplier of 1.0
         * is configured — a forced bonus round (multiplier 1.0) should be played.
         */
        BONUS_ROUND_REQUIRED,
        /** No winner and no further rounds are possible — the auction is over. */
        AUCTION_ENDED
    }

    private final Outcome outcome;
    @Nullable
    private final UUID winnerId;
    private final double highestBid;
    private final double secondHighestBid;
    private final double binThreshold;
    private final boolean wasTie;

    public RoundResult(
            @NotNull Outcome outcome,
            @Nullable UUID winnerId,
            double highestBid,
            double secondHighestBid,
            double binThreshold,
            boolean wasTie
    ) {
        this.outcome = outcome;
        this.winnerId = winnerId;
        this.highestBid = highestBid;
        this.secondHighestBid = secondHighestBid;
        this.binThreshold = binThreshold;
        this.wasTie = wasTie;
    }

    // --- Factory helpers ---

    public static RoundResult playerWon(
            @NotNull UUID winnerId,
            double highestBid,
            double secondHighestBid,
            double binThreshold,
            boolean wasTie
    ) {
        return new RoundResult(Outcome.PLAYER_WON, winnerId, highestBid, secondHighestBid, binThreshold, wasTie);
    }

    public static RoundResult noWinnerContinue(
            double highestBid,
            double secondHighestBid,
            double binThreshold
    ) {
        return new RoundResult(Outcome.NO_WINNER_CONTINUE, null, highestBid, secondHighestBid, binThreshold, false);
    }

    public static RoundResult bonusRoundRequired(
            double highestBid,
            double secondHighestBid,
            double binThreshold
    ) {
        return new RoundResult(Outcome.BONUS_ROUND_REQUIRED, null, highestBid, secondHighestBid, binThreshold, false);
    }

    public static RoundResult auctionEnded(
            double highestBid,
            double secondHighestBid,
            double binThreshold
    ) {
        return new RoundResult(Outcome.AUCTION_ENDED, null, highestBid, secondHighestBid, binThreshold, false);
    }

    // --- Getters ---

    @NotNull
    public Outcome outcome() {
        return outcome;
    }

    /**
     * @return the winner's UUID, or {@code null} if no winner was determined
     */
    @Nullable
    public UUID winnerId() {
        return winnerId;
    }

    /** The highest bid amount placed this round. */
    public double highestBid() {
        return highestBid;
    }

    /** The second-highest bid amount placed this round. */
    public double secondHighestBid() {
        return secondHighestBid;
    }

    /**
     * The BIN (Buy It Now) threshold for this round:
     * {@code max(secondHighestBid, basePrice) * multiplier}.
     */
    public double binThreshold() {
        return binThreshold;
    }

    /** Whether a tie occurred (multiple players with the same top bid). */
    public boolean wasTie() {
        return wasTie;
    }

    @Override
    public String toString() {
        return "RoundResult{" +
                "outcome=" + outcome +
                ", winnerId=" + winnerId +
                ", highestBid=" + highestBid +
                ", secondHighestBid=" + secondHighestBid +
                ", binThreshold=" + binThreshold +
                ", wasTie=" + wasTie +
                '}';
    }
}
