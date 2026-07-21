package org.yuemi.magicauction.plugin.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.magicauction.bot.BotHandler;
import org.yuemi.magicauction.bot.BotProviderImpl;
import org.yuemi.magicauction.matchmaking.MatchmakingService;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.config.ItemConfig;
import org.yuemi.magicauction.plugin.config.RarityRegistry;
import org.yuemi.magicauction.plugin.config.TypeRegistry;
import org.yuemi.magicauction.plugin.config.EventRegistry;
import org.yuemi.magicauction.seed.RandomSeedGenerator;
import org.yuemi.magicauction.seed.SeedGenerator;

import java.io.File;
import java.util.*;

public final class AuctionManager {

    private final JavaPlugin plugin;
    private final Map<String, ItemConfig> items = new HashMap<>();
    private final Map<String, ArenaConfig> arenas = new HashMap<>();
    private final List<AuctionSession> activeSessions = new ArrayList<>();
    private final SeedGenerator seedGenerator = new RandomSeedGenerator();
    private BotHandler botHandler;
    private MatchmakingService matchmakingService;

    public AuctionManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setBotHandler(@Nullable BotHandler handler) {
        this.botHandler = handler;
    }

    @Nullable
    public BotHandler getBotHandler() {
        return botHandler;
    }

    public boolean isBot(@NotNull Player player) {
        return botHandler != null && botHandler.isBot(player);
    }

