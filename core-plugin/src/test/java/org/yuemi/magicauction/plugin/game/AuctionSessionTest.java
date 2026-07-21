package org.yuemi.magicauction.plugin.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.yuemi.magicauction.plugin.config.ArenaConfig;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionSessionTest {

    private JavaPlugin createMockPlugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(new File("build/tmp/test_data"));
        return plugin;
    }

    private Player createMockPlayer(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        return player;
    }

    private AuctionManager createManager(JavaPlugin plugin) {
        AuctionManager manager = new AuctionManager(plugin);
        return manager;
    }

    @Test
    void constructor_createsSessionWithFourPlayers() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player p1 = createMockPlayer("Player1");
        Player p2 = createMockPlayer("Player2");
        Player p3 = createMockPlayer("Player3");
        Player p4 = createMockPlayer("Player4");

        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0, 1.5), List.of(), List.of(), 1, 1, 0);

        AuctionSession session = new AuctionSession(manager, arena,
                Arrays.asList(p1, p2, p3, p4), 12345L);

        assertEquals(4, session.getPlayers().size());
        assertTrue(session.getPlayers().contains(p1));
        assertTrue(session.getPlayers().contains(p2));
        assertTrue(session.getPlayers().contains(p3));
        assertTrue(session.getPlayers().contains(p4));
    }

    @Test
    void constructor_withBots_createsSession() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player p1 = createMockPlayer("Player1");
        Player p2 = createMockPlayer("Player2");
        Player bot1 = createMockPlayer("Bot_1");
        Player bot2 = createMockPlayer("Bot_2");

        ArenaConfig arena = new ArenaConfig("test_bots", "Test Bots", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);

        AuctionSession session = new AuctionSession(manager, arena,
                Arrays.asList(p1, p2, bot1, bot2), 99999L);

        assertEquals(4, session.getPlayers().size());
    }

    @Test
    void constructor_emptyRewards_noCrash() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player p1 = createMockPlayer("P1");
        Player p2 = createMockPlayer("P2");
        Player p3 = createMockPlayer("P3");
        Player p4 = createMockPlayer("P4");

        ArenaConfig arena = new ArenaConfig("empty", "Empty", 15, 30, 100.0,
                List.of(2.0, 1.5), List.of(), List.of(), 1, 1, 0);

        assertDoesNotThrow(() -> new AuctionSession(manager, arena,
                Arrays.asList(p1, p2, p3, p4), 12345L));
    }

    @Test
    void handlePlayerDisconnect_noOtherRealPlayers_succeeds() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player real = createMockPlayer("RealPlayer");
        Player bot1 = createMockPlayer("Bot_1");
        Player bot2 = createMockPlayer("Bot_2");
        Player bot3 = createMockPlayer("Bot_3");

        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0, 1.5), List.of(), List.of(), 1, 1, 0);

        AuctionSession session = new AuctionSession(manager, arena,
                Arrays.asList(real, bot1, bot2, bot3), 12345L);

        assertDoesNotThrow(() -> session.handlePlayerDisconnect(real));
    }

    @Test
    void handlePlayerDisconnect_otherRealPlayersRemain() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player p1 = createMockPlayer("Player1");
        Player p2 = createMockPlayer("Player2");
        Player bot1 = createMockPlayer("Bot_1");
        Player bot2 = createMockPlayer("Bot_2");

        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0, 1.5), List.of(), List.of(), 1, 1, 0);

        AuctionSession session = new AuctionSession(manager, arena,
                Arrays.asList(p1, p2, bot1, bot2), 12345L);

        assertDoesNotThrow(() -> session.handlePlayerDisconnect(p1));
    }

    @Test
    void session_seedDeterminism() {
        JavaPlugin plugin = createMockPlugin();
        AuctionManager manager = createManager(plugin);

        Player p1 = createMockPlayer("P1");
        Player p2 = createMockPlayer("P2");
        Player p3 = createMockPlayer("P3");
        Player p4 = createMockPlayer("P4");
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        ArenaConfig arena = new ArenaConfig("seed_test", "Seed Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);

        // Two sessions with same seed should create identical player lists
        AuctionSession s1 = new AuctionSession(manager, arena, players, 42L);
        AuctionSession s2 = new AuctionSession(manager, arena, players, 42L);

        assertEquals(s1.getPlayers(), s2.getPlayers());
    }
}
