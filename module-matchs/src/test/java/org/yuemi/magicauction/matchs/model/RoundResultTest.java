package org.yuemi.magicauction.matchs.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoundResultTest {

    private final UUID winnerId = UUID.randomUUID();

    @Test
    void playerWon_setsOutcomeAndWinner() {
        RoundResult result = RoundResult.playerWon(winnerId, 200.0, 100.0, 200.0, false);
        assertEquals(RoundResult.Outcome.PLAYER_WON, result.outcome());
        assertEquals(winnerId, result.winnerId());
        assertEquals(200.0, result.highestBid(), 0.0001);
        assertEquals(100.0, result.secondHighestBid(), 0.0001);
        assertEquals(200.0, result.binThreshold(), 0.0001);
        assertFalse(result.wasTie());
    }

    @Test
    void playerWon_withTie() {
        RoundResult result = RoundResult.playerWon(winnerId, 200.0, 200.0, 400.0, true);
        assertTrue(result.wasTie());
        assertEquals(RoundResult.Outcome.PLAYER_WON, result.outcome());
    }

    @Test
    void noWinnerContinue_setsOutcomeAndNullWinner() {
        RoundResult result = RoundResult.noWinnerContinue(150.0, 100.0, 300.0);
        assertEquals(RoundResult.Outcome.NO_WINNER_CONTINUE, result.outcome());
        assertNull(result.winnerId());
        assertEquals(150.0, result.highestBid(), 0.0001);
        assertEquals(100.0, result.secondHighestBid(), 0.0001);
        assertEquals(300.0, result.binThreshold(), 0.0001);
        assertFalse(result.wasTie());
    }

    @Test
    void bonusRoundRequired_setsOutcomeAndNullWinner() {
        RoundResult result = RoundResult.bonusRoundRequired(150.0, 100.0, 300.0);
        assertEquals(RoundResult.Outcome.BONUS_ROUND_REQUIRED, result.outcome());
        assertNull(result.winnerId());
    }

    @Test
    void auctionEnded_setsOutcome() {
        RoundResult result = RoundResult.auctionEnded(50.0, 25.0, 100.0);
        assertEquals(RoundResult.Outcome.AUCTION_ENDED, result.outcome());
        assertNull(result.winnerId());
    }

    @Test
    void winnerId_returnsNullForNonWinOutcomes() {
        assertNull(RoundResult.noWinnerContinue(0, 0, 0).winnerId());
        assertNull(RoundResult.bonusRoundRequired(0, 0, 0).winnerId());
        assertNull(RoundResult.auctionEnded(0, 0, 0).winnerId());
    }

    @Test
    void toString_containsOutcome() {
        RoundResult result = RoundResult.playerWon(winnerId, 200.0, 100.0, 200.0, false);
        String str = result.toString();
        assertTrue(str.contains("PLAYER_WON"));
        assertTrue(str.contains(winnerId.toString()));
    }
}
