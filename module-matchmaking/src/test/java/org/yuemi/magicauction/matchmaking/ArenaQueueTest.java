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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArenaQueueTest {

    @Mock private JavaPlugin plugin;
    @Mock private Server server;
    @Mock private BukkitScheduler scheduler;
    @Mock private BukkitTask mockTask;
    @Mock private Player player1;
    @Mock private Player player2;
    @Mock private Player player3;
    @Mock private Player player4;

    private final QueueReadyCallback callback = mock(QueueReadyCallback.class);
    private ArenaQueue queue;

    @BeforeEach
    void setUp() {
        lenient().when(plugin.getServer()).thenReturn(server);
        lenient().when(server.getScheduler()).thenReturn(scheduler);
        lenient().when(scheduler.runTaskLater(any(JavaPlugin.class), any(Runnable.class), anyLong()))
                .thenReturn(mockTask);

        // Give each player mock a unique UUID so they are treated as distinct
        lenient().when(player1.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player3.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player4.getUniqueId()).thenReturn(UUID.randomUUID());

        queue = new ArenaQueue("test_arena", 4, 120, true, callback, plugin);
    }

    @Nested
    @DisplayName("Adding players")
    class AddPlayer {

        @Test
        @DisplayName("should accept players up to minPlayers")
        void shouldAcceptPlayers() {
            assertTrue(queue.addPlayer(player1));
            assertTrue(queue.addPlayer(player2));
            assertTrue(queue.addPlayer(player3));
            assertTrue(queue.addPlayer(player4));
            assertEquals(4, queue.getQueueSize());
        }

        @Test
        @DisplayName("should reject duplicate player")
        void shouldRejectDuplicate() {
            assertTrue(queue.addPlayer(player1));
            assertFalse(queue.addPlayer(player1));
        }

        @Test
        @DisplayName("should be empty initially")
        void shouldBeEmptyInitially() {
            assertTrue(queue.isEmpty());
            assertEquals(0, queue.getQueueSize());
        }

        @Test
        @DisplayName("should fire callback immediately when minPlayers reached")
        void shouldFireOnMinPlayers() {
            queue.addPlayer(player1);
            queue.addPlayer(player2);
            queue.addPlayer(player3);

            // Not yet — only 3 of 4
            verify(callback, never()).onQueueReady(anyString(), anyList(), anyBoolean());

            queue.addPlayer(player4);

            // 4th player triggers the callback
            verify(callback, times(1)).onQueueReady(eq("test_arena"), anyList(), eq(false));
        }

        @Test
        @DisplayName("should reject new players after queue has started")
        void shouldRejectAfterStart() {
            queue.addPlayer(player1);
            queue.addPlayer(player2);
            queue.addPlayer(player3);
            queue.addPlayer(player4); // triggers start

            assertFalse(queue.addPlayer(player1)); // already started
        }

        @Test
        @DisplayName("should save old task when removing a player")
        void shouldBeInQueueAfterAdd() {
            queue.addPlayer(player1);
            assertEquals("test_arena", queue.getPlayerQueue(player1.getUniqueId()));
        }
    }

    @Nested
    @DisplayName("Removing players")
    class RemovePlayer {

        @Test
        @DisplayName("should remove player and decrement count")
        void shouldRemovePlayer() {
            queue.addPlayer(player1);
            queue.addPlayer(player2);
            assertEquals(2, queue.getQueueSize());

            assertTrue(queue.removePlayer(player1.getUniqueId()));
            assertEquals(1, queue.getQueueSize());
        }

        @Test
        @DisplayName("should return false for non-existent player")
        void shouldReturnFalseForMissing() {
            assertFalse(queue.removePlayer(UUID.randomUUID()));
        }

        @Test
        @DisplayName("should cancel timeout when queue becomes empty")
        void shouldCancelTimeoutOnEmpty() {
            queue.addPlayer(player1); // starts timeout
            queue.addPlayer(player2);
            queue.removePlayer(player1.getUniqueId());
            queue.removePlayer(player2.getUniqueId());

            assertTrue(queue.isEmpty());
            verify(mockTask).cancel();
        }
    }

    @Nested
    @DisplayName("Timeout behavior")
    class Timeout {

        @Test
        @DisplayName("should start timeout on first player")
        void shouldStartTimeoutOnFirstPlayer() {
            queue.addPlayer(player1);
            verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(120 * 20L));
        }

        @Test
        @DisplayName("should not start timeout again for subsequent players")
        void shouldNotStartMultipleTimeouts() {
            queue.addPlayer(player1);
            queue.addPlayer(player2);
            queue.addPlayer(player3);

            verify(scheduler, times(1)).runTaskLater(eq(plugin), any(Runnable.class), anyLong());
        }

        @Test
        @DisplayName("should fire callback with timedOut=true when timeout triggers")
        void shouldFireOnTimeout() {
            // Capture the timeout runnable
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            queue.addPlayer(player1);
            queue.addPlayer(player2); // only 2 of 4, not enough for immediate start

            verify(scheduler).runTaskLater(eq(plugin), captor.capture(), eq(120 * 20L));
            verify(callback, never()).onQueueReady(anyString(), anyList(), anyBoolean());

            // Run the timeout
            captor.getValue().run();

            verify(callback, times(1)).onQueueReady(eq("test_arena"), anyList(), eq(true));
        }

        @Test
        @DisplayName("should cancel timeout and not fire when minPlayers reached before timeout")
        void shouldNotFireTimeoutAfterMinPlayers() {
            queue.addPlayer(player1); // starts timeout
            queue.addPlayer(player2);
            queue.addPlayer(player3);
            queue.addPlayer(player4); // triggers callback, cancels timeout

            verify(callback, times(1)).onQueueReady(anyString(), anyList(), eq(false));
            verify(mockTask).cancel();
        }
    }

    @Nested
    @DisplayName("Queue state")
    class QueueState {

        @Test
        @DisplayName("reset should clear all state")
        void resetShouldClear() {
            queue.addPlayer(player1);
            queue.addPlayer(player2);
            queue.reset();

            assertTrue(queue.isEmpty());
            assertFalse(queue.isStarted());

            // Should be able to add players again after reset
            assertTrue(queue.addPlayer(player1));
        }

        @Test
        @DisplayName("getQueueSize should return correct count")
        void getQueueSize() {
            assertEquals(0, queue.getQueueSize());
            queue.addPlayer(player1);
            assertEquals(1, queue.getQueueSize());
            queue.addPlayer(player2);
            assertEquals(2, queue.getQueueSize());
        }

        @Test
        @DisplayName("getPlayerQueue should return arenaId for queued player")
        void getPlayerQueue() {
            queue.addPlayer(player1);
            assertEquals("test_arena", queue.getPlayerQueue(player1.getUniqueId()));
            assertNull(queue.getPlayerQueue(UUID.randomUUID()));
        }
    }
}
