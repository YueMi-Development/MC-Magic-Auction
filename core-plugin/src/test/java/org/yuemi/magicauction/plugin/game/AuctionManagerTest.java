package org.yuemi.magicauction.plugin.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionManagerTest {

    @Mock
    private JavaPlugin plugin;

    @Test
    void constructor_storesPlugin() {
        AuctionManager manager = new AuctionManager(plugin);
        assertSame(plugin, manager.getPlugin());
    }

    @Test
    void setBotHandler_allowsInjection() {
        AuctionManager manager = new AuctionManager(plugin);
        manager.setBotHandler(null);
        assertNull(manager.getBotHandler());
    }

    @Test
    void isBot_nullHandler_returnsFalse() {
        AuctionManager manager = new AuctionManager(plugin);
        manager.setBotHandler(null);
        Player player = mock(Player.class);
        assertFalse(manager.isBot(player));
    }

    @Test
    void getItemConfig_unknownId_returnsNull() {
        AuctionManager manager = new AuctionManager(plugin);
        assertNull(manager.getItemConfig("nonexistent"));
    }

    @Test
    void getArenaConfig_unknownId_returnsNull() {
        AuctionManager manager = new AuctionManager(plugin);
        assertNull(manager.getArenaConfig("nonexistent"));
    }

    @Test
    void getArenas_emptyInitially() {
        AuctionManager manager = new AuctionManager(plugin);
        assertTrue(manager.getArenas().isEmpty());
    }

    @Test
    void isPlayerInSession_returnsFalse() {
        AuctionManager manager = new AuctionManager(plugin);
        Player player = mock(Player.class);
        assertFalse(manager.isPlayerInSession(player));
    }
}
