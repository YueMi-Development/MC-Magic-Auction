package org.yuemi.magicauction.seed;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RandomSeedGeneratorTest {

    private final RandomSeedGenerator generator = new RandomSeedGenerator();

    @Test
    void generateSeed_returnsPositiveValue() {
        long seed = generator.generateSeed();
        assertTrue(seed > 0, "Seed must be positive, got " + seed);
    }

    @Test
    void generateSeed_neverReturnsZero() {
        for (int i = 0; i < 1000; i++) {
            long seed = generator.generateSeed();
            assertNotEquals(0, seed, "Seed must not be zero");
        }
    }

    @Test
    void generateSeed_returnsValueWithinBounds() {
        long seed = generator.generateSeed();
        assertTrue(seed <= 1_000_000_000L,
                "Seed must be <= 1_000_000_000, got " + seed);
    }

    @Test
    void generateSeed_multipleCalls_differentValues() {
        long s1 = generator.generateSeed();
        long s2 = generator.generateSeed();
        // Statistically extremely unlikely to collide
        assertNotEquals(s1, s2);
    }

    @RepeatedTest(100)
    void generateSeed_alwaysInValidRange() {
        long seed = generator.generateSeed();
        assertTrue(seed > 0 && seed <= 1_000_000_000L,
                "Seed " + seed + " out of valid range (1, 1_000_000_000]");
    }

    @Test
    void generateSeed_producesDistinctSeeds() {
        Set<Long> seeds = new HashSet<>();
        int count = 100;
        for (int i = 0; i < count; i++) {
            seeds.add(generator.generateSeed());
        }
        // With 100 draws from 1e9 range, collisions are astronomically unlikely
        assertEquals(count, seeds.size(), "Should produce distinct seeds");
    }

    @Test
    void implementsSeedGenerator() {
        assertInstanceOf(SeedGenerator.class, generator);
    }

    @Test
    void resolve_zero_generatesRandomSeed() {
        long result = generator.resolve(0);
        assertTrue(result > 0);
        assertTrue(result <= 1_000_000_000L);
    }

    @Test
    void resolve_positive_returnsAsIs() {
        assertEquals(999L, generator.resolve(999));
    }

    @Test
    void resolve_negative_returnsAbsolute() {
        long result = generator.resolve(-777);
        assertEquals(777L, result);
        assertTrue(result > 0);
    }
}
