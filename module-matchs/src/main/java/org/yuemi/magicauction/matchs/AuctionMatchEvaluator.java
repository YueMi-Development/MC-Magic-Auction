package org.yuemi.magicauction.matchs;

import org.jetbrains.annotations.NotNull;
import org.yuemi.magicauction.matchs.model.Bid;
import org.yuemi.magicauction.matchs.model.RoundContext;
import org.yuemi.magicauction.matchs.model.RoundResult;

import java.util.List;
import java.util.UUID;

/**
 * Pure evaluator for auction round matching logic.
 *
 * <p>This class is stateless and thread-safe — all inputs are provided via
 * {@link RoundContext} and a new {@link RoundResult} is returned for each
 * invocation, making it easily testable without any Bukkit dependency.
 */
public final class AuctionMatchEvaluator {

    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 10.0;

    /**
     * Clamp a multiplier to the valid {@code [1.0, 10.0]} range.
     */
    public static double clampMultiplier(double raw) {
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, raw));
    }

    /**
     * Evaluate an auction round and determine the outcome.
     *
     * @param context the round input data (bids, configuration, round state)
     * @return a {@link RoundResult} describing what should happen next
     */
    @NotNull
    public static RoundResult evaluateRound(@NotNull RoundContext context) {
        // --- Find highest and second-highest bids ---
        double highestBid = -1;
        double secondHighestBid = -1;
        UUID highestBidder = null;

        for (Bid bid : context.bids()) {
            double amount = bid.amount();
            if (amount > highestBid) {
                secondHighestBid = highestBid;
                highestBid = amount;
                highestBidder = bid.playerId();
            } else if (amount > secondHighestBid) {
                secondHighestBid = amount;
            }
        }

        // If no valid bids exist (shouldn't happen in normal play)
        if (highestBidder == null) {
            return buildNoWinnerResult(context, 0, 0);
        }

        // --- Tie-breaking on the last round ---
        UUID winner = null;
        boolean wasTie = false;

        if (context.isLastRound()) {
            // Capture highest bid value for lambda (effectively final)
            double maxBid = highestBid;

            // Find all players who bid exactly the highest amount
            List<UUID> topBidders = context.bids().stream()
                    .filter(b -> Math.abs(b.amount() - maxBid) < 0.0001)
                    .map(Bid::playerId)
                    .toList();

            if (topBidders.size() > 1) {
                // Tie: the first among them to bid wins
                wasTie = true;
                UUID firstBidder = null;
                int firstIndex = Integer.MAX_VALUE;
                for (int i = 0; i < context.bidOrder().size(); i++) {
                    UUID pid = context.bidOrder().get(i);
                    if (topBidders.contains(pid) && i < firstIndex) {
                        firstIndex = i;
                        firstBidder = pid;
                    }
                }
                winner = firstBidder;
            } else if (!topBidders.isEmpty()) {
                winner = topBidders.getFirst();
            }
        } else {
            winner = highestBidder;
        }

        // --- Recalculate second-highest excluding the winner ---
        double effectiveSecondHighest = 0;
        for (Bid bid : context.bids()) {
            if (bid.playerId().equals(winner)) continue;
            if (bid.amount() > effectiveSecondHighest) {
                effectiveSecondHighest = bid.amount();
            }
        }

        // --- BIN threshold ---
        double binThreshold = Math.max(effectiveSecondHighest, context.basePrice()) * context.multiplier();

        // --- Determine outcome ---
        if (winner != null && highestBid >= binThreshold) {
            return RoundResult.playerWon(winner, highestBid, effectiveSecondHighest, binThreshold, wasTie);
        }

        // No winner this round
        if (!context.isLastRound()) {
            return buildNoWinnerContinue(context, highestBid, effectiveSecondHighest, binThreshold);
        }

        // Last round without a winner — check bonus round
        if (!context.hasMultiplierOne()) {
            return RoundResult.bonusRoundRequired(highestBid, effectiveSecondHighest, binThreshold);
        }

        // Auction over
        return RoundResult.auctionEnded(highestBid, effectiveSecondHighest, binThreshold);
    }

    private static RoundResult buildNoWinnerResult(
            RoundContext context,
            double highestBid,
            double secondHighestBid
    ) {
        double binThreshold = Math.max(secondHighestBid, context.basePrice()) * context.multiplier();
        return buildNoWinnerContinue(context, highestBid, secondHighestBid, binThreshold);
    }

    private static RoundResult buildNoWinnerContinue(
            RoundContext context,
            double highestBid,
            double secondHighestBid,
            double binThreshold
    ) {
        return RoundResult.noWinnerContinue(highestBid, secondHighestBid, binThreshold);
    }
}
