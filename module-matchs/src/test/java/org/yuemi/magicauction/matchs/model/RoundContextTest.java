package org.yuemi.magicauction.matchs.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoundContextTest {

    private final UUID p1 = UUID.randomUUID();
    private final List<Bid> bids = List.of(new Bid(p1, 100.0));
    private final List<UUID> bidOrder = List.of(p1);

    private RoundContext create(double basePrice, double multiplier, int currentRound, int totalRounds) {
        return new RoundContext(bids, bidOrder, basePrice, multiplier, currentRound, totalRounds, false);
    }

    @Test
    void constructor_validInputs_createsContext() {
        RoundContext ctx = create(100.0, 2.0, 1, 5);
        assertEquals(100.0, ctx.basePrice());
        assertEquals(2.0, ctx.multiplier());
        assertEquals(1, ctx.currentRound());
        assertEquals(5, ctx.totalRounds());
        assertFalse(ctx.hasMultiplierOne());
    }

    @Test
    void constructor_hasMultiplierOne_preserved() {
        RoundContext ctx = new RoundContext(bids, bidOrder, 100.0, 2.0, 1, 5, true);
        assertTrue(ctx.hasMultiplierOne());
    }

    @Test
    void constructor_nullBids_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new RoundContext(null, bidOrder, 100.0, 2.0, 1, 5, false));
    }

    @Test
    void constructor_nullBidOrder_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new RoundContext(bids, null, 100.0, 2.0, 1, 5, false));
    }

    @Test
    void constructor_negativeBasePrice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundContext(bids, bidOrder, -0.01, 2.0, 1, 5, false));
    }

    @Test
    void constructor_zeroBasePrice_isValid() {
        RoundContext ctx = create(0.0, 2.0, 1, 5);
        assertEquals(0.0, ctx.basePrice());
    }

    @Test
    void constructor_multiplierBelowOne_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundContext(bids, bidOrder, 100.0, 0.9, 1, 5, false));
    }

    @Test
    void constructor_multiplierAboveTen_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundContext(bids, bidOrder, 100.0, 10.1, 1, 5, false));
    }

    @Test
    void constructor_multiplierExactlyOne_isValid() {
        RoundContext ctx = create(100.0, 1.0, 1, 5);
        assertEquals(1.0, ctx.multiplier());
    }

    @Test
    void constructor_multiplierExactlyTen_isValid() {
        RoundContext ctx = create(100.0, 10.0, 1, 5);
        assertEquals(10.0, ctx.multiplier());
    }

    @Test
    void constructor_currentRoundBelowOne_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundContext(bids, bidOrder, 100.0, 2.0, 0, 5, false));
    }

    @Test
    void constructor_totalRoundsBelowOne_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundContext(bids, bidOrder, 100.0, 2.0, 1, 0, false));
    }

    @Test
    void isLastRound_currentEqualsTotal_returnsTrue() {
        RoundContext ctx = create(100.0, 2.0, 5, 5);
        assertTrue(ctx.isLastRound());
    }

    @Test
    void isLastRound_currentExceedsTotal_returnsTrue() {
        RoundContext ctx = create(100.0, 2.0, 6, 5);
        assertTrue(ctx.isLastRound());
    }

    @Test
    void isLastRound_currentLessThanTotal_returnsFalse() {
        RoundContext ctx = create(100.0, 2.0, 3, 5);
        assertFalse(ctx.isLastRound());
    }

    @Test
    void bids_returnsUnmodifiableList() {
        RoundContext ctx = create(100.0, 2.0, 1, 5);
        assertThrows(UnsupportedOperationException.class, () -> ctx.bids().add(new Bid(UUID.randomUUID(), 1.0)));
    }

    @Test
    void bidOrder_returnsUnmodifiableList() {
        RoundContext ctx = create(100.0, 2.0, 1, 5);
        assertThrows(UnsupportedOperationException.class, () -> ctx.bidOrder().add(UUID.randomUUID()));
    }

    @Test
    void constructor_emptyBidOrder_works() {
        RoundContext ctx = new RoundContext(bids, List.of(), 100.0, 2.0, 1, 5, false);
        assertTrue(ctx.bidOrder().isEmpty());
    }
}