    public void setMatchmakingService(@Nullable MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Nullable
    public MatchmakingService getMatchmakingService() {
        return matchmakingService;
    }

    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void initialize() {
        this.botHandler = new BotProviderImpl();
        createDirectoriesAndDefaults();
        reload();

        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                Player player = event.getPlayer();
                // Remove from matchmaking queue if queued
                if (matchmakingService != null) {
                    matchmakingService.leaveQueue(player, null);
                }
                // Handle active auction disconnect
                for (AuctionSession session : new ArrayList<>(activeSessions)) {
                    if (session.getPlayers().contains(player)) {
                        session.handlePlayerDisconnect(player);
                    }
                }
            }

            @org.bukkit.event.EventHandler
            public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
                Player player = event.getPlayer();
                // Handle active auction death — skip player and close GUI
                for (AuctionSession session : new ArrayList<>(activeSessions)) {
                    if (session.getPlayers().contains(player)) {
                        session.handlePlayerDeath(player);
                    }
                }
                // Matchmaking queue is unaffected by death — player stays queued
            }
        }, plugin);
    }

    private void createDirectoriesAndDefaults() {
        File dataFolder = plugin.getDataFolder();
        java.net.URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (jarUrl == null) return;

        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(jarUrl.openStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && (name.startsWith("items/") || name.startsWith("auction/") || name.startsWith("rarities/") || name.startsWith("types/") || name.startsWith("events/")) && name.endsWith(".yml")) {
                    File outFile = new File(dataFolder, name);
                    if (!outFile.exists()) {
                        outFile.getParentFile().mkdirs();
                        try {
                            plugin.saveResource(name, false);
                        } catch (IllegalArgumentException ignored) {
                            // Suppress exception if resource doesn't exist or already exists
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to auto-discover default resources from JAR: " + e.getMessage());
        }
    }

    public void reload() {
        File dataFolder = plugin.getDataFolder();
        
        // Load rarities, types, and events first
        try {
            RarityRegistry.load(new File(dataFolder, "rarities"));
            TypeRegistry.load(new File(dataFolder, "types"));
            EventRegistry.load(new File(dataFolder, "events"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load registry: " + e.getMessage());
        }

        items.clear();
        arenas.clear();

        File itemsFolder = new File(dataFolder, "items");
        File auctionFolder = new File(dataFolder, "auction");

        // Load items
        File[] itemFiles = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (itemFiles != null) {
            for (File file : itemFiles) {
                try {
                    ItemConfig config = ItemConfig.load(file);
                    items.put(config.getId().toLowerCase(), config);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load item config: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        // Load arenas
        File[] arenaFiles = auctionFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (arenaFiles != null) {
            for (File file : arenaFiles) {
                try {
                    ArenaConfig config = ArenaConfig.load(file);
                    arenas.put(config.getId().toLowerCase(), config);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load arena config: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("Loaded " + items.size() + " items and " + arenas.size() + " arenas.");
    }

    @Nullable
    public ItemConfig getItemConfig(@NotNull String id) {
        return items.get(id.toLowerCase());
    }

    @Nullable
    public ArenaConfig getArenaConfig(@NotNull String id) {
        return arenas.get(id.toLowerCase());
    }

    @NotNull
    public Collection<ArenaConfig> getArenas() {
        return arenas.values();
    }

    public boolean isPlayerInSession(@NotNull Player player) {
        for (AuctionSession session : activeSessions) {
            if (session.getPlayers().contains(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether the matchmaking system is enabled globally in config.yml
     */
    public boolean isMatchmakingGloballyEnabled() {
        return plugin.getConfig().getBoolean("matchmaking.enabled", true);
    }

    /**
     * @return true if the timeout action is set to {@code "fill_bots"}, false
     *         if it is {@code "cancel"} or any unrecognised value
     */
    public boolean isMatchmakingTimeoutFillBots() {
        String action = plugin.getConfig().getString("matchmaking.on-timeout", "fill_bots");
        return "fill_bots".equalsIgnoreCase(action);
    }

    /** @return the global minimum real players for matchmaking (from config.yml) */
    public int getMatchmakingMinPlayers() {
        return Math.max(2, plugin.getConfig().getInt("matchmaking.min-players", 4));
    }

    /** @return the global matchmaking timeout in seconds (from config.yml) */
    public int getMatchmakingTimeoutSeconds() {
        return Math.max(10, plugin.getConfig().getInt("matchmaking.timeout-seconds", 120));
    }

    /** @return whether bots are allowed to fill matchmaking queues (from config.yml) */
    public boolean isMatchmakingAllowBots() {
        return plugin.getConfig().getBoolean("matchmaking.allow-bots", true);
    }

    /**
     * Callback invoked by {@link org.yuemi.magicauction.matchmaking.MatchmakingService} when a
     * matchmaking queue is ready to start an auction.
     *
     * @param arenaId     the arena to start
     * @param realPlayers the real players from the queue
     * @param timedOut    true if the timeout triggered (bots may be needed)
     */
    public void onMatchmakingQueueReady(@NotNull String arenaId, @NotNull List<Player> realPlayers, boolean timedOut) {
        ArenaConfig arena = getArenaConfig(arenaId);
        if (arena == null) {
            plugin.getLogger().warning("Matchmaking queue ready for unknown arena: " + arenaId);
            return;
        }

        int needed = getMatchmakingMinPlayers();
        List<Player> players = new ArrayList<>(realPlayers);

        // On timeout, respect the configured action
        if (timedOut && !isMatchmakingTimeoutFillBots()) {
            // "cancel" action — notify players and abort
            var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
            for (Player p : realPlayers) {
                p.sendMessage(mm.deserialize(
                        "<red>Matchmaking for arena <yellow>" + arena.getName()
                        + "</yellow> timed out without enough players."));
            }
            return;
        }

        // Fill remaining slots with bots if allowed
        if (players.size() < needed && isMatchmakingAllowBots()) {
            for (int i = players.size(); i < needed; i++) {
                if (botHandler != null) {
                    players.add(botHandler.createBot("MatchmakingBot_" + (i + 1)));
                }
            }
        }

        // If still not enough players, abort
        if (players.size() < needed) {
            var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
            for (Player p : realPlayers) {
                p.sendMessage(mm.deserialize(
                        "<red>Matchmaking for arena <yellow>" + arena.getName()
                        + "</yellow> cancelled: not enough players and bots are disabled."));
            }
            return;
        }

        // Generate a random seed
        long seed = seedGenerator.resolve(0);
        startSession(arena, seed, players);
    }

    public void startSession(@NotNull ArenaConfig arena, long seed, @NotNull List<Player> players) {
        AuctionSession session = new AuctionSession(this, arena, players, seed);
        activeSessions.add(session);
        session.start();
    }

    public void sessionEnded(@NotNull AuctionSession session) {
        activeSessions.remove(session);
    }
}
