package org.yuemi.magicauction.plugin.commands.magicauction;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.yuemi.magicauction.bot.BotHandler;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.game.AuctionManager;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StartSubCommandTest {

    private AuctionManager auctionManager;
    private BotHandler botHandler;
    private CommandSender sender;
    private StartSubCommand command;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        auctionManager = mock(AuctionManager.class);
        botHandler = mock(BotHandler.class);
        sender = mock(CommandSender.class);

        var plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(auctionManager.getPlugin()).thenReturn(plugin);
        when(auctionManager.getBotHandler()).thenReturn(botHandler);
        command = new StartSubCommand(auctionManager);

        bukkitMock = mockStatic(Bukkit.class);
    }

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    @Test
    void getName_returnsStart() {
        assertEquals("start", command.getName());
    }

    @Test
    void getDescription_returnsDescription() {
        assertNotNull(command.getDescription());
    }

    @Test
    void getSyntax_returnsSyntax() {
        assertNotNull(command.getSyntax());
    }

    @Test
    void execute_tooFewArgs_sendsUsage() {
        String[] args = {"arena"};
        command.execute(sender, args);
        verify(sender, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.<Component>any());
    }

    @Test
    void execute_unknownArena_sendsError() {
        String[] args = {"nonexistent", "p1", "p2", "p3", "p4"};
        when(auctionManager.getArenaConfig("nonexistent")).thenReturn(null);

        command.execute(sender, args);
        verify(sender, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.<Component>any());
        verify(auctionManager, never()).startSession(any(), anyLong(), anyList());
    }

    @Test
    void execute_fiveArgs_noSeed_createsSession() {
        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenaConfig("test")).thenReturn(arena);

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        bukkitMock.when(() -> Bukkit.getPlayerExact("p1")).thenReturn(p1);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p2")).thenReturn(p2);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p3")).thenReturn(p3);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p4")).thenReturn(p4);

        String[] args = {"test", "p1", "p2", "p3", "p4"};
        command.execute(sender, args);

        verify(auctionManager).startSession(eq(arena), anyLong(), argThat(list -> list.size() == 4));
    }

    @Test
    void execute_withBotPlayer_createsBot() {
        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenaConfig("test")).thenReturn(arena);

        Player p1 = mock(Player.class);
        Player bot1 = mock(Player.class);
        Player bot2 = mock(Player.class);
        Player p4 = mock(Player.class);
        when(botHandler.createBot("Bot_1")).thenReturn(bot1);
        when(botHandler.createBot("Bot_2")).thenReturn(bot2);

        bukkitMock.when(() -> Bukkit.getPlayerExact("p1")).thenReturn(p1);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p4")).thenReturn(p4);

        String[] args = {"test", "p1", "_BOT_", "_BOT_", "p4"};
        command.execute(sender, args);

        verify(auctionManager).startSession(eq(arena), anyLong(), argThat(list -> list.size() == 4));
        verify(botHandler).createBot("Bot_1");
        verify(botHandler).createBot("Bot_2");
    }

    @Test
    void execute_playerNotOnline_sendsError() {
        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenaConfig("test")).thenReturn(arena);

        bukkitMock.when(() -> Bukkit.getPlayerExact("offline")).thenReturn(null);

        String[] args = {"test", "offline", "p2", "p3", "p4"};
        command.execute(sender, args);
        verify(sender, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.<Component>any());
        verify(auctionManager, never()).startSession(any(), anyLong(), anyList());
    }

    @Test
    void execute_botHandlerNull_returnsError() {
        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenaConfig("test")).thenReturn(arena);
        when(auctionManager.getBotHandler()).thenReturn(null);

        String[] args = {"test", "p1", "_BOT_", "p3", "p4"};
        command.execute(sender, args);
        verify(sender, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.<Component>any());
        verify(auctionManager, never()).startSession(any(), anyLong(), anyList());
    }

    @Test
    void tabComplete_firstArg_returnsArenaNames() {
        ArenaConfig arena1 = new ArenaConfig("arena1", "Arena1", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        ArenaConfig arena2 = new ArenaConfig("arena2", "Arena2", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenas()).thenReturn(List.of(arena1, arena2));

        String[] args = {"a"};
        List<String> completions = command.tabComplete(sender, args);
        assertEquals(2, completions.size());
        assertTrue(completions.contains("arena1"));
        assertTrue(completions.contains("arena2"));
    }

    @Test
    void tabComplete_secondArg_returnsOnlinePlayers() {
        Player p1 = mock(Player.class);
        when(p1.getName()).thenReturn("Player1");
        Player p2 = mock(Player.class);
        when(p2.getName()).thenReturn("Player2");

        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(List.of(p1, p2));

        String[] args = {"test", ""};
        List<String> completions = command.tabComplete(sender, args);
        assertEquals(2, completions.size());
        assertTrue(completions.contains("Player1"));
        assertTrue(completions.contains("Player2"));
    }

    @Test
    void execute_sixArgs_withSeed() {
        ArenaConfig arena = new ArenaConfig("test", "Test", 15, 30, 100.0,
                List.of(2.0), List.of(), List.of(), 1, 1, 0);
        when(auctionManager.getArenaConfig("test")).thenReturn(arena);

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        bukkitMock.when(() -> Bukkit.getPlayerExact("p1")).thenReturn(p1);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p2")).thenReturn(p2);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p3")).thenReturn(p3);
        bukkitMock.when(() -> Bukkit.getPlayerExact("p4")).thenReturn(p4);

        String[] args = {"test", "42", "p1", "p2", "p3", "p4"};
        command.execute(sender, args);

        verify(auctionManager).startSession(eq(arena), eq(42L), argThat(list -> list.size() == 4));
    }

    @Test
    void execute_invalidSeed_sendsError() {
        String[] args = {"test", "notanumber", "p1", "p2", "p3", "p4"};
        command.execute(sender, args);
        verify(sender, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.<Component>any());
        verify(auctionManager, never()).startSession(any(), anyLong(), anyList());
    }

    @Test
    void tabComplete_unknownFirstArg_returnsEmptyList() {
        String[] args = {};
        List<String> completions = command.tabComplete(sender, args);
        assertTrue(completions.isEmpty());
    }
}
