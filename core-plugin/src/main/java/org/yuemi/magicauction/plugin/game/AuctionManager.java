package org.yuemi.magicauction.plugin.game;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.config.ItemConfig;

import java.io.File;
import java.util.*;

public final class AuctionManager {

    private final JavaPlugin plugin;
    private final Map<String, ItemConfig> items = new HashMap<>();
    private final Map<String, ArenaConfig> arenas = new HashMap<>();
    private final List<AuctionSession> activeSessions = new ArrayList<>();

    public AuctionManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void initialize() {
        createDirectoriesAndDefaults();
        reload();
    }

    private void createDirectoriesAndDefaults() {
        File dataFolder = plugin.getDataFolder();
        java.net.URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (jarUrl == null) return;

        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(jarUrl.openStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && (name.startsWith("items/") || name.startsWith("auction/")) && name.endsWith(".yml")) {
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
        items.clear();
        arenas.clear();

        File dataFolder = plugin.getDataFolder();
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
        // AuctionSession doesn't expose players list currently, but we could check
        // Or simply check activeSessions
        return false; // In a single-session environment or Command validations, we could enhance this.
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
