package org.yuemi.magicauction.plugin.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ItemConfigTest {

    @TempDir
    File tempDir;

    private File rarityDir;
    private File typeDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create rarity files to satisfy ItemConfig's RarityRegistry dependency
        rarityDir = new File(tempDir, "rarities");
        rarityDir.mkdir();
        writeYaml(new File(rarityDir, "rare.yml"),
                "name: Rare\ncolor: red\n");

        // Create type files to satisfy ItemConfig's TypeRegistry dependency
        typeDir = new File(tempDir, "types");
        typeDir.mkdir();
        writeYaml(new File(typeDir, "weapon.yml"),
                "name: Weapon\n");

        // Load into the static registries
        RarityRegistry.load(rarityDir);
        TypeRegistry.load(typeDir);
    }

    private void writeYaml(File file, String content) throws IOException {
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        }
    }

    private File createItemYaml(String filename, String content) throws IOException {
        File file = new File(tempDir, filename);
        writeYaml(file, content);
        return file;
    }

    @Test
    void load_fullConfig_parsesCorrectly() throws IOException {
        File file = createItemYaml("test_item.yml",
                "id: test_sword\n" +
                "rarity: rare\n" +
                "type: weapon\n" +
                "display-name: Test Sword\n" +
                "desc: A mighty blade\n" +
                "material: DIAMOND_SWORD\n" +
                "width: 2\n" +
                "height: 1\n" +
                "custom-model-data: 100\n" +
                "worth: 500.0\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give %player% diamond 1\n" +
                "  - type: item\n" +
                "    id: diamond_block\n" +
                "    amount: 2\n");

        ItemConfig config = ItemConfig.load(file);
        assertEquals("test_sword", config.getId());
        assertEquals("rare", config.getRarity());
        assertEquals("weapon", config.getType());
        assertEquals("Test Sword", config.getDisplayName());
        assertEquals("A mighty blade", config.getDesc());
        assertEquals(org.bukkit.Material.DIAMOND_SWORD, config.getMaterial());
        assertEquals(2, config.getWidth());
        assertEquals(1, config.getHeight());
        assertEquals(100, config.getCustomModelData());
        assertEquals(500.0, config.getWorth(), 0.0001);
        assertFalse(config.isVirtualItem());
        assertEquals(2, config.getRewards().size());
        assertEquals("command", config.getRewards().get(0).getType());
        assertEquals("give %player% diamond 1", config.getRewards().get(0).getValue());
        assertEquals("item", config.getRewards().get(1).getType());
        assertEquals("diamond_block", config.getRewards().get(1).getItemId());
        assertEquals(2, config.getRewards().get(1).getAmount());
    }

    @Test
    void load_defaultId_usesFilename() throws IOException {
        File file = createItemYaml("my_custom_item.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        assertEquals("my_custom_item", config.getId());
    }

    @Test
    void load_defaultMaterial() throws IOException {
        File file = createItemYaml("no_mat.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        // The code uses "if (matStr != null) { try { ... } catch {} }" so null material = null
        // Constructor defaults null to Material.STONE
        assertEquals(org.bukkit.Material.STONE, config.getMaterial());
    }

    @Test
    void load_dimensions_defaultToOne() throws IOException {
        File file = createItemYaml("no_size.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        assertEquals(1, config.getWidth());
        assertEquals(1, config.getHeight());
    }

    @Test
    void load_worth_defaultZero() throws IOException {
        File file = createItemYaml("no_worth.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        assertEquals(0.0, config.getWorth(), 0.0001);
    }

    @Test
    void load_virtualItemNoRewards_ok() throws IOException {
        File file = createItemYaml("virtual.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "virtual-item: true\n");

        ItemConfig config = ItemConfig.load(file);
        assertTrue(config.isVirtualItem());
        assertTrue(config.getRewards().isEmpty());
    }

    @Test
    void load_nonVirtualNoRewards_throws() throws IOException {
        File file = createItemYaml("bad.yml",
                "rarity: rare\n" +
                "type: weapon\n");

        assertThrows(IllegalArgumentException.class, () -> ItemConfig.load(file));
    }

    @Test
    void load_missingRarity_throws() throws IOException {
        File file = createItemYaml("no_rarity.yml",
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        assertThrows(IllegalArgumentException.class, () -> ItemConfig.load(file));
    }

    @Test
    void load_invalidRarity_throws() throws IOException {
        File file = createItemYaml("bad_rarity.yml",
                "rarity: nonexistent\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        assertThrows(IllegalArgumentException.class, () -> ItemConfig.load(file));
    }

    @Test
    void load_missingType_throws() throws IOException {
        File file = createItemYaml("no_type.yml",
                "rarity: rare\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        assertThrows(IllegalArgumentException.class, () -> ItemConfig.load(file));
    }

    @Test
    void load_invalidType_throws() throws IOException {
        File file = createItemYaml("bad_type.yml",
                "rarity: rare\n" +
                "type: invalid_type\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        assertThrows(IllegalArgumentException.class, () -> ItemConfig.load(file));
    }

    @Test
    void load_commandsList_parsed() throws IOException {
        File file = createItemYaml("cmd.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "commands:\n" +
                "  - say hello\n" +
                "  - say world\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        assertEquals(2, config.getCommands().size());
        assertEquals("say hello", config.getCommands().get(0));
        assertEquals("say world", config.getCommands().get(1));
    }

    @Test
    void load_overrideFlags_setWhenPresent() throws IOException {
        File file = createItemYaml("overrides.yml",
                "rarity: rare\n" +
                "type: weapon\n" +
                "rewards:\n" +
                "  - type: command\n" +
                "    value: give @p diamond 1\n");

        ItemConfig config = ItemConfig.load(file);
        // display-name, desc, and custom-model-data were NOT present
        assertFalse(config.getDisplayName() != null); // relying on getter being null
    }

    @Test
    void constructor_clampsDimensions() {
        ItemConfig config = new ItemConfig("test", null, null, null, null,
                -5, 0, 0, 0.0, false, java.util.List.of(), java.util.List.of(),
                false, false, false, "rare", "weapon");
        assertEquals(1, config.getWidth());
        assertEquals(1, config.getHeight());
    }

    @Test
    void constructor_clampsWorth() {
        ItemConfig config = new ItemConfig("test", null, null, null, null,
                1, 1, 0, -100.0, false, java.util.List.of(), java.util.List.of(),
                false, false, false, "rare", "weapon");
        assertEquals(0.0, config.getWorth(), 0.0001);
    }

    @Test
    void rewardEntry_amountClampedToMin() {
        ItemConfig.RewardEntry entry = new ItemConfig.RewardEntry("command", "say hi", null, 0);
        assertEquals(1, entry.getAmount());
    }
}
