package org.yuemi.magicauction.seed;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates seeds backed by {@link ThreadLocalRandom}.
 *
 * <p>Each call to {@link #generateSeed()} produces an independent random
 * positive long suitable for seeding a session's {@link java.util.Random}.
 */
public final class RandomSeedGenerator implements SeedGenerator {

    private static final long MAX_SEED = 1_000_000_000L;

    @Override
    public long generateSeed() {
        long seed;
        do {
            seed = ThreadLocalRandom.current().nextLong(MAX_SEED) + 1;
        } while (seed <= 0);
        return seed;
    }
}
