package org.yuemi.magicauction.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotPlayerProxyTest {

    private static final String BOT_NAME = "TestBot";
    private static final UUID BOT_UUID = UUID.randomUUID();
    private static MockedStatic<Bukkit> bukkitMock;

    @BeforeAll
    static void setUp() {
        bukkitMock = mockStatic(Bukkit.class);
        World world = mock(World.class);
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(Bukkit.getWorlds()).thenReturn(List.of(world));
    }

    @AfterAll
    static void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    @Test
    void proxy_isInstanceOfPlayer() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertInstanceOf(Player.class, bot);
    }

    @Test
    void getName_returnsBotName() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertEquals(BOT_NAME, bot.getName());
    }

    @Test
    void getDisplayName_returnsBotName() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertEquals(BOT_NAME, bot.getDisplayName());
    }

    @Test
    void getUniqueId_returnsAssignedUuid() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertEquals(BOT_UUID, bot.getUniqueId());
    }

    @Test
    void isOnline_returnsTrue() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertTrue(bot.isOnline());
    }

    @Test
    void isValid_returnsTrue() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertTrue(bot.isValid());
    }

    @Test
    void sendMessage_doesNotThrow() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertDoesNotThrow(() -> bot.sendMessage("Hello"));
        assertDoesNotThrow(() -> bot.sendRichMessage("<red>Hello</red>"));
    }

    @Test
    void closeInventory_doesNotThrow() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertDoesNotThrow(() -> bot.closeInventory());
    }

    @Test
    void hashCode_returnsUuidHashCode() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertEquals(BOT_UUID.hashCode(), bot.hashCode());
    }

    @Test
    void equals_sameUuid_returnsTrue() {
        Player bot1 = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        Player bot2 = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertEquals(bot1, bot2);
    }

    @Test
    void equals_differentUuid_returnsFalse() {
        Player bot1 = BotPlayerProxy.create("A", BOT_UUID);
        Player bot2 = BotPlayerProxy.create("B", UUID.randomUUID());
        assertNotEquals(bot1, bot2);
    }

    @Test
    void equals_null_returnsFalse() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertNotEquals(null, bot);
    }

    @Test
    void equals_nonPlayerObject_returnsFalse() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertNotEquals("string", bot);
    }

    @Test
    void toString_containsNameAndUuid() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        String str = bot.toString();
        assertTrue(str.contains(BOT_NAME));
        assertTrue(str.contains(BOT_UUID.toString()));
    }

    @Test
    void getWorld_returnsFirstWorld() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertNotNull(bot.getWorld());
    }

    @Test
    void getLocation_returnsSpawn() {
        Player bot = BotPlayerProxy.create(BOT_NAME, BOT_UUID);
        assertNotNull(bot.getLocation());
    }
}
