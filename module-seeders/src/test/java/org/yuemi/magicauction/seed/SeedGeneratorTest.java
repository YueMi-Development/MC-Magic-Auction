package org.yuemi.magicauction.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SeedGeneratorTest {

    private final SeedGenerator generator = () -> 42L; // deterministic mock

    @Test
    void resolve_positiveValue_returnsAsIs() {
        assertEquals(42L, generator.resolve(42));
    }

    @Test
    void resolve_one_returnsOne() {
        assertEquals(1L, generator.resolve(1));
    }

    @Test
    void resolve_maxLong_returnsMaxLong() {
        assertEquals(Long.MAX_VALUE, generator.resolve(Long.MAX_VALUE));
    }

    @Test
    void resolve_negativeValue_returnsAbsolute() {
        assertEquals(42L, generator.resolve(-42));
    }

    @Test
    void resolve_negativeOne_returnsOne() {
        assertEquals(1L, generator.resolve(-1));
    }

    @Test
    void resolve_zero_delegatesToGenerateSeed() {
        assertEquals(42L, generator.resolve(0));
    }

    @Test
    void resolve_zero_callsGenerateSeedExactlyOnce() {
        final long[] callCount = {0};
        SeedGenerator countingGen = () -> {
            callCount[0]++;
            return 99L;
        };
        assertEquals(99L, countingGen.resolve(0));
        assertEquals(1, callCount[0]);
    }

    @Test
    void resolve_positiveValue_doesNotCallGenerateSeed() {
        final boolean[] wasCalled = {false};
        SeedGenerator guarded = () -> {
            wasCalled[0] = true;
            return 99L;
        };
        assertEquals(42L, guarded.resolve(42));
        assertFalse(wasCalled[0]);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 100, Long.MAX_VALUE})
    void resolve_positiveValues_roundTrip(long value) {
        assertEquals(value, generator.resolve(value));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, -999999})
    void resolve_negativeValues_absoluteAndPositive(long value) {
        long result = generator.resolve(value);
        assertEquals(Math.abs(value), result);
        assertTrue(result > 0);
    }

    @Test
    void resolve_longMinValue_overflowBehavior() {
        // Math.abs(Long.MIN_VALUE) = Long.MIN_VALUE due to overflow
        // The implementation uses Math.abs, so we document this behavior
        long result = generator.resolve(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, result);
        // This is a known limitation of the Math.abs approach
    }

    @Test
    void resolve_zero_withRandomGenerator_returnsPositiveValue() {
        SeedGenerator randomGen = new RandomSeedGenerator();
        long result = randomGen.resolve(0);
        assertTrue(result > 0, "Generated seed must be positive");
    }

    @Test
    void functionalInterface_canBeUsedAsLambda() {
        SeedGenerator lambda = () -> 12345L;
        assertEquals(12345L, lambda.generateSeed());
        assertEquals(12345L, lambda.resolve(0));
    }
}
