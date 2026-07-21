package org.yuemi.magicauction.plugin.config;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps rarity color names to stained glass pane {@link Material materials}.
 *
 * <p>Minecraft chat color names (e.g. {@code dark_red}, {@code gold}) differ from
 * Bukkit's glass pane material naming (e.g. {@code RED_STAINED_GLASS_PANE},
 * {@code ORANGE_STAINED_GLASS_PANE}). This mapper provides the correct
 * translation and falls back to {@link Material#GRAY_STAINED_GLASS_PANE}
 * for unknown colors.
 */
public final class GlassPaneMapper {

    private static final Map<String, Material> GLASS_PANE_MAP;
    private static final Map<String, Material> GLASS_BLOCK_MAP;

    static {
        Map<String, Material> map = new HashMap<>();
        map.put("black", Material.BLACK_STAINED_GLASS_PANE);
        map.put("dark_blue", Material.BLUE_STAINED_GLASS_PANE);
        map.put("dark_green", Material.GREEN_STAINED_GLASS_PANE);
        map.put("dark_aqua", Material.CYAN_STAINED_GLASS_PANE);
        map.put("dark_red", Material.RED_STAINED_GLASS_PANE);
        map.put("dark_purple", Material.PURPLE_STAINED_GLASS_PANE);
        map.put("gold", Material.ORANGE_STAINED_GLASS_PANE);
        map.put("orange", Material.ORANGE_STAINED_GLASS_PANE);
        map.put("gray", Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        map.put("dark_gray", Material.GRAY_STAINED_GLASS_PANE);
        map.put("blue", Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        map.put("green", Material.LIME_STAINED_GLASS_PANE);
        map.put("aqua", Material.CYAN_STAINED_GLASS_PANE);
        map.put("red", Material.RED_STAINED_GLASS_PANE);
        map.put("light_purple", Material.MAGENTA_STAINED_GLASS_PANE);
        map.put("yellow", Material.YELLOW_STAINED_GLASS_PANE);
        map.put("white", Material.WHITE_STAINED_GLASS_PANE);
        GLASS_PANE_MAP = Collections.unmodifiableMap(map);
    }

    static {
        Map<String, Material> map = new HashMap<>();
        map.put("black", Material.BLACK_STAINED_GLASS);
        map.put("dark_blue", Material.BLUE_STAINED_GLASS);
        map.put("dark_green", Material.GREEN_STAINED_GLASS);
        map.put("dark_aqua", Material.CYAN_STAINED_GLASS);
        map.put("dark_red", Material.RED_STAINED_GLASS);
        map.put("dark_purple", Material.PURPLE_STAINED_GLASS);
        map.put("gold", Material.ORANGE_STAINED_GLASS);
        map.put("orange", Material.ORANGE_STAINED_GLASS);
        map.put("gray", Material.LIGHT_GRAY_STAINED_GLASS);
        map.put("dark_gray", Material.GRAY_STAINED_GLASS);
        map.put("blue", Material.LIGHT_BLUE_STAINED_GLASS);
        map.put("green", Material.LIME_STAINED_GLASS);
        map.put("aqua", Material.CYAN_STAINED_GLASS);
        map.put("red", Material.RED_STAINED_GLASS);
        map.put("light_purple", Material.MAGENTA_STAINED_GLASS);
        map.put("yellow", Material.YELLOW_STAINED_GLASS);
        map.put("white", Material.WHITE_STAINED_GLASS);
        GLASS_BLOCK_MAP = Collections.unmodifiableMap(map);
    }

    private GlassPaneMapper() {
    }

    /**
     * Resolve a color name to its corresponding stained glass pane material.
     *
     * @param colorName the lowercase color name (e.g. {@code "dark_red"}, {@code "gold"})
     * @return the matching glass pane material, or {@link Material#GRAY_STAINED_GLASS_PANE} if unknown
     */
    @NotNull
    public static Material getMaterial(@NotNull String colorName) {
        return GLASS_PANE_MAP.getOrDefault(colorName.toLowerCase(), Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * Resolve a color name to its corresponding stained glass block material
     * (not pane). Used when size is unknown — the full glass block conveys the
     * rarity color without implying a size shape.
     *
     * @param colorName the lowercase color name (e.g. {@code "dark_red"}, {@code "gold"})
     * @return the matching glass block material, or {@link Material#GRAY_STAINED_GLASS} if unknown
     */
    @NotNull
    public static Material getBlockMaterial(@NotNull String colorName) {
        return GLASS_BLOCK_MAP.getOrDefault(colorName.toLowerCase(), Material.GRAY_STAINED_GLASS);
    }
}
