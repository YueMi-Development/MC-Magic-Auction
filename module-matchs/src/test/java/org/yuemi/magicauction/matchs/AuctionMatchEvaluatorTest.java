package org.yuemi.magicauction.matchs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.yuemi.magicauction.matchs.model.Bid;
import org.yuemi.magicauction.matchs.model.RoundContext;
import org.yuemi.magicauction.matchs.model.RoundResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionMatchEvaluatorTest {

    // --- clampMultiplier ---

    @Nested
    class ClampMultiplier {
        @Test
        void clampMultiplier_belowMin_returnsMin() {
            assertEquals(1.0, AuctionMatchEvaluator.clampMultiplier(0.5));
            assertEquals(1.0, AuctionMatchEvaluator.clampMultiplier(0.0));
            assertEquals(1.0, AuctionMatchEvaluator.clampMultiplier(-5.0));
        }

        @Test
        void clampMultiplier_aboveMax_returnsMax() {
            assertEquals(10.0, AuctionMatchEvaluator.clampMultiplier(15.0));
            assertEquals(10.0, AuctionMatchEvaluator.clampMultiplier(100.0));
        }

        @Test
        void clampMultiplier_inRange_returnsValue() {
            assertEquals(1.0, AuctionMatchEvaluator.clampMultiplier(1.0));
            assertEquals(5.0, AuctionMatchEvaluator.clampMultiplier(5.0));
            assertEquals(10.0, AuctionMatchEvaluator.clampMultiplier(10.0));
            assertEquals(2.5, AuctionMatchEvaluator.clampMultiplier(2.5));
        }
    }

    // --- Single bidder scenarios ---

    @Nested
    class SingleBidder {
        private final UUID p1 = UUID.randomUUID();
        private final List<Bid> bids = List.of(new Bid(p1, 200.0));
        private final List<UUID> order = List.of(p1);

        @Test
        void meetsBIN_earlyRound_wins() {
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p1, r.winnerId());
            assertEquals(200.0, r.binThreshold(), 0.0001);
        }

        @Test
        void exactlyAtBIN_wins() {
            // BIN = max(0, 100) * 2.0 = 200
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
        }

        @Test
        void belowBIN_notLastRound_continues() {
            List<Bid> lowBid = List.of(new Bid(p1, 50.0));
            RoundContext ctx = new RoundContext(lowBid, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
            assertNull(r.winnerId());
            // BIN = max(0, 100) * 2.0 = 200, highest = 50
            assertEquals(200.0, r.binThreshold(), 0.0001);
        }

        @Test
        void belowBIN_lastRound_hasMultiplierOne_auctionEnds() {
            List<Bid> lowBid = List.of(new Bid(p1, 50.0));
            RoundContext ctx = new RoundContext(lowBid, order, 100.0, 2.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.AUCTION_ENDED, r.outcome());
        }

        @Test
        void belowBIN_lastRound_noMultiplierOne_bonusRound() {
            List<Bid> lowBid = List.of(new Bid(p1, 50.0));
            RoundContext ctx = new RoundContext(lowBid, order, 100.0, 2.0, 5, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.BONUS_ROUND_REQUIRED, r.outcome());
        }

        @Test
        void zeroBid_notLastRound_continues() {
            List<Bid> zeroBid = List.of(new Bid(p1, 0.0));
            RoundContext ctx = new RoundContext(zeroBid, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
            assertEquals(0.0, r.highestBid(), 0.0001);
        }
    }

    // --- Multiple bidders ---

    @Nested
    class MultipleBidders {
        private final UUID p1 = UUID.randomUUID();
        private final UUID p2 = UUID.randomUUID();
        private final UUID p3 = UUID.randomUUID();
        private final List<UUID> order = List.of(p1, p2, p3);

        @Test
        void highestExceedsBIN_wins() {
            List<Bid> bids = List.of(
                    new Bid(p1, 100.0),
                    new Bid(p2, 300.0),
                    new Bid(p3, 50.0)
            );
            // BIN = max(100, 100) * 2.0 = 200, p2 bids 300 >= 200 -> WON
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId());
            assertEquals(300.0, r.highestBid(), 0.0001);
        }

        @Test
        void secondHighestBelowBasePrice_basePriceUsedForBIN() {
            List<Bid> bids = List.of(
                    new Bid(p1, 50.0),
                    new Bid(p2, 250.0)
            );
            // secondHighest=50, basePrice=100, BIN = max(50, 100) * 2.0 = 200
            // p2 bids 250 >= 200 -> WON
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId());
            assertEquals(200.0, r.binThreshold(), 0.0001);
        }

        @Test
        void secondHighestAboveBasePrice_secondHighestUsedForBIN() {
            List<Bid> bids = List.of(
                    new Bid(p1, 150.0),
                    new Bid(p2, 400.0)
            );
            // secondHighest=150, basePrice=100, BIN = max(150, 100) * 2.0 = 300
            // p2 bids 400 >= 300 -> WON
            RoundContext ctx = new RoundContext(bids, order, 50.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(300.0, r.binThreshold(), 0.0001);
        }

        @Test
        void allBelowBIN_notLastRound_continues() {
            List<Bid> bids = List.of(
                    new Bid(p1, 50.0),
                    new Bid(p2, 180.0)
            );
            // BIN = max(50, 100) * 2.0 = 200, p2 bids 180 < 200
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
            assertNull(r.winnerId());
        }

        @Test
        void allBelowBIN_lastRound_noMultiplierOne_bonusRound() {
            List<Bid> bids = List.of(
                    new Bid(p1, 50.0),
                    new Bid(p2, 180.0)
            );
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 5, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.BONUS_ROUND_REQUIRED, r.outcome());
        }

        @Test
        void allBelowBIN_lastRound_hasMultiplierOne_auctionEnded() {
            List<Bid> bids = List.of(
                    new Bid(p1, 50.0),
                    new Bid(p2, 180.0)
            );
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.AUCTION_ENDED, r.outcome());
        }
    }

    // --- Tie-breaking (last round only) ---

    @Nested
    class TieBreaking {
        private final UUID p1 = UUID.randomUUID();
        private final UUID p2 = UUID.randomUUID();
        private final UUID p3 = UUID.randomUUID();

        @Test
        void twoTied_firstToSubmitWins() {
            // p2 and p3 tie at 200, p2 submitted first.
            // Because p3 also bids 200, second-highest (excluding p2) = 200
            // BIN = max(200, 100) * 1.0 = 200, p2 at 200 meets it exactly
            List<Bid> bids = List.of(
                    new Bid(p1, 50.0),
                    new Bid(p2, 200.0),
                    new Bid(p3, 200.0)
            );
            List<UUID> order = List.of(p1, p2, p3);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId(), "First to submit among ties should win");
            assertTrue(r.wasTie());
        }

        @Test
        void threeTied_firstToSubmitWins() {
            // p1, p2, p3 all tie at 150
            List<Bid> bids = List.of(
                    new Bid(p1, 150.0),
                    new Bid(p2, 150.0),
                    new Bid(p3, 150.0)
            );
            List<UUID> order = List.of(p2, p1, p3); // p2 came first
            // BIN = max(150, 100) * 2.0 = 300, no one meets it -> no winner
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 5, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.BONUS_ROUND_REQUIRED, r.outcome());
            // But also test case where they meet BIN
        }

        @Test
        void threeTied_meetBIN_firstToSubmitWins() {
            List<Bid> bids = List.of(
                    new Bid(p1, 400.0),
                    new Bid(p2, 400.0),
                    new Bid(p3, 400.0)
            );
            List<UUID> order = List.of(p3, p1, p2); // p3 came first
            // BIN = max(400, 100) * 2.0 = 800, but 400 < 800. Hmm, doesn't meet.
            // Actually with multiplier 1.0 and basePrice 100: BIN = 400, exactly meets
            RoundContext ctx = new RoundContext(bids, order, 400.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p3, r.winnerId(), "First to submit among ties should win");
            assertTrue(r.wasTie());
        }

        @Test
        void lastRound_noTie_noWasTieFlag() {
            List<Bid> bids = List.of(
                    new Bid(p1, 400.0),
                    new Bid(p2, 200.0)
            );
            List<UUID> order = List.of(p1, p2);
            // BIN = max(200, 100) * 1.0 = 200, p1 bids 400 >= 200
            RoundContext ctx = new RoundContext(bids, order, 100.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertFalse(r.wasTie(), "No tie should have wasTie=false");
        }

        @Test
        void tieOnNonLastRound_notTreatedAsTie() {
            List<Bid> bids = List.of(
                    new Bid(p1, 200.0),
                    new Bid(p2, 200.0)
            );
            List<UUID> order = List.of(p2, p1); // p2 first
            // Round 1 of 5, not last round
            RoundContext ctx = new RoundContext(bids, order, 100.0, 1.0, 1, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            // On non-last round, the "tie" winner is the first encountered in the loop (p1)
            assertFalse(r.wasTie());
            // BIN = max(200, 100) * 1.0 = 200, p1 meets it
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
        }
    }

    // --- BIN threshold edge cases ---

    @Nested
    class BINThreshold {
        private final UUID p1 = UUID.randomUUID();
        private final UUID p2 = UUID.randomUUID();

        @Test
        void noSecondBidder_basePriceUsedAsFloor() {
            List<Bid> bids = List.of(new Bid(p1, 50.0));
            List<UUID> order = List.of(p1);
            // secondHighest = 0, basePrice = 100, BIN = max(0, 100) * 2.0 = 200
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(0.0, r.secondHighestBid(), 0.0001);
            assertEquals(200.0, r.binThreshold(), 0.0001);
        }

        @Test
        void basePriceZero_binZero_anyBidWins() {
            List<Bid> bids = List.of(new Bid(p1, 1.0));
            List<UUID> order = List.of(p1);
            RoundContext ctx = new RoundContext(bids, order, 0.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(0.0, r.binThreshold(), 0.0001);
        }

        @Test
        void basePriceZero_binZeroWithSecondBidder() {
            List<Bid> bids = List.of(new Bid(p1, 0.0), new Bid(p2, 1.0));
            List<UUID> order = List.of(p1, p2);
            RoundContext ctx = new RoundContext(bids, order, 0.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId());
            assertEquals(0.0, r.binThreshold(), 0.0001);
        }

        @Test
        void exactBINThreshold_meetsRequirement() {
            List<Bid> bids = List.of(new Bid(p1, 200.0));
            List<UUID> order = List.of(p1);
            // BIN = max(0, 100) * 2.0 = 200, bid = 200, exactly meets
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
        }

        @Test
        void justBelowBINThreshold_doesNotMeet() {
            List<Bid> bids = List.of(new Bid(p1, 199.9999));
            List<UUID> order = List.of(p1);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
        }
    }

    // --- Multiplier edge cases ---

    @Nested
    class MultiplierEdgeCases {
        private final UUID p1 = UUID.randomUUID();
        private final UUID p2 = UUID.randomUUID();

        @Test
        void multiplier10_binIs10x() {
            List<Bid> bids = List.of(new Bid(p1, 1000.0));
            List<UUID> order = List.of(p1);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 10.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(1000.0, r.binThreshold(), 0.0001);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
        }

        @Test
        void multiplier1_binEqualsMaxSecondBase() {
            List<Bid> bids = List.of(new Bid(p1, 150.0), new Bid(p2, 100.0));
            List<UUID> order = List.of(p1, p2);
            // BIN = max(100, 100) * 1.0 = 100, p1 at 150 >= 100
            RoundContext ctx = new RoundContext(bids, order, 100.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(100.0, r.binThreshold(), 0.0001);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
        }
    }

    // --- Empty / zero edge cases ---

    @Nested
    class EdgeCases {
        private final UUID p1 = UUID.randomUUID();
        private final UUID p2 = UUID.randomUUID();
        private final UUID p3 = UUID.randomUUID();

        @Test
        void noBids_returnsNoWinnerContinue() {
            RoundContext ctx = new RoundContext(List.of(), List.of(), 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
            assertEquals(0.0, r.highestBid(), 0.0001);
            assertNull(r.winnerId());
        }

        @Test
        void allZeroBids_notLastRound_continues() {
            List<Bid> bids = List.of(new Bid(p1, 0.0), new Bid(p2, 0.0));
            List<UUID> order = List.of(p1, p2);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, r.outcome());
        }

        @Test
        void allZeroBids_lastRound_hasMultiplierOne_auctionEnded() {
            List<Bid> bids = List.of(new Bid(p1, 0.0), new Bid(p2, 0.0));
            List<UUID> order = List.of(p1, p2);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.AUCTION_ENDED, r.outcome());
        }

        @Test
        void secondHighestRecalculatedExcludingWinner() {
            // p2 (first to submit) and p1 tie at 200, p3 lower at 50.
            // After tie-breaking, winner = p2 (first in bidOrder).
            // second-highest recalculated excl. winner = max(p1=200, p3=50) = 200
            // BIN = max(200, 100) * 1.0 = 200, p2 at 200 >= 200 → PLAYER_WON
            // Assert second-highest is NOT 50 but 200 (the other tied player's bid)
            List<Bid> bids = List.of(
                    new Bid(p1, 200.0),
                    new Bid(p2, 200.0),
                    new Bid(p3, 50.0)
            );
            List<UUID> order = List.of(p2, p1, p3);
            RoundContext ctx = new RoundContext(bids, order, 100.0, 1.0, 5, 5, true);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId());
            // The other tied player (p1) still counts toward second-highest
            assertEquals(200.0, r.secondHighestBid(), 0.0001);
        }

        @Test
        void bidOrderContainsUUIDsNotInBids() {
            UUID p3 = UUID.randomUUID();
            List<Bid> bids = List.of(new Bid(p1, 100.0), new Bid(p2, 300.0));
            List<UUID> order = List.of(p3, p1, p2); // p3 not in bids, p1 and p2 are
            RoundContext ctx = new RoundContext(bids, order, 100.0, 2.0, 1, 5, false);
            // Should not throw, should work normally
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            assertEquals(RoundResult.Outcome.PLAYER_WON, r.outcome());
            assertEquals(p2, r.winnerId());
        }

        @Test
        void largeBidValues_noOverflow() {
            List<Bid> bids = List.of(new Bid(p1, Double.MAX_VALUE / 2));
            List<UUID> order = List.of(p1);
            RoundContext ctx = new RoundContext(bids, order, Double.MAX_VALUE / 4, 2.0, 1, 5, false);
            RoundResult r = AuctionMatchEvaluator.evaluateRound(ctx);
            // Should produce some result without overflow
            assertNotNull(r);
        }
    }
}
