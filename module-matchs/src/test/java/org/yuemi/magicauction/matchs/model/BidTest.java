package org.yuemi.magicauction.matchs.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void constructor_validInputs_createsBid() {
        Bid bid = new Bid(playerId, 100.0);
        assertEquals(playerId, bid.playerId());
        assertEquals(100.0, bid.amount(), 0.0001);
    }

    @Test
    void constructor_zeroAmount_isValid() {
        Bid bid = new Bid(playerId, 0.0);
        assertEquals(0.0, bid.amount(), 0.0001);
    }

    @Test
    void constructor_nullPlayerId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Bid(null, 50.0));
    }

    @Test
    void constructor_negativeAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Bid(playerId, -1.0));
    }

    @Test
    void constructor_negativeAmount_withMessage() {
        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> new Bid(playerId, -0.01));
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test
    void record_equals_sameValues() {
        Bid bid1 = new Bid(playerId, 100.0);
        Bid bid2 = new Bid(playerId, 100.0);
        assertEquals(bid1, bid2);
        assertEquals(bid1.hashCode(), bid2.hashCode());
    }

    @Test
    void record_equals_differentAmount() {
        Bid bid1 = new Bid(playerId, 100.0);
        Bid bid2 = new Bid(playerId, 200.0);
        assertNotEquals(bid1, bid2);
    }

    @Test
    void record_equals_differentPlayer() {
        Bid bid1 = new Bid(playerId, 100.0);
        Bid bid2 = new Bid(UUID.randomUUID(), 100.0);
        assertNotEquals(bid1, bid2);
    }

    @Test
    void record_toString_containsFields() {
        Bid bid = new Bid(playerId, 50.0);
        String str = bid.toString();
        assertTrue(str.contains(playerId.toString()));
        assertTrue(str.contains("50.0"));
    }
}
