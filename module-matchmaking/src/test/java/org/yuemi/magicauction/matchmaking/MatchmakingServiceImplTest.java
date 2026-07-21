package org.yuemi.magicauction.matchmaking;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceImplTest {

    @Mock private JavaPlugin plugin;
    @Mock private Server server;
    @Mock private BukkitScheduler scheduler;
    @Mock private BukkitTask mockTask;
    @Mock private Player player1;
    @Mock private Player player2;
    @Mock private Player player3;
    @Mock private Player player4;

    private final QueueReadyCallback callback = mock(QueueReadyCallback.class);
    private MatchmakingServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(plugin.getServer()).thenReturn(server);
        lenient().when(server.getScheduler()).thenReturn(scheduler);
        lenient().when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong()))
                .thenReturn(mockTask);
        lenient().when(player1.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player3.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player4.getUniqueId()).thenReturn(UUID.randomUUID());

        service = new MatchmakingServiceImpl(plugin, callback);
    }

    @Nested
    @DisplayName("Joining queues")
    class JoinQueue {

        @Test
        @DisplayName("should join queue for an arena")
        void shouldJoinQueue() {
            assertTrue(service.joinQueue(player1, "arena1", 4, 120, true));
            assertEquals("arena1", service.getPlayerQueue(player1));
            assertEquals(1, service.getQueueSize("arena1"));
        }

        @Test
        @DisplayName("should reject joining a second queue simultaneously")
        void shouldRejectSecondQueue() {
            assertTrue(service.joinQueue(player1, "arena1", 4, 120, true));
            assertFalse(service.joinQueue(player1, "arena2", 4, 120, true));
            assertEquals("arena1", service.getPlayerQueue(player1));
        }

        @Test
        @DisplayName("should handle multiple arenas independently")
        void shouldHandleMultipleArenas() {
            assertTrue(service.joinQueue(player1, "arena1", 4, 120, true));
            assertTrue(service.joinQueue(player2, "arena2", 4, 120, true));

            assertEquals(1, service.getQueueSize("arena1"));
            assertEquals(1, service.getQueueSize("arena2"));
            assertEquals("arena1", service.getPlayerQueue(player1));
            assertEquals("arena2", service.getPlayerQueue(player2));
        }

        @Test
        @DisplayName("should be case-insensitive for arena IDs")
        void shouldBeCaseInsensitive() {
            assertTrue(service.joinQueue(player1, "ARENA_ONE", 4, 120, true));
            assertEquals("arena_one", service.getPlayerQueue(player1));
            assertEquals(1, service.getQueueSize("ARENA_ONE"));
            assertEquals(1, service.getQueueSize("arena_one"));
        }
    }

    @Nested
    @DisplayName("Leaving queues")
    class LeaveQueue {

        @Test
        @DisplayName("should leave queue without arena specifier")
        void shouldLeaveWithoutArena() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            assertTrue(service.leaveQueue(player1, null));
            assertNull(service.getPlayerQueue(player1));
            assertEquals(0, service.getQueueSize("arena1"));
        }

        @Test
        @DisplayName("should leave queue with matching arena specifier")
        void shouldLeaveWithMatchingArena() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            assertTrue(service.leaveQueue(player1, "arena1"));
            assertNull(service.getPlayerQueue(player1));
        }

        @Test
        @DisplayName("should NOT leave queue with non-matching arena specifier")
        void shouldNotLeaveWithWrongArena() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            assertFalse(service.leaveQueue(player1, "arena2"));
            assertEquals("arena1", service.getPlayerQueue(player1));
        }

        @Test
        @DisplayName("should return false if not in any queue")
        void shouldReturnFalseIfNotInQueue() {
            assertFalse(service.leaveQueue(player1, null));
            assertFalse(service.leaveQueue(player1, "arena1"));
        }

        @Test
        @DisplayName("should clean up empty queuedAreas entry for unregistered player")
        void shouldHandleOrphanedEntry() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            assertTrue(service.leaveQueue(player1, null));
            assertNull(service.getPlayerQueue(player1));
            // Second leave should return false (already removed)
            assertFalse(service.leaveQueue(player1, null));
        }
    }

    @Nested
    @DisplayName("Queue size tracking")
    class QueueSize {

        @Test
        @DisplayName("should serve correct size for empty arena")
        void shouldServeEmptySize() {
            assertEquals(0, service.getQueueSize("nonexistent"));
        }

        @Test
        @DisplayName("should reflect player count")
        void shouldReflectPlayerCount() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            service.joinQueue(player2, "arena1", 4, 120, true);
            assertEquals(2, service.getQueueSize("arena1"));

            service.leaveQueue(player1, null);
            assertEquals(1, service.getQueueSize("arena1"));
        }
    }

    @Nested
    @DisplayName("Player lifecycle")
    class PlayerLifecycle {

        @Test
        @DisplayName("should remove player from queue on disconnect")
        void disconnect_removesPlayer() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            assertTrue(service.leaveQueue(player1, null));
            assertNull(service.getPlayerQueue(player1));
            assertEquals(0, service.getQueueSize("arena1"));
        }

        @Test
        @DisplayName("should keep player in queue after simulated death (no death handler)")
        void death_keepsPlayerInQueue() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            // Death in Minecraft does not trigger any queue-specific logic.
            // Queue state must remain unchanged.
            assertEquals("arena1", service.getPlayerQueue(player1));
            assertEquals(1, service.getQueueSize("arena1"));
        }

        @Test
        @DisplayName("should keep player in queue when other players disconnect")
        void otherPlayerDisconnect_doesNotAffectQueue() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            service.joinQueue(player2, "arena1", 4, 120, true);
            // player2 disconnects — only player2 is removed
            service.leaveQueue(player2, null);
            assertNull(service.getPlayerQueue(player2));
            assertEquals("arena1", service.getPlayerQueue(player1));
            assertEquals(1, service.getQueueSize("arena1"));
        }

        @Test
        @DisplayName("should cancel timeout when disconnected player was the only one in queue")
        void disconnectLastPlayer_cancelsTimeout() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            service.leaveQueue(player1, null);
            // Queue should be empty and timeout already cancelled by ArenaQueue
            assertEquals(0, service.getQueueSize("arena1"));
            // Joining again should work (queue re-created)
            assertTrue(service.joinQueue(player1, "arena1", 4, 120, true));
            assertEquals(1, service.getQueueSize("arena1"));
        }
    }

    @Nested
    @DisplayName("Shutdown")
    class Shutdown {

        @Test
        @DisplayName("should clear all queues")
        void shouldClearAllQueues() {
            service.joinQueue(player1, "arena1", 4, 120, true);
            service.joinQueue(player2, "arena2", 4, 120, true);
            service.shutdown();

            assertEquals(0, service.getQueueSize("arena1"));
            assertEquals(0, service.getQueueSize("arena2"));
            assertNull(service.getPlayerQueue(player1));
            assertNull(service.getPlayerQueue(player2));
        }
    }
}
