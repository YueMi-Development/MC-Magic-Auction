package org.yuemi.magicauction.plugin.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RarityColorMapperTest {

    @ParameterizedTest
    @CsvSource({
            "black, black",
            "dark_blue, dark_blue",
            "dark_green, dark_green",
            "dark_aqua, dark_aqua",
            "dark_red, dark_red",
            "dark_purple, dark_purple",
            "gold, gold",
            "gray, gray",
            "dark_gray, dark_gray",
            "blue, blue",
            "green, green",
            "aqua, aqua",
            "red, red",
            "light_purple, light_purple",
            "yellow, yellow",
            "white, white"
    })
    void knownColors_resolveCorrectly(String input, String expected) {
        assertEquals(expected, RarityColorMapper.resolve(input));
    }

    @Test
    void orangeAlias_resolvesToGold() {
        assertEquals("gold", RarityColorMapper.resolve("orange"));
    }

    @Test
    void unknownColor_fallsBackToWhite() {
        assertEquals("white", RarityColorMapper.resolve("rainbow"));
    }

    @Test
    void emptyString_fallsBackToWhite() {
        assertEquals("white", RarityColorMapper.resolve(""));
    }

    @Test
    void toTag_knownColor() {
        assertEquals("<red>", RarityColorMapper.toTag("red"));
    }

    @Test
    void toTag_orangeAlias() {
        assertEquals("<gold>", RarityColorMapper.toTag("orange"));
    }

    @Test
    void toTag_unknownColor() {
        assertEquals("<white>", RarityColorMapper.toTag("rainbow"));
    }

    @Test
    void toCloseTag_knownColor() {
        assertEquals("</red>", RarityColorMapper.toCloseTag("red"));
    }

    @Test
    void toCloseTag_orangeAlias() {
        assertEquals("</gold>", RarityColorMapper.toCloseTag("orange"));
    }

    @Test
    void toCloseTag_unknownColor() {
        assertEquals("</white>", RarityColorMapper.toCloseTag("rainbow"));
    }

    @Test
    void caseInsensitive_upper() {
        assertEquals("red", RarityColorMapper.resolve("RED"));
        assertEquals("<red>", RarityColorMapper.toTag("RED"));
    }

    @Test
    void caseInsensitive_mixed() {
        assertEquals("dark_red", RarityColorMapper.resolve("Dark_Red"));
    }
}
