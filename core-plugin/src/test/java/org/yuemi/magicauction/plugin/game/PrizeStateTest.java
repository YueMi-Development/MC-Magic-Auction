package org.yuemi.magicauction.plugin.game;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.yuemi.magicauction.plugin.config.ItemConfig;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PrizeStateTest {

    @Mock
    private ItemStack mockStack;

    @Mock
    private ItemConfig mockConfig;

    private PrizeState state;
    private final int[] position = {2, 3};

    @BeforeEach
    void setUp() {
        state = new PrizeState(mockStack, mockConfig, position);
    }

    @Test
    void constructor_setsFields() {
        assertSame(mockStack, state.getOriginalStack());
        assertSame(mockConfig, state.getConfig());
        assertArrayEquals(position, state.getPosition());
        assertNotNull(state.getUniqueId());
    }

    @Test
    void uniqueId_differsPerInstance() {
        PrizeState other = new PrizeState(mockStack, mockConfig, new int[]{0, 0});
        assertNotEquals(state.getUniqueId(), other.getUniqueId());
    }

    @Test
    void initialHideState_isHideReturnsTrue() {
        assertTrue(state.isHide());
    }

    @Test
    void initialReveals_allFalse() {
        assertFalse(state.isTypeRevealed());
        assertFalse(state.isRarityRevealed());
        assertFalse(state.isSizeRevealed());
        assertFalse(state.isFullyRevealed());
    }

    @Test
    void setTypeRevealed_true() {
        state.setTypeRevealed(true);
        assertTrue(state.isTypeRevealed());
        assertFalse(state.isHide(), "Any reveal should set hide=false");
    }

    @Test
    void setRarityRevealed_true() {
        state.setRarityRevealed(true);
        assertTrue(state.isRarityRevealed());
        assertFalse(state.isHide());
    }

    @Test
    void setSizeRevealed_true() {
        state.setSizeRevealed(true);
        assertTrue(state.isSizeRevealed());
        assertFalse(state.isHide());
    }

    @Test
    void setFullyRevealed_cascadesAllFlags() {
        state.setFullyRevealed(true);
        assertTrue(state.isFullyRevealed());
        assertTrue(state.isTypeRevealed());
        assertTrue(state.isRarityRevealed());
        assertTrue(state.isSizeRevealed());
        assertFalse(state.isHide());
    }

    @Test
    void setFullyRevealed_false_individualFlagsKeepValue() {
        // fullyRevealed=false doesn't cascade backwards
        state.setTypeRevealed(true);
        state.setFullyRevealed(true);
        state.setFullyRevealed(false);
        // individual flags retain their state from setFullyRevealed(true)
        assertTrue(state.isTypeRevealed(), "Formerly cascaded flag should remain true");
        assertTrue(state.isRarityRevealed(), "Formerly cascaded flag should remain true");
    }

    @Test
    void isHide_returnsTrue_whenAllFalseAndHideTrue() {
        state.setHide(true);
        // All reveals false, hide=true
        assertTrue(state.isHide());
    }

    @Test
    void isHide_returnsFalse_afterTypeRevealed() {
        state.setTypeRevealed(true);
        assertFalse(state.isHide());
    }

    @Test
    void isHide_returnsFalse_afterRarityRevealed() {
        state.setRarityRevealed(true);
        assertFalse(state.isHide());
    }

    @Test
    void isHide_returnsFalse_afterSizeRevealed() {
        state.setSizeRevealed(true);
        assertFalse(state.isHide());
    }

    @Test
    void isHide_returnsFalse_whenFullyRevealedEvenIfHideTrue() {
        state.setHide(true);
        state.setFullyRevealed(true);
        assertFalse(state.isHide(), "fullyRevealed overrides hide");
    }

    @Test
    void isTypeRevealed_returnsTrue_whenFullyRevealed() {
        state.setFullyRevealed(true);
        assertTrue(state.isTypeRevealed());
    }

    @Test
    void isRarityRevealed_returnsTrue_whenFullyRevealed() {
        state.setFullyRevealed(true);
        assertTrue(state.isRarityRevealed());
    }

    @Test
    void isSizeRevealed_returnsTrue_whenFullyRevealed() {
        state.setFullyRevealed(true);
        assertTrue(state.isSizeRevealed());
    }

    @Test
    void setTypeRevealed_false_getterStillTrueIfFullyRevealed() {
        state.setFullyRevealed(true);
        state.setTypeRevealed(false);
        assertTrue(state.isTypeRevealed(), "Gated by isTypeRevealed() || fullyRevealed");
    }

    @Test
    void setRarityRevealed_false_getterStillTrueIfFullyRevealed() {
        state.setFullyRevealed(true);
        state.setRarityRevealed(false);
        assertTrue(state.isRarityRevealed());
    }

    @Test
    void setSizeRevealed_false_getterStillTrueIfFullyRevealed() {
        state.setFullyRevealed(true);
        state.setSizeRevealed(false);
        assertTrue(state.isSizeRevealed());
    }
}
