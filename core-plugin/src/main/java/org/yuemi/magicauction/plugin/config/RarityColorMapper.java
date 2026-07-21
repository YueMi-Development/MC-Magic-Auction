package org.yuemi.magicauction.plugin.config;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps rarity color names to MiniMessage color tags for text rendering.
 *
 * <p>Minecraft chat color names (e.g. {@code red}, {@code dark_red}, {@code gold}) map
 * directly to MiniMessage tags ({@code <red>}, {@code <dark_red>}, {@code <gold>}),
 * but some common names (e.g. {@code orange}) are not valid MiniMessage colors.
 * This mapper handles aliases and provides a fallback for unknown colors.
 *
 * @see <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage Format</a>
 */
public final class RarityColorMapper {

    private static final Set<String> VALID_COLORS;
    private static final Map<String, String> COLOR_ALIASES;

    static {
        Set<String> colors = new HashSet<>();
        colors.add("black");
        colors.add("dark_blue");
        colors.add("dark_green");
        colors.add("dark_aqua");
        colors.add("dark_red");
        colors.add("dark_purple");
        colors.add("gold");
        colors.add("gray");
        colors.add("dark_gray");
        colors.add("blue");
        colors.add("green");
        colors.add("aqua");
        colors.add("red");
        colors.add("light_purple");
        colors.add("yellow");
        colors.add("white");
        VALID_COLORS = Collections.unmodifiableSet(colors);

        Map<String, String> aliases = new HashMap<>();
        // "orange" has no MiniMessage tag — map to the nearest visual equivalent
        aliases.put("orange", "gold");
        // "purple" has no MiniMessage tag — map to the nearest visual equivalent
        aliases.put("purple", "dark_purple");
        COLOR_ALIASES = Collections.unmodifiableMap(aliases);
    }

    private RarityColorMapper() {
    }

    /**
     * Resolve a possibly-aliased color name into a valid MiniMessage color name.
     *
     * @param colorName the lowercase color name (e.g. {@code "dark_red"}, {@code "orange"})
     * @return a valid MiniMessage color name (e.g. {@code "dark_red"}, {@code "gold"}),
     *         or {@code "white"} if unknown
     */
    @NotNull
    public static String resolve(@NotNull String colorName) {
        String lower = colorName.toLowerCase();
        if (VALID_COLORS.contains(lower)) {
            return lower;
        }
        return COLOR_ALIASES.getOrDefault(lower, "white");
    }

    /**
     * Convert a color name into a MiniMessage color tag.
     *
     * @param colorName the lowercase color name (e.g. {@code "dark_red"}, {@code "orange"})
     * @return a MiniMessage color tag (e.g. {@code "<dark_red>"}, {@code "<gold>"}),
     *         or {@code "<white>"} if the color is unknown
     */
    @NotNull
    public static String toTag(@NotNull String colorName) {
        return "<" + resolve(colorName) + ">";
    }

    /**
     * Convert a color name into a MiniMessage closing color tag.
     *
     * @param colorName the lowercase color name (e.g. {@code "dark_red"}, {@code "orange"})
     * @return a MiniMessage closing color tag (e.g. {@code "</dark_red>"}, {@code "</gold>"}),
     *         or {@code "</white>"} if the color is unknown
     */
    @NotNull
    public static String toCloseTag(@NotNull String colorName) {
        return "</" + resolve(colorName) + ">";
    }
}
