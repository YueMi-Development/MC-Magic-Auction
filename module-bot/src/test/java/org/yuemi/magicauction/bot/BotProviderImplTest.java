package org.yuemi.magicauction.bot;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BotProviderImplTest {

    private BotProviderImpl provider;

    @Mock
    private Player realPlayer;

    @BeforeEach
    void setUp() {
        provider = new BotProviderImpl();
    }

    @Test
    void createBot_returnsNonNullPlayer() {
        Player bot = provider.createBot("TestBot");
        assertNotNull(bot);
    }

    @Test
    void createBot_setsCorrectName() {
        Player bot = provider.createBot("AuctionBot");
        assertEquals("AuctionBot", bot.getName());
    }

    @Test
    void createBot_uniqueUUIDPerCall() {
        Player bot1 = provider.createBot("Bot_1");
        Player bot2 = provider.createBot("Bot_2");
        assertNotEquals(bot1.getUniqueId(), bot2.getUniqueId());
    }

    @Test
    void isBot_returnsTrueForCreatedBot() {
        Player bot = provider.createBot("Bot_1");
        assertTrue(provider.isBot(bot));
    }

    @Test
    void isBot_returnsFalseForRealPlayer() {
        UUID realUuid = UUID.randomUUID();
        Player mockPlayer = org.mockito.Mockito.mock(Player.class);
        org.mockito.Mockito.when(mockPlayer.getUniqueId()).thenReturn(realUuid);

        assertFalse(provider.isBot(mockPlayer));
    }

    @Test
    void createBot_multipleBots_allTracked() {
        Player bot1 = provider.createBot("Bot_1");
        Player bot2 = provider.createBot("Bot_2");
        Player bot3 = provider.createBot("Bot_3");

        assertTrue(provider.isBot(bot1));
        assertTrue(provider.isBot(bot2));
        assertTrue(provider.isBot(bot3));
    }

    @Test
    void isBot_returnsFalseForUntrackedPlayer() {
        // Player with UUID that was never registered
        assertFalse(provider.isBot(org.mockito.Mockito.mock(Player.class)));
    }

    @Test
    void createBot_sameNameStillUnique() {
        Player bot1 = provider.createBot("Bot");
        Player bot2 = provider.createBot("Bot");

        assertNotEquals(bot1.getUniqueId(), bot2.getUniqueId());
    }

    @Test
    void isBot_nullSafe_differentUUID() {
        Player bot = provider.createBot("Test");
        Player fake = org.mockito.Mockito.mock(Player.class);
        org.mockito.Mockito.when(fake.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(provider.isBot(bot));
        assertFalse(provider.isBot(fake));
    }
}
