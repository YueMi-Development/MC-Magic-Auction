package org.yuemi.magicauction.seed;

/**
 * Generates and resolves seed values for auction sessions.
 *
 * <p>A seed is used to deterministically control prize shuffling, packing,
 * event selection, and any other random processes within an auction session.
 * The same seed always produces the same auction layout and event order.
 *
 * <p>Resolution rules (see {@link #resolve(long)}):
 * <ul>
 *   <li>{@code 0} &rarr; a random positive seed is generated via {@link #generateSeed()}</li>
 *   <li>{@code < 0} &rarr; the absolute value is used ({@code -42} becomes {@code 42})</li>
 *   <li>{@code > 0} &rarr; used as-is</li>
 * </ul>
 */
@FunctionalInterface
public interface SeedGenerator {

    /**
     * Generate a random positive seed value.
     *
     * @return a positive long suitable for seeding a {@link java.util.Random}
     */
    long generateSeed();

    /**
     * Resolve a raw seed input into a valid positive seed.
     *
     * <ul>
     *   <li>{@code 0} &rarr; delegates to {@link #generateSeed()} for a random seed</li>
     *   <li>{@code < 0} &rarr; returns {@code Math.abs(raw)}</li>
     *   <li>{@code > 0} &rarr; returns {@code raw} unchanged</li>
     * </ul>
     *
     * @param raw the raw seed value from user input
     * @return a positive seed long
     */
    default long resolve(long raw) {
        if (raw == 0) {
            return generateSeed();
        }
        return Math.abs(raw);
    }
}
