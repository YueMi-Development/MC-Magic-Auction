package org.yuemi.magicauction.plugin.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.libs.api.YueMiLibsProvider;
import org.yuemi.libs.api.economy.EconomyApi;
import org.yuemi.libs.api.gui.ClosePolicy;
import org.yuemi.libs.api.gui.Gui;
import org.yuemi.libs.api.gui.GuiApi;
import org.yuemi.libs.api.gui.GuiItem;
import org.yuemi.magicauction.plugin.config.ArenaConfig;
import org.yuemi.magicauction.plugin.config.ItemConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public final class AuctionSession {

    private final AuctionManager manager;
    private final ArenaConfig arena;
    private final List<Player> players;
    private final long seed;
    private final Random random;

    private int currentRound = 1;
    private double currentBasePrice;
    private int currentRevealStep = -1;
    private int currentGraphProgress = 0;
    private Gui graphGui;
    private Gui revealGui;
    
    // Bids for the current round
    private final Map<UUID, Double> currentBids = new HashMap<>();
    
    private BukkitTask activeTask;
    private final List<ItemStack> generatedPrizes = new ArrayList<>();
    private final Map<ItemStack, int[]> prizePositions = new HashMap<>(); // [y, x] in 3x6 grid
    private final Map<ItemStack, ItemConfig> prizeConfigs = new HashMap<>(); // Maps custom items to configs

    public AuctionSession(
            @NotNull AuctionManager manager,
            @NotNull ArenaConfig arena,
            @NotNull List<Player> players,
            long seed
    ) {
        this.manager = manager;
        this.arena = arena;
        this.players = new ArrayList<>(players);
        this.seed = seed;
        this.random = new Random(seed);
        this.currentBasePrice = arena.getBasePrice();
        
        generatePrizesFromArena();
    }

    private void generatePrizesFromArena() {
        if (arena.getRewards().isEmpty()) {
            manager.getPlugin().getLogger().warning("Arena " + arena.getId() + " has no rewards! Auction will have no prizes.");
            return;
        }

        // Shuffle arena rewards pool deterministically
        List<ArenaConfig.PrizeEntry> pool = new ArrayList<>(arena.getRewards());
        Collections.shuffle(pool, random);

        // Pack items into 3x6 grid
        boolean[][] occupied = new boolean[3][6];
        
        for (ArenaConfig.PrizeEntry entry : pool) {
            ItemStack stack = null;
            ItemConfig itemConfig = null;
            int width = 1;
            int height = 1;

            ItemConfig localConfig = manager.getItemConfig(entry.getItemId());
            if (localConfig != null) {
                itemConfig = localConfig;
                stack = localConfig.createItemStack(entry.getAmount());
                width = localConfig.getWidth();
                height = localConfig.getHeight();
            }

            if (stack == null) continue;

            // Pack item
            boolean placed = false;
            for (int y = 0; y <= 3 - height; y++) {
                for (int x = 0; x <= 6 - width; x++) {
                    // Check overlap
                    boolean overlaps = false;
                    for (int dy = 0; dy < height; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            if (occupied[y + dy][x + dx]) {
                                overlaps = true;
                                break;
                            }
                        }
                        if (overlaps) break;
                    }

                    if (!overlaps) {
                        // Place item
                        for (int dy = 0; dy < height; dy++) {
                            for (int dx = 0; dx < width; dx++) {
                                occupied[y + dy][x + dx] = true;
                            }
                        }
                        generatedPrizes.add(stack);
                        prizePositions.put(stack, new int[]{y, x});
                        if (itemConfig != null) {
                            prizeConfigs.put(stack, itemConfig);
                        }
                        placed = true;
                        break;
                    }
                }
                if (placed) break;
            }
        }
    }

    public void start() {
        broadcast("<green>Auction starting for arena: <yellow>" + arena.getName() + "</yellow> (Seed: " + seed + ")!");
        startPreviewState();
    }

    private void startPreviewState() {
        cancelActiveTask();
        currentBids.clear();

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        broadcast("<gray>------------------------------------");
        broadcast("<green>Round " + currentRound + " Preview Starts!");
        broadcast("<gray>BIN (Buy It Now) Price: <gold>$" + String.format("%.2f", binPrice) + "</gold>");
        broadcast("<gray>------------------------------------");

        Gui gui = buildPreviewGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            gui.open(player);
        }

        activeTask = new BukkitRunnable() {
            int timeLeft = 15;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    gui.setClosePolicy(ClosePolicy.CLOSE);
                    for (Player player : players) {
                        if (manager.isBot(player)) continue;
                        player.closeInventory();
                    }
                    startBiddingState();
                    return;
                }

                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    gui.updateTitle(player, "Round " + currentRound + "/5 | Time: " + timeLeft + "s");
                }

                timeLeft--;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 20L);
    }

    private Gui buildPreviewGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        
        var builder = guiApi.createBuilder()
                .title("Round " + currentRound + "/5 | Time: 15s")
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        // Add prizes layer packed in 3x6 starting at row 1, col 1
        builder.createLayer("prizes", 1, layer -> {
            for (ItemStack stack : generatedPrizes) {
                int[] pos = prizePositions.get(stack);
                int slot = (1 + pos[0]) * 9 + (1 + pos[1]);
                
                GuiItem prizeGuiItem = guiApi.createItemBuilder()
                        .item(stack)
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();
                        
                layer.setItem(slot, prizeGuiItem);
            }
        });

        return builder.build();
    }

    private void startBiddingState() {
        cancelActiveTask();
        broadcast("<green>Thinking time over! Opening Anvil bidding...");

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        for (Player player : players) {
            if (manager.isBot(player)) {
                // Simulate bot bidding after a delay (1-3 seconds)
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        double botBid = simulateBotBid(binPrice);
                        currentBids.put(player.getUniqueId(), botBid);
                        broadcast("<gray>" + player.getName() + " has submitted their bid.");
                        
                        if (currentBids.size() >= players.size()) {
                            startGraphicsState();
                        }
                    }
                }, 20L + (long) (Math.random() * 40L));
            } else {
                openSinglePlayerBidding(player, binPrice);
            }
        }
    }

    private void openSinglePlayerBidding(Player p, double binPrice) {
        if (!players.contains(p) || currentBids.containsKey(p.getUniqueId())) return;
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<yellow>Enter your bid amount"));
            paper.setItemMeta(meta);
        }

        guiApi.createAnvilInputBuilder()
                .title("Bid (Base: " + (int) currentBasePrice + ", BIN: " + (int) binPrice + ")")
                .initialText("0")
                .leftItem(paper)
                .closePolicy(ClosePolicy.REOPEN)
                .onSubmit((player, input) -> {
                    double bid;
                    try {
                        bid = Double.parseDouble(input.trim());
                    } catch (NumberFormatException e) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid bid! Must be an integer."));
                        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                        return;
                    }

                    if (bid < 0) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Bid cannot be negative! Use 0 to pass."));
                        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                        return;
                    }

                    EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
                    var provider = econ.getActiveProvider();
                    if (provider != null) {
                        double balance = provider.getBalance(player);
                        if (balance < bid) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot afford this bid! Balance: $" + String.format("%.2f", balance)));
                            Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                            return;
                        }
                    }

                    currentBids.put(player.getUniqueId(), bid);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Bid of <yellow>$" + bid + "</yellow> registered."));

                    if (currentBids.size() >= players.size()) {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), this::startGraphicsState);
                    } else {
                        player.closeInventory();
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Waiting for other players to bid..."));
                    }
                })
                .onClose(player -> {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice));
                    }
                })
                .open(p);
    }

    private void startGraphicsState() {
        cancelActiveTask();
        broadcast("<green>All bids collected! Simulating bid graphics...");

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        currentGraphProgress = 0;
        this.graphGui = buildGraphGui(binPrice);
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            this.graphGui.open(player);
        }

        activeTask = new BukkitRunnable() {
            int progress = 0; // 0 to 7 (Columns 3-9)

            @Override
            public void run() {
                if (progress > 7) {
                    cancel();
                    closeGraphGui();
                    activeTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            evaluateRound();
                        }
                    }.runTaskLater(manager.getPlugin(), 60L);
                    return;
                }

                currentGraphProgress = progress;
                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    graphGui.update(player);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f + (progress * 0.1f));
                }

                progress++;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 5L);
    }

    private Gui buildGraphGui(double binPrice) {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        var mm = MiniMessage.miniMessage();

        var builder = guiApi.createBuilder()
                .title("Bidding Progress (Round " + currentRound + ")")
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        GuiItem borderBlack = guiApi.createItemBuilder()
                .item(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                .build();

        GuiItem borderGray = guiApi.createItemBuilder()
                .item(new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
                .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                .build();

        builder.createLayer("borders", 0, layer -> {
            for (int i = 0; i < 9; i++) {
                layer.setItem(i, borderBlack);
                layer.setItem(45 + i, borderBlack);
            }
            for (int r = 1; r <= 4; r++) {
                layer.setItem(r * 9 + 1, borderGray);
            }
        });

        builder.createLayer("players", 1, layer -> {
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                double bid = currentBids.getOrDefault(p.getUniqueId(), 0.0);
                
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(p);
                    skullMeta.displayName(mm.deserialize("<yellow>" + p.getName()));
                    skullMeta.lore(List.of(
                            mm.deserialize("<gray>Bid: <gold>$" + (int) bid),
                            mm.deserialize(bid >= binPrice ? "<green>BIN SUCCESS!" : "<red>No BIN")
                    ));
                    skull.setItemMeta(skullMeta);
                }

                GuiItem skullGuiItem = guiApi.createItemBuilder()
                        .item(skull)
                        .onClick((pl, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();

                int row = i + 1;
                layer.setItem(row * 9, skullGuiItem);

                double fraction = bid / binPrice;
                for (int colIndex = 2; colIndex <= 8; colIndex++) {
                    int step = colIndex - 1;
                    
                    Material progressMaterial;
                    if (bid >= binPrice) {
                        progressMaterial = Material.LIME_STAINED_GLASS_PANE;
                    } else if (bid > 0 && fraction * 7 >= step) {
                        progressMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                    } else if (bid > 0) {
                        progressMaterial = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
                    } else {
                        progressMaterial = Material.RED_STAINED_GLASS_PANE;
                    }

                    ItemStack pane = new ItemStack(progressMaterial);
                    ItemMeta paneMeta = pane.getItemMeta();
                    if (paneMeta != null) {
                        paneMeta.displayName(mm.deserialize("<gray>Progress"));
                        pane.setItemMeta(paneMeta);
                    }

                    final int stepVal = step;
                    GuiItem paneGuiItem = guiApi.createItemBuilder()
                            .item(pane)
                            .condition(player -> stepVal <= currentGraphProgress)
                            .onClick((pl, ctx) -> ctx.getEvent().setCancelled(true))
                            .build();

                    layer.setItem(row * 9 + colIndex, paneGuiItem);
                }
            }
        });

        return builder.build();
    }

    private void evaluateRound() {
        cancelActiveTask();

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        Player winner = null;
        double highestBid = -1;

        for (Player p : players) {
            double bid = currentBids.getOrDefault(p.getUniqueId(), 0.0);
            if (bid >= binPrice && bid > highestBid) {
                highestBid = bid;
                winner = p;
            }
        }

        if (winner != null) {
            EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
            var provider = econ.getActiveProvider();
            if (provider != null && !manager.isBot(winner)) {
                provider.withdraw(winner, highestBid);
            }

            broadcast("<gold><bold>WINNER!</bold> <yellow>" + winner.getName() + "</yellow> won the auction with a bid of <gold>$" + highestBid + "</gold>!");
            startWinnerRevealAnimation(winner);
        } else {
            broadcast("<red>No players matched the required BIN price of <gold>$" + binPrice + "</gold> in round " + currentRound + ".");
            if (currentRound < 5) {
                currentRound++;
                startPreviewState();
            } else {
                broadcast("<red><bold>AUCTION OVER!</bold> No winner. Items have been locked away.");
                endSession();
            }
        }
    }

    private void startWinnerRevealAnimation(@NotNull Player winner) {
        var mm = MiniMessage.miniMessage();

        Gui revealGui = buildRevealGui();
        this.revealGui = revealGui;
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            revealGui.open(player);
        }

        activeTask = new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= generatedPrizes.size()) {
                    cancel();
                    
                    // Award prizes
                    EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
                    var provider = econ.getActiveProvider();
                    
                    boolean isBotWinner = manager.isBot(winner);
                    for (ItemStack stack : generatedPrizes) {
                        ItemConfig config = prizeConfigs.get(stack);
                        if (config != null) {
                            if (config.isVirtualItem()) {
                                // Virtual Item: Award worth to economy
                                double totalWorth = config.getWorth() * stack.getAmount();
                                if (provider != null && !isBotWinner) {
                                    provider.deposit(winner, totalWorth);
                                    winner.sendMessage(mm.deserialize("<green>Awarded <gold>$" + totalWorth + "</gold> for virtual item: <yellow>" + config.getDisplayName() + "</yellow> (Worth: $" + config.getWorth() + " each)"));
                                }
                            } else {
                                // Non-virtual item: Award its config rewards list only!
                                for (ItemConfig.RewardEntry reward : config.getRewards()) {
                                    if ("command".equalsIgnoreCase(reward.getType()) && reward.getValue() != null) {
                                        String processedCmd = reward.getValue().replace("%player%", winner.getName());
                                        int runCount = stack.getAmount() * reward.getAmount();
                                        for (int a = 0; a < runCount; a++) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
                                        }
                                    } else if ("item".equalsIgnoreCase(reward.getType()) && reward.getItemId() != null && !isBotWinner) {
                                        int totalAmount = reward.getAmount() * stack.getAmount();
                                        ItemStack rewardStack = null;
                                        
                                        // Resolve nested item from local items first
                                        ItemConfig rewardLocalConfig = manager.getItemConfig(reward.getItemId());
                                        if (rewardLocalConfig != null) {
                                            rewardStack = rewardLocalConfig.createItemStack(totalAmount);
                                        } else {
                                            var libs = YueMiLibsProvider.getApi();
                                            if (libs != null) {
                                                try {
                                                    ItemStack resolved = libs.getItems().getItem(reward.getItemId(), totalAmount);
                                                    if (resolved != null) {
                                                        rewardStack = resolved.clone();
                                                    }
                                                } catch (Exception ignored) {}
                                            }
                                        }
                                        
                                        if (rewardStack != null) {
                                            Map<Integer, ItemStack> leftover = winner.getInventory().addItem(rewardStack);
                                            for (ItemStack leftoverItem : leftover.values()) {
                                                winner.getWorld().dropItemNaturally(winner.getLocation(), leftoverItem);
                                            }
                                        }
                                    }
                                }
                                if (!isBotWinner) {
                                    winner.sendMessage(mm.deserialize("<green>Received rewards for: <yellow>" + config.getDisplayName()));
                                }
                            }
                        } else if (!isBotWinner) {
                            // Vanilla physical item fallback
                            Map<Integer, ItemStack> leftover = winner.getInventory().addItem(stack);
                            for (ItemStack leftoverItem : leftover.values()) {
                                winner.getWorld().dropItemNaturally(winner.getLocation(), leftoverItem);
                            }
                        }
                    }
                    
                    activeTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            closeRevealGui();
                            endSession();
                        }
                    }.runTaskLater(manager.getPlugin(), 40L);
                    return;
                }

                currentRevealStep = step;
                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    revealGui.update(player);
                    revealGui.updateTitle(player, "Auction Reveal (" + (step + 1) + "/" + generatedPrizes.size() + ")");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }

                step++;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 15L);
    }

    private Gui buildRevealGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();

        var builder = guiApi.createBuilder()
                .title("Auction Reveal")
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        builder.createLayer("reveal", 1, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                ItemStack stack = generatedPrizes.get(i);
                int[] pos = prizePositions.get(stack);
                int slot = (1 + pos[0]) * 9 + (1 + pos[1]);

                if (i <= currentRevealStep) {
                    GuiItem prizeItem = guiApi.createItemBuilder()
                            .item(stack)
                            .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                            .build();
                    layer.setItem(slot, prizeItem);
                }
            }
        });

        return builder.build();
    }

    private double getMultiplier() {
        if (currentRound - 1 < arena.getMultipliers().size()) {
            return arena.getMultipliers().get(currentRound - 1);
        }
        return 1.0;
    }

    private void broadcast(String message) {
        var mm = MiniMessage.miniMessage();
        for (Player player : players) {
            player.sendMessage(mm.deserialize(message));
        }
    }

    private void cancelActiveTask() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    private void closeGraphGui() {
        if (graphGui != null) {
            graphGui.setClosePolicy(ClosePolicy.CLOSE);
            for (Player player : players) {
                if (manager.isBot(player)) continue;
                player.closeInventory();
            }
            graphGui = null;
        }
    }

    private void closeRevealGui() {
        if (revealGui != null) {
            revealGui.setClosePolicy(ClosePolicy.CLOSE);
            for (Player player : players) {
                if (manager.isBot(player)) continue;
                player.closeInventory();
            }
            revealGui = null;
        }
    }

    private double simulateBotBid(double binPrice) {
        double roll = random.nextDouble();
        if (roll < 0.10) {
            return binPrice;
        } else if (roll < 0.85) {
            double minBid = currentBasePrice;
            double maxBid = binPrice - 1.0;
            if (maxBid <= minBid) {
                return minBid;
            }
            return Math.floor(minBid + random.nextDouble() * (maxBid - minBid));
        } else {
            return 0.0;
        }
    }

    private void endSession() {
        cancelActiveTask();
        closeGraphGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            player.closeInventory();
        }
        manager.sessionEnded(this);
    }
}
