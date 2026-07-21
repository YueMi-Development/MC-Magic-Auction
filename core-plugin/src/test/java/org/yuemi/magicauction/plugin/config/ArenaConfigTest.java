package org.yuemi.magicauction.plugin.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArenaConfigTest {

    @TempDir
    File tempDir;

    private File createYaml(String filename, String content) throws IOException {
        File file = new File(tempDir, filename);
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        }
        return file;
    }

    @Test
    void load_fullConfig_parsesCorrectly() throws IOException {
        File file = createYaml("test_arena.yml", "" +
                "id: myarena\n" +
                "name: My Arena\n" +
                "thinking-time: 10\n" +
                "bid-time: 20\n" +
                "base-price: 500.0\n" +
                "multipliers:\n" +
                "  - 3.0\n" +
                "  - 2.0\n" +
                "  - 1.0\n" +
                "rewards:\n" +
                "  - id: diamond\n" +
                "    amount: 2\n" +
                "  - id: gold_ingot\n" +
                "events:\n" +
                "  - reveal_type\n" +
                "min-items: 3\n" +
                "max-items: 5\n" +
                "start-events: 2\n");

        ArenaConfig config = ArenaConfig.load(file);

        assertEquals("myarena", config.getId());
        assertEquals("My Arena", config.getName());
        assertEquals(10, config.getThinkingTime());
        assertEquals(20, config.getBidDuration());
        assertEquals(500.0, config.getBasePrice(), 0.0001);
        assertEquals(List.of(3.0, 2.0, 1.0), config.getMultipliers());
        assertEquals(2, config.getRewards().size());
        assertEquals("diamond", config.getRewards().get(0).getItemId());
        assertEquals(2, config.getRewards().get(0).getAmount());
        assertEquals("gold_ingot", config.getRewards().get(1).getItemId());
        assertEquals(1, config.getRewards().get(1).getAmount());
        assertEquals(List.of("reveal_type"), config.getEvents());
        assertEquals(3, config.getMinItems());
        assertEquals(5, config.getMaxItems());
        assertEquals(2, config.getStartEvents());
    }

    @Test
    void load_defaultId_usesFilename() throws IOException {
        File file = createYaml("arena_custom.yml", "" +
                "name: Custom\n");

        ArenaConfig config = ArenaConfig.load(file);
        assertEquals("arena_custom", config.getId());
    }

    @Test
    void load_missingName_defaultsToId() throws IOException {
        File file = createYaml("test.yml", "");

        ArenaConfig config = ArenaConfig.load(file);
        assertEquals(config.getId(), config.getName());
    }

    @Test
    void load_defaultValues() throws IOException {
        File file = createYaml("defaults.yml", "");

        ArenaConfig config = ArenaConfig.load(file);
        assertEquals(15, config.getThinkingTime());
        assertEquals(30, config.getBidDuration());
        assertEquals(100.0, config.getBasePrice(), 0.0001);
        assertEquals(List.of(2.0, 1.5, 1.3, 1.1, 1.0), config.getMultipliers());
        assertEquals(0, config.getMinItems());
        assertEquals(0, config.getMaxItems());
        assertEquals(0, config.getStartEvents());
    }

    @Test
    void load_emptyMultipliers_defaultsToStandard() throws IOException {
        File file = createYaml("empty_mult.yml", "" +
                "multipliers: []\n");

        ArenaConfig config = ArenaConfig.load(file);
        assertEquals(List.of(2.0, 1.5, 1.3, 1.1, 1.0), config.getMultipliers());
    }

    @Test
    void load_minItemsMaxItems_defaultToRewardCount() throws IOException {
        File file = createYaml("reward_count.yml", "" +
                "rewards:\n" +
                "  - id: a\n" +
                "  - id: b\n" +
                "    amount: 3\n");

        ArenaConfig config = ArenaConfig.load(file);
        // Total reward count = 1 + 3 = 4
        assertEquals(4, config.getMinItems());
        assertEquals(4, config.getMaxItems());
    }

    @Test
    void load_minItemsGreaterThanMax_throws() throws IOException {
        File file = createYaml("bad.yml", "" +
                "min-items: 10\n" +
                "max-items: 5\n");

        assertThrows(IllegalArgumentException.class, () -> ArenaConfig.load(file));
    }

    @Test
    void constructor_clampsThinkingTimeMin() {
        ArenaConfig config = new ArenaConfig("t", "t", -5, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        assertEquals(1, config.getThinkingTime());
    }

    @Test
    void constructor_clampsBidDurationMin() {
        ArenaConfig config = new ArenaConfig("t", "t", 15, 1, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        assertEquals(5, config.getBidDuration());
    }

    @Test
    void constructor_clampsBasePriceMin() {
        ArenaConfig config = new ArenaConfig("t", "t", 15, 30, -50.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        assertEquals(0.0, config.getBasePrice(), 0.0001);
    }

    @Test
    void constructor_emptyMultipliersDefaults() {
        ArenaConfig config = new ArenaConfig("t", "t", 15, 30, 100.0,
                List.of(), List.of(), List.of(), 1, 1, 0);
        assertEquals(List.of(2.0, 1.5, 1.3, 1.1, 1.0), config.getMultipliers());
    }

    @Test
    void prizeEntry_amountClampedToMin() {
        ArenaConfig.PrizeEntry entry = new ArenaConfig.PrizeEntry("test", 0);
        assertEquals(1, entry.getAmount());
        assertEquals("test", entry.getItemId());
    }

    @Test
    void load_missingRewards_returnsEmptyList() throws IOException {
        File file = createYaml("no_rewards.yml", "");

        ArenaConfig config = ArenaConfig.load(file);
        assertTrue(config.getRewards().isEmpty());
    }

    @Test
    void load_startEventsDefaultZero() throws IOException {
        File file = createYaml("no_events.yml", "");

        ArenaConfig config = ArenaConfig.load(file);
        assertEquals(0, config.getStartEvents());
    }
}
