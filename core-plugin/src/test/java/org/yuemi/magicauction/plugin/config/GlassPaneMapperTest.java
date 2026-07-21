package org.yuemi.magicauction.plugin.config;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GlassPaneMapperTest {

    @ParameterizedTest
    @CsvSource({
            "black, BLACK_STAINED_GLASS_PANE",
            "dark_blue, BLUE_STAINED_GLASS_PANE",
            "dark_green, GREEN_STAINED_GLASS_PANE",
            "dark_aqua, CYAN_STAINED_GLASS_PANE",
            "dark_red, RED_STAINED_GLASS_PANE",
            "dark_purple, PURPLE_STAINED_GLASS_PANE",
            "gold, ORANGE_STAINED_GLASS_PANE",
            "orange, ORANGE_STAINED_GLASS_PANE",
            "gray, LIGHT_GRAY_STAINED_GLASS_PANE",
            "dark_gray, GRAY_STAINED_GLASS_PANE",
            "blue, LIGHT_BLUE_STAINED_GLASS_PANE",
            "green, LIME_STAINED_GLASS_PANE",
            "aqua, CYAN_STAINED_GLASS_PANE",
            "red, RED_STAINED_GLASS_PANE",
            "light_purple, MAGENTA_STAINED_GLASS_PANE",
            "yellow, YELLOW_STAINED_GLASS_PANE",
            "white, WHITE_STAINED_GLASS_PANE"
    })
    void knownColors_mapCorrectly(String colorName, String expectedMaterialName) {
        Material expected = Material.valueOf(expectedMaterialName);
        assertEquals(expected, GlassPaneMapper.getMaterial(colorName));
    }

    @Test
    void unknownColor_returnsGrayFallback() {
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial("rainbow"));
    }

    @Test
    void emptyString_returnsGrayFallback() {
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial(""));
    }

    @Test
    void caseInsensitive_upper() {
        assertEquals(Material.RED_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial("DARK_RED"));
    }

    @Test
    void caseInsensitive_mixed() {
        assertEquals(Material.RED_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial("Dark_Red"));
    }

    @Test
    void goldAndOrange_bothMapToOrange() {
        assertEquals(Material.ORANGE_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial("gold"));
        assertEquals(Material.ORANGE_STAINED_GLASS_PANE, GlassPaneMapper.getMaterial("orange"));
    }
}
