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
import org.yuemi.magicauction.plugin.config.EventConfig;
import org.yuemi.magicauction.plugin.config.EventRegistry;
import org.yuemi.magicauction.plugin.config.GlassPaneMapper;
import org.yuemi.magicauction.plugin.config.RarityColorMapper;
import org.yuemi.magicauction.plugin.config.RarityRegistry;
import org.yuemi.magicauction.plugin.config.TypeRegistry;
import org.yuemi.magicauction.matchs.AuctionMatchEvaluator;
import org.yuemi.magicauction.matchs.model.Bid;
import org.yuemi.magicauction.matchs.model.RoundContext;
import org.yuemi.magicauction.matchs.model.RoundResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public final class AuctionSession {

    private final AuctionManager manager;
    private final ArenaConfig arena;
    private final List<Player> players;
    private final long seed;
    private final Random random;
    private final Random botRandom;

    private int currentRound = 1;
    private double currentBasePrice;
    private int revealAnimationTick = -1;
    private int currentGraphProgress = 0;
    private boolean biddingActive = false;
    private boolean revealActive = false;
    private Gui graphGui;
    private Gui revealGui;
    
    // Bids for the current round
    private final Map<UUID, Double> currentBids = new HashMap<>();
    private final List<UUID> bidOrder = new ArrayList<>();
    
    private BukkitTask activeTask;
    private BukkitTask bidCountdownTask;
    private BukkitTask previewCountdownTask;
    private Gui previewGui;
    private final List<ItemStack> generatedPrizes = new ArrayList<>();
    private final List<PrizeState> prizeStates = new ArrayList<>();
    private final List<String> shuffledEvents = new ArrayList<>();
    private final List<String> shuffledStartEvents = new ArrayList<>();
    private final Set<String> triggeredEvents = new HashSet<>();

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
        this.botRandom = new Random(seed + 9999L);
        this.currentBasePrice = arena.getBasePrice();
        
        this.shuffledEvents.addAll(arena.getEvents());
        Collections.shuffle(this.shuffledEvents, this.random);

        this.shuffledStartEvents.addAll(arena.getEvents());
        Collections.shuffle(this.shuffledStartEvents, new Random(seed));
        
        generatePrizesFromArena();
    }

    private static final int GRID_ROWS = 6;
    private static final int GRID_COLS = 9;

    private void generatePrizesFromArena() {
        if (arena.getRewards().isEmpty()) {
            manager.getPlugin().getLogger().warning("Arena " + arena.getId() + " has no rewards! Auction will have no prizes.");
            return;
        }

        // Split reward quantities to individual entries of amount 1 (dont stack same items)
        List<ArenaConfig.PrizeEntry> pool = new ArrayList<>();
        for (ArenaConfig.PrizeEntry entry : arena.getRewards()) {
            for (int i = 0; i < entry.getAmount(); i++) {
                pool.add(new ArenaConfig.PrizeEntry(entry.getItemId(), 1));
            }
        }
        Collections.shuffle(pool, random);

        // Limit selected items count within min-items and max-items
        int min = arena.getMinItems();
        int max = arena.getMaxItems();
        int targetCount;
        if (min == max) {
            targetCount = min;
        } else {
            targetCount = min + random.nextInt(max - min + 1);
        }

        // --- Phase 1: Resolve ItemConfigs (validate size, warn on missing) ---
        List<ItemConfig> resolvedConfigs = new ArrayList<>();
        for (ArenaConfig.PrizeEntry entry : pool) {
            ItemConfig config = manager.getItemConfig(entry.getItemId());
            if (config == null) {
                manager.getPlugin().getLogger().warning(
                    "Arena " + arena.getId() + ": item '" + entry.getItemId()
                    + "' has no local config — skipping. Add a config in items/ or ensure it is registered."
                );
                continue;
            }
            int w = Math.min(config.getWidth(), GRID_COLS);
            int h = Math.min(config.getHeight(), GRID_ROWS);
            if (w != config.getWidth() || h != config.getHeight()) {
                manager.getPlugin().getLogger().warning(
                    "Arena " + arena.getId() + ": item '" + entry.getItemId()
                    + "' size " + config.getWidth() + "x" + config.getHeight()
                    + " exceeds " + GRID_ROWS + "x" + GRID_COLS + " grid — clamped to " + w + "x" + h + "."
                );
            }
            resolvedConfigs.add(config);
        }

        targetCount = Math.min(targetCount, resolvedConfigs.size());

        // --- Phase 2: Two-pass grid placement ---
        boolean[] configPlaced = new boolean[resolvedConfigs.size()];
        int[][] configPositions = new int[resolvedConfigs.size()][];
        boolean[][] occupied = new boolean[GRID_ROWS][GRID_COLS];
        int placedCount = 0;

        // Pass 1: ordered linear scan
        for (int i = 0; i < resolvedConfigs.size() && placedCount < targetCount; i++) {
            ItemConfig config = resolvedConfigs.get(i);
            int pw = Math.min(config.getWidth(), GRID_COLS);
            int ph = Math.min(config.getHeight(), GRID_ROWS);
            int[] pos = tryFindPosition(pw, ph, occupied);
            if (pos != null) {
                markOccupied(occupied, pos[0], pos[1], pw, ph);
                configPlaced[i] = true;
                configPositions[i] = pos;
                placedCount++;
            }
        }

        // Pass 2: full sweep for any still-unplaced items
        if (placedCount < targetCount) {
            for (int i = 0; i < resolvedConfigs.size() && placedCount < targetCount; i++) {
                if (configPlaced[i]) continue;
                ItemConfig config = resolvedConfigs.get(i);
                int pw = Math.min(config.getWidth(), GRID_COLS);
                int ph = Math.min(config.getHeight(), GRID_ROWS);
                int[] pos = tryFindPosition(pw, ph, occupied);
                if (pos != null) {
                    markOccupied(occupied, pos[0], pos[1], pw, ph);
                    configPlaced[i] = true;
                    configPositions[i] = pos;
                    placedCount++;
                }
            }
        }

        // Warning on shortfall
        if (placedCount < targetCount) {
            manager.getPlugin().getLogger().warning(
                "Arena " + arena.getId() + ": only placed " + placedCount + "/" + targetCount
                + " prizes — grid full or pool exhausted."
            );
        }

        // --- Phase 3: Create ItemStacks and PrizeStates for placed items ---
        for (int i = 0; i < resolvedConfigs.size(); i++) {
            if (!configPlaced[i]) continue;
            ItemConfig config = resolvedConfigs.get(i);
            int[] pos = configPositions[i];

            ItemStack stack = config.createItemStack(1);
            generatedPrizes.add(stack);

            PrizeState pState = new PrizeState(stack, config, pos);
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(manager.getPlugin(), "auction_item_uid");
                meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, pState.getUniqueId().toString());
                stack.setItemMeta(meta);
            }
            prizeStates.add(pState);
        }
    }

    @Nullable
    private int[] tryFindPosition(int width, int height, boolean[][] occupied) {
        for (int y = 0; y <= GRID_ROWS - height; y++) {
            for (int x = 0; x <= GRID_COLS - width; x++) {
                if (!hasOverlap(occupied, y, x, width, height)) {
                    return new int[]{y, x};
                }
            }
        }
        return null;
    }

    private boolean hasOverlap(boolean[][] occupied, int row, int col, int width, int height) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                if (occupied[row + dy][col + dx]) return true;
            }
        }
        return false;
    }

    private void markOccupied(boolean[][] occupied, int row, int col, int width, int height) {
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                occupied[row + dy][col + dx] = true;
            }
        }
    }

    public void start() {
        broadcast("<green>Auction started in <yellow>" + arena.getName() + "</yellow>!");
        
        int startEventsCount = arena.getStartEvents();
        if (startEventsCount > 0) {
            executeStartEvents(startEventsCount);
        }
        
        startPreviewState();
    }

    private void executeStartEvents(int count) {
        int triggered = 0;
        // First try configured events
        for (String eventId : shuffledStartEvents) {
            if (triggered >= count) break;

            EventConfig event = EventRegistry.get(eventId);
            if (event != null) {
                boolean meetsMinRound = 1 >= event.getMinRounds();
                boolean meetsOnlyOnce = !event.isOnlyOnce() || !triggeredEvents.contains(event.getId().toLowerCase());

                if (meetsMinRound && meetsOnlyOnce) {
                    triggeredEvents.add(event.getId().toLowerCase());
                    executeEvent(event);
                    triggered++;
                }
            }
        }

        // Fallback to any registered event in the EventRegistry that meets the minRound condition
        if (triggered < count) {
            List<EventConfig> allEvents = new ArrayList<>(EventRegistry.getEvents().values());
            Collections.shuffle(allEvents, new Random(seed + 12345L));
            for (EventConfig event : allEvents) {
                if (triggered >= count) break;

                boolean meetsMinRound = 1 >= event.getMinRounds();
                boolean meetsOnlyOnce = !event.isOnlyOnce() || !triggeredEvents.contains(event.getId().toLowerCase());
                if (meetsMinRound && meetsOnlyOnce) {
                    triggeredEvents.add(event.getId().toLowerCase());
                    executeEvent(event);
                    triggered++;
                }
            }
        }

        // Last resort fallback (ignore onlyOnce)
        if (triggered < count) {
            List<EventConfig> allEvents = new ArrayList<>(EventRegistry.getEvents().values());
            Collections.shuffle(allEvents, new Random(seed + 12345L));
            for (EventConfig event : allEvents) {
                if (triggered >= count) break;

                if (1 >= event.getMinRounds()) {
                    triggeredEvents.add(event.getId().toLowerCase());
                    executeEvent(event);
                    triggered++;
                }
            }
        }
    }

    private EventConfig getRoundEvent() {
        for (int i = currentRound - 1; i < shuffledEvents.size(); i++) {
            String eventId = shuffledEvents.get(i);
            EventConfig event = EventRegistry.get(eventId);
            if (event != null) {
                boolean meetsOnlyOnce = !event.isOnlyOnce() || !triggeredEvents.contains(event.getId().toLowerCase());
                boolean meetsMinRound = currentRound >= event.getMinRounds();
                if (meetsOnlyOnce && meetsMinRound) {
                    if (i != currentRound - 1) {
                        String temp = shuffledEvents.get(currentRound - 1);
                        shuffledEvents.set(currentRound - 1, eventId);
                        shuffledEvents.set(i, temp);
                    }
                    return event;
                }
            }
        }
        // Fallback to round_<N>
        EventConfig fallback = EventRegistry.get("round_" + currentRound);
        if (fallback != null) {
            boolean meetsOnlyOnce = !fallback.isOnlyOnce() || !triggeredEvents.contains(fallback.getId().toLowerCase());
            boolean meetsMinRound = currentRound >= fallback.getMinRounds();
            if (meetsOnlyOnce && meetsMinRound) {
                return fallback;
            }
        }
        
        // Fallback to any registered event that meets minRound and onlyOnce
        List<EventConfig> allEvents = new ArrayList<>(EventRegistry.getEvents().values());
        Collections.shuffle(allEvents, this.random);
        for (EventConfig event : allEvents) {
            boolean meetsOnlyOnce = !event.isOnlyOnce() || !triggeredEvents.contains(event.getId().toLowerCase());
            boolean meetsMinRound = currentRound >= event.getMinRounds();
            if (meetsOnlyOnce && meetsMinRound) {
                return event;
            }
        }
        
        // Last resort fallback: relax only-once constraint
        for (EventConfig event : allEvents) {
            if (currentRound >= event.getMinRounds()) {
                return event;
            }
        }
        return null;
    }

    private void executeEvent(@NotNull EventConfig event) {
        broadcast("<light_purple>Event: <yellow>" + event.getId().replace("_", " "));
        for (EventConfig.ActionEntry action : event.getActions()) {
            executeAction(action);
        }
    }

    private void executeAction(@NotNull EventConfig.ActionEntry action) {
        List<PrizeState> candidates = new ArrayList<>();
        for (PrizeState state : prizeStates) {
            if (state.isFullyRevealed()) continue;

            boolean alreadyRevealed = false;
            switch (action.getType()) {
                case "type" -> alreadyRevealed = state.isTypeRevealed();
                case "rarity" -> alreadyRevealed = state.isRarityRevealed();
                case "size" -> alreadyRevealed = state.isSizeRevealed();
                case "full" -> alreadyRevealed = state.isFullyRevealed();
            }
            if (alreadyRevealed) continue;

            // Rarity condition
            if (action.getRarity() != null && !action.getRarity().equalsIgnoreCase(state.getConfig().getRarity())) {
                continue;
            }

            // Size condition
            if (action.getMinTotalSize() > 0) {
                int totalSize = state.getConfig().getWidth() * state.getConfig().getHeight();
                if (totalSize < action.getMinTotalSize()) {
                    continue;
                }
            }

            candidates.add(state);
        }

        List<PrizeState> selected = new ArrayList<>();
        if ("random".equalsIgnoreCase(action.getSelection())) {
            int toSelect = Math.min(action.getCount(), candidates.size());
            for (int i = 0; i < toSelect; i++) {
                int idx = random.nextInt(candidates.size());
                selected.add(candidates.remove(idx));
            }
        } else {
            selected.addAll(candidates);
        }

        for (PrizeState state : selected) {
            switch (action.getType()) {
                case "type" -> state.setTypeRevealed(true);
                case "rarity" -> state.setRarityRevealed(true);
                case "size" -> state.setSizeRevealed(true);
                case "full" -> state.setFullyRevealed(true);
            }
        }
    }

    private ItemStack buildContainerItemStack(@NotNull PrizeState state, boolean forceFullyRevealed) {
        boolean hide = state.isHide() && !forceFullyRevealed;
        
        boolean fullyRevealed = !hide && (forceFullyRevealed || state.isFullyRevealed());
        boolean rarityRevealed = !hide && (forceFullyRevealed || state.isRarityRevealed());
        boolean typeRevealed = !hide && (forceFullyRevealed || state.isTypeRevealed());
        boolean sizeRevealed = !hide && (forceFullyRevealed || state.isSizeRevealed());

        ItemStack item;
        if (fullyRevealed) {
            item = state.getOriginalStack().clone();
        } else {
            Material material;
            if (sizeRevealed && rarityRevealed) {
                String color = getRarityColor(state.getConfig().getRarity());
                material = GlassPaneMapper.getMaterial(color);
            } else if (rarityRevealed) {
                // Rarity known but size unknown — use a solid glass block (no pane)
                // to convey color without implying the item's full shape
                String color = getRarityColor(state.getConfig().getRarity());
                material = GlassPaneMapper.getBlockMaterial(color);
            } else {
                material = Material.BLACK_STAINED_GLASS_PANE;
            }
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var mm = MiniMessage.miniMessage();
            
            if (fullyRevealed) {
                meta.displayName(mm.deserialize("<italic:false>" + state.getConfig().getDisplayName()));
            } else {
                meta.displayName(mm.deserialize("<italic:false><red>???</red>"));
            }

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            
            // Rarity (if revealed)
            if (rarityRevealed) {
                var rarityInfo = RarityRegistry.get(state.getConfig().getRarity());
                if (rarityInfo != null) {
                    lore.add(mm.deserialize("<italic:false><gray>Rarity: </gray>" + RarityColorMapper.toTag(rarityInfo.getColor()) + rarityInfo.getName() + RarityColorMapper.toCloseTag(rarityInfo.getColor())));
                } else {
                    lore.add(mm.deserialize("<italic:false><gray>Rarity: " + state.getConfig().getRarity() + "</gray>"));
                }
            } else {
                lore.add(mm.deserialize("<italic:false><gray>Rarity: <red>???</red></gray>"));
            }

            // Types (if revealed)
            if (typeRevealed) {
                var typeInfo = TypeRegistry.get(state.getConfig().getType());
                String typeName = typeInfo != null ? typeInfo.getName() : state.getConfig().getType();
                lore.add(mm.deserialize("<italic:false><gray>Type: <yellow>" + typeName + "</yellow></gray>"));
            } else {
                lore.add(mm.deserialize("<italic:false><gray>Type: <red>???</red></gray>"));
            }

            // Desc (if fully revealed)
            if (fullyRevealed && state.getConfig().getDesc() != null) {
                lore.add(mm.deserialize("<italic:false><gray>Desc: <white>" + state.getConfig().getDesc() + "</white></gray>"));
            }

            // Worth (if fully revealed)
            if (fullyRevealed) {
                lore.add(mm.deserialize("<italic:false><gray>Worth: <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(state.getConfig().getWorth()) + "</gold></gray>"));
            }

            // Size (if revealed)
            if (sizeRevealed) {
                lore.add(mm.deserialize("<italic:false><gray>Size: <yellow>" + state.getConfig().getWidth() + "x" + state.getConfig().getHeight() + "</yellow></gray>"));
            } else {
                lore.add(mm.deserialize("<italic:false><gray>Size: <red>???</red></gray>"));
            }

            var key = new org.bukkit.NamespacedKey(manager.getPlugin(), "auction_item_uid");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, state.getUniqueId().toString());
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startPreviewState() {
        cancelActiveTask();
        cancelPreviewCountdownTask();
        currentBids.clear();
        bidOrder.clear();

        // Apply round event
        EventConfig event = getRoundEvent();
        if (event != null) {
            triggeredEvents.add(event.getId().toLowerCase());
            executeEvent(event);
        }

        broadcast("<yellow>" + arena.getName() + " — Round " + currentRound + " started!");

        int thinkingTime = arena.getThinkingTime();
        this.previewGui = buildPreviewGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            previewGui.open(player);
        }

        // Action bar countdown for preview phase
        previewCountdownTask = startCountdown(thinkingTime, secondsLeft -> {
            String color = getTimeColor(secondsLeft, thinkingTime);
            for (Player player : players) {
                if (manager.isBot(player)) continue;
                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                        "<" + color + ">" + secondsLeft + "s left</" + color + ">"));
            }
        });

        activeTask = new BukkitRunnable() {
            int timeLeft = thinkingTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    closePreviewGui();
                    cancelPreviewCountdownTask();
                    startBiddingState();
                    return;
                }

                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    // Time shown via action bar — no GUI title update needed
                }

                timeLeft--;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 20L);
    }

    private Gui buildPreviewGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        
        var builder = guiApi.createBuilder()
                .title(getRoundTitle(""))
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        // Add prizes layer packed in 3x6 starting at row 1, col 1
        builder.createLayer("prizes", 1, layer -> {
            for (PrizeState state : prizeStates) {
                int[] pos = state.getPosition();
                int startY = pos[0];
                int startX = pos[1];

                if (state.isHide()) {
                    // Nothing revealed yet — don't render in preview at all
                    continue;
                }

                if (state.isFullyRevealed()) {
                    int slot = startY * 9 + startX;
                    ItemStack prizeItem = buildContainerItemStack(state, false);
                    GuiItem prizeGuiItem = guiApi.createItemBuilder()
                            .item(prizeItem)
                            .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                            .build();
                    layer.setItem(slot, prizeGuiItem);
                } else {
                    ItemStack glassPane = buildContainerItemStack(state, false);
                    GuiItem paneGuiItem = guiApi.createItemBuilder()
                            .item(glassPane)
                            .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                            .build();
                            
                    if (state.isSizeRevealed()) {
                        int width = state.getConfig().getWidth();
                        int height = state.getConfig().getHeight();
                        for (int dy = 0; dy < height; dy++) {
                            for (int dx = 0; dx < width; dx++) {
                                int slot = (startY + dy) * 9 + (startX + dx);
                                layer.setItem(slot, paneGuiItem);
                            }
                        }
                    } else {
                        // 1x1 at anchor position
                        int slot = startY * 9 + startX;
                        layer.setItem(slot, paneGuiItem);
                    }
                }
            }
        });

        return builder.build();
    }

    private void startBiddingState() {
        cancelActiveTask();
        cancelBidCountdownTask();
        cancelPreviewCountdownTask();
        biddingActive = true;
        // Bidding phase begins — players see the anvil GUI

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;
        int totalSeconds = arena.getBidDuration();

        // Start action bar countdown
        bidCountdownTask = startCountdown(totalSeconds, secondsLeft -> {
            String color = getTimeColor(secondsLeft, totalSeconds);
            for (Player player : players) {
                if (manager.isBot(player)) continue;
                if (currentBids.containsKey(player.getUniqueId())) continue;
                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                        "<" + color + ">" + secondsLeft + "s left</" + color + ">"));
            }
        });

        // Defer anvil open by 1 tick to avoid stacking InventoryChangeTrigger evaluation
        // on the same tick as the preview close (Paper 1.21.8 advancement/tag hang).
        for (Player player : players) {
            if (manager.isBot(player)) {
                // Simulate bot bidding after a delay (1-3 seconds)
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        double botBid = simulateBotBid(binPrice);
                        currentBids.put(player.getUniqueId(), botBid);
                        bidOrder.add(player.getUniqueId());
                        broadcast("<gray>" + player.getName() + " placed a bid.");
                        
                        if (currentBids.size() >= players.size()) {
                            startGraphicsState();
                        }
                    }
                }, 20L + (long) (Math.random() * 40L));
            } else if (!player.isOnline()) {
                currentBids.put(player.getUniqueId(), 1.0);
                bidOrder.add(player.getUniqueId());
                broadcast("<gray>" + player.getName() + " is offline — skipped.");
                if (currentBids.size() >= players.size()) {
                    startGraphicsState();
                }
            } else {
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 1L);
            }
        }

        // Bid timeout: force any player who has not bid to bid $1
        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : players) {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        currentBids.put(player.getUniqueId(), 1.0);
                        bidOrder.add(player.getUniqueId());
                        broadcast("<gray>" + player.getName() + " missed the bid timer.");
                    }
                }
                // No explicit broadcast — players see the graph opening
                if (currentBids.size() >= players.size()) {
                    startGraphicsState();
                }
            }
        }.runTaskLater(manager.getPlugin(), arena.getBidDuration() * 20L);
    }

    private void openSinglePlayerBidding(Player p, double binPrice) {
        if (!players.contains(p) || currentBids.containsKey(p.getUniqueId())) return;

        double playerBalance = 0;
        EconomyApi econApi = YueMiLibsProvider.getApi().getEconomy();
        var econProvider = econApi.getActiveProvider();
        if (econProvider != null) {
            playerBalance = econProvider.getBalance(p);
        }
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<yellow>Enter your bid amount"));
            paper.setItemMeta(meta);
        }

        guiApi.createAnvilInputBuilder()
                .title("$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(playerBalance)
                        + " | " + String.format("%.1f", getMultiplier()) + "x")
                .initialText("0")
                .leftItem(paper)
                .closePolicy(ClosePolicy.CLOSE)
                .onSubmit((player, input) -> {
                    // Guard: ignore if the player already submitted a bid this round
                    if (currentBids.containsKey(player.getUniqueId())) {
                        player.closeInventory();
                        return;
                    }

                    double bid;
                    try {
                        bid = org.yuemi.libs.api.util.NumberUtils.parseSuffix(input);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid amount. Use a number (e.g. 1000, 1.5k)."));
                        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                        return;
                    }

                    if (bid <= 0) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Bid must be greater than 0!"));
                        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                        return;
                    }

                    EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
                    var provider = econ.getActiveProvider();
                    if (provider != null) {
                        double balance = provider.getBalance(player);
                        if (bid > balance) {
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Bid exceeds your balance! Balance: $" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(balance)));
                            Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 3L);
                            return;
                        }
                    }

                    currentBids.put(player.getUniqueId(), bid);
                    bidOrder.add(player.getUniqueId());
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Bid: <yellow>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(bid)));

                    if (currentBids.size() >= players.size()) {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), this::startGraphicsState);
                    } else {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), this::showGraphView);
                    }
                })
                .onClose(player -> {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> openSinglePlayerBidding(player, binPrice), 1L);
                    }
                })
                .open(p);
    }

    private void startGraphicsState() {
        cancelActiveTask();
        cancelBidCountdownTask();
        biddingActive = false;
        closeGraphGui();
        broadcast("<green>Bids are in!");

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        currentGraphProgress = 0;
        this.graphGui = buildGraphGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            this.graphGui.open(player);
        }

        startGraphAnimation();
    }

    private void startGraphAnimation() {
        activeTask = new BukkitRunnable() {
            int progress = 0; // 0 to 7 (Columns 3-9)

            @Override
            public void run() {
                if (progress > 7) {
                    cancel();
                    activeTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            closeGraphGui();
                            evaluateRound();
                        }
                    }.runTaskLater(manager.getPlugin(), 200L);
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

    private void showGraphView() {
        closeRevealGui();
        currentGraphProgress = 0;
        this.graphGui = buildGraphGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            if (currentBids.containsKey(player.getUniqueId())) {
                this.graphGui.open(player);
            }
        }
    }

    private Gui buildGraphGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        var mm = MiniMessage.miniMessage();

        double graphMultiplier = getMultiplier();
        String multColor = getMultiplierColor(graphMultiplier);
        String graphTitle = "Bidding Progress (" + multColor + String.format("%.1f", graphMultiplier) + "x§r)";
        var builder = guiApi.createBuilder()
                .title(graphTitle)
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blackMeta = blackFiller.getItemMeta();
        if (blackMeta != null) {
            blackMeta.displayName(Component.text(" "));
            blackMeta.lore(List.of(Component.text(" ")));
            blackFiller.setItemMeta(blackMeta);
        }
        GuiItem borderBlack = guiApi.createItemBuilder()
                .item(blackFiller)
                .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                .build();

        ItemStack grayFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayFiller.getItemMeta();
        if (grayMeta != null) {
            grayMeta.displayName(Component.text(" "));
            grayMeta.lore(List.of(Component.text(" ")));
            grayFiller.setItemMeta(grayMeta);
        }
        GuiItem borderGray = guiApi.createItemBuilder()
                .item(grayFiller)
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
            // Pre-calculate bids, winner, and second highest for coloring
            double multiplier = getMultiplier();
            double highestBid = -1;
            double secondHighestBid = -1;
            UUID winnerUUID = null;

            for (Player pl : players) {
                double b = currentBids.getOrDefault(pl.getUniqueId(), 0.0);
                if (b > highestBid) {
                    highestBid = b;
                    winnerUUID = pl.getUniqueId();
                }
            }

            for (Player pl : players) {
                if (pl.getUniqueId().equals(winnerUUID)) continue;
                double b = currentBids.getOrDefault(pl.getUniqueId(), 0.0);
                if (b > secondHighestBid) {
                    secondHighestBid = b;
                }
            }

            double binThreshold = Math.max(secondHighestBid, currentBasePrice) * multiplier;
            boolean hasWinner = highestBid >= binThreshold;

            // Dynamic bar sizing: find min and max bids across all players
            double minBid = Double.MAX_VALUE;
            double maxBid = Double.MIN_VALUE;
            for (Player pl : players) {
                double b = currentBids.getOrDefault(pl.getUniqueId(), 0.0);
                if (b > maxBid) maxBid = b;
                if (b < minBid) minBid = b;
            }

            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                double bid = currentBids.getOrDefault(p.getUniqueId(), 0.0);

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(p);
                    skullMeta.displayName(mm.deserialize("<italic:false><yellow>" + p.getName()));
                    skullMeta.lore(List.of(
                            mm.deserialize("<italic:false><gray>Bid: <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(bid))
                    ));
                    skull.setItemMeta(skullMeta);
                }

                GuiItem skullGuiItem = guiApi.createItemBuilder()
                        .item(skull)
                        .onClick((pl, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();

                int row = i + 1;
                layer.setItem(row * 9, skullGuiItem);

                // Normalize bid to continuous bar height [1, 7] — lowest bid = 1, highest bid = 7
                int length;
                if (maxBid <= minBid) {
                    length = 7;
                } else {
                    double normalized = 1.0 + ((bid - minBid) / (maxBid - minBid)) * 6.0;
                    length = (int) Math.round(normalized);
                    length = Math.max(1, Math.min(7, length));
                }

                for (int colIndex = 2; colIndex <= 8; colIndex++) {
                    int step = colIndex - 1;
                    
                    Material progressMaterial;
                    if (p.getUniqueId().equals(winnerUUID)) {
                        if (hasWinner) {
                            progressMaterial = Material.LIME_STAINED_GLASS_PANE;
                        } else {
                            progressMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                        }
                    } else if (bid > 0 && Math.abs(bid - secondHighestBid) < 0.0001) {
                        progressMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                    } else {
                        progressMaterial = Material.RED_STAINED_GLASS_PANE;
                    }

                    ItemStack pane = new ItemStack(progressMaterial);
                    ItemMeta paneMeta = pane.getItemMeta();
                    if (paneMeta != null) {
                        paneMeta.displayName(mm.deserialize("<gray>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(bid)));
                        pane.setItemMeta(paneMeta);
                    }

                    final int stepVal = step;
                    final int finalLength = length;
                    GuiItem paneGuiItem = guiApi.createItemBuilder()
                            .item(pane)
                            .condition(player -> stepVal <= finalLength && stepVal <= currentGraphProgress)
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

        // Build input for the match evaluator
        List<Bid> bidList = players.stream()
                .map(p -> new Bid(p.getUniqueId(), currentBids.getOrDefault(p.getUniqueId(), 0.0)))
                .toList();

        boolean hasMultiplierOne = arena.getMultipliers().stream()
                .anyMatch(m -> Math.abs(m - 1.0) < 0.0001);

        RoundContext context = new RoundContext(
                bidList,
                List.copyOf(bidOrder),
                currentBasePrice,
                multiplier,
                currentRound,
                arena.getMultipliers().size(),
                hasMultiplierOne
        );

        RoundResult result = AuctionMatchEvaluator.evaluateRound(context);

        switch (result.outcome()) {
            case PLAYER_WON -> {
                Player winner = Bukkit.getPlayer(result.winnerId());
                if (winner == null) {
                    broadcast("<red>Winner disconnected — auction cancelled.");
                    endSession();
                    return;
                }

                // Withdraw money from winner
                EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
                var provider = econ.getActiveProvider();
                if (provider != null && !manager.isBot(winner)) {
                    provider.withdraw(winner, result.highestBid());
                }

                broadcast("<gold><bold>" + winner.getName() + " won!</bold> (<yellow>$"
                        + org.yuemi.libs.api.util.NumberUtils.formatSuffix(result.highestBid()) + "</yellow>)");
                startWinnerRevealAnimation(winner);
            }
            case NO_WINNER_CONTINUE -> {
                broadcast("<red>No winner in round " + currentRound + ". Next round...");
                currentRound++;
                startPreviewState();
            }
            case BONUS_ROUND_REQUIRED -> {
                broadcast("<light_purple><bold>BONUS ROUND!</bold> (1.0x multiplier)</light_purple>");
                currentRound++;
                startPreviewState();
            }
            case AUCTION_ENDED -> {
                broadcast("<red><bold>Auction ended — no winner.");
                endSession();
            }
        }
    }

    private void startWinnerRevealAnimation(@NotNull Player winner) {
        boolean revealEnabled = manager.getPlugin().getConfig().getBoolean("container.reveal.enabled", true);
        boolean useAnimation = revealEnabled && manager.getPlugin().getConfig().getBoolean("container.reveal.animation", true);

        if (!revealEnabled) {
            // Reveal disabled — skip the GUI entirely and award immediately
            awardPrizes(winner);
            endSession();
            return;
        }

        revealAnimationTick = -1;
        revealActive = true;
        Gui revealGui = buildRevealGui();
        this.revealGui = revealGui;
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            revealGui.open(player);
        }

        long initialDelay = useAnimation ? 100L : 60L;
        long period = useAnimation ? 3L : 15L;

        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!revealActive) {
                    cancel();
                    return;
                }

                if (revealAnimationTick >= generatedPrizes.size() * 4) {
                    cancel();
                    revealActive = false;
                    awardPrizes(winner);
                    closeRevealGui();
                    endSession();
                    return;
                }

                if (useAnimation) {
                    revealAnimationTick++;
                } else {
                    revealAnimationTick += 4;
                }

                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    revealGui.update(player);

                    // Update counter when an item is fully revealed
                    if (revealAnimationTick >= 4 && revealAnimationTick % 4 == 0) {
                        int revealedNum = revealAnimationTick / 4;
                        revealGui.updateTitle(player, "Auction Reveal (" + revealedNum + "/" + generatedPrizes.size() + ")");
                    }

                    if (useAnimation) {
                        // Flicker tick on each color phase
                        if (revealAnimationTick % 2 == 1) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                        }
                    }

                    // Reveal sound when an item becomes visible
                    if (revealAnimationTick >= 4 && revealAnimationTick % 4 == 0) {
                        int itemIndex = revealAnimationTick / 4 - 1;
                        if (itemIndex >= 0 && itemIndex < prizeStates.size()) {
                            PrizeState ps = prizeStates.get(itemIndex);
                            var rarityInfo = RarityRegistry.get(ps.getConfig().getRarity());
                            List<String> revealSounds = rarityInfo != null ? rarityInfo.getRevealSounds() : null;
                            if (revealSounds != null) {
                                for (String soundName : revealSounds) {
                                    var soundKey = org.bukkit.NamespacedKey.fromString(soundName.toLowerCase());
                                    if (soundKey == null) continue;
                                    Sound sound = org.bukkit.Registry.SOUND_EVENT.get(soundKey);
                                    if (sound != null) {
                                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                                    }
                                }
                            } else {
                                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(manager.getPlugin(), initialDelay, period);
    }

    /**
     * Award all prizes to the winner — virtual deposits, commands, and physical
     * item drops. Sends the reward summary message to the winner.
     */
    private void awardPrizes(@NotNull Player winner) {
        var mm = MiniMessage.miniMessage();
        EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
        var provider = econ.getActiveProvider();

        boolean isBotWinner = manager.isBot(winner);
        long virtualCount = 0;
        long physicalCount = 0;
        double totalVirtualWorth = 0;
        double totalItemWorth = 0;

        for (PrizeState prizeState : prizeStates) {
            ItemStack stack = prizeState.getOriginalStack();
            ItemConfig config = prizeState.getConfig();
            if (config != null) {
                if (config.isVirtualItem()) {
                    double itemWorth = config.getWorth() * stack.getAmount();
                    totalVirtualWorth += itemWorth;
                    virtualCount++;
                    if (provider != null && !isBotWinner) {
                        provider.deposit(winner, itemWorth);
                    }
                } else {
                    totalItemWorth += config.getWorth() * stack.getAmount();
                    physicalCount++;
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
                }
            }
        }

        if (!isBotWinner) {
            double totalWorth = totalVirtualWorth + totalItemWorth;
            StringBuilder rewardMsg = new StringBuilder("<green><bold>Rewards!</bold> <gray>Total worth: <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(totalWorth) + "</gold>");
            if (virtualCount > 0) {
                rewardMsg.append(" <gray>(<gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(totalVirtualWorth) + "</gold> virtual</gray>");
                if (physicalCount > 0) {
                    rewardMsg.append(" <gray>+ " + physicalCount + " item" + (physicalCount > 1 ? "s" : "") + "</gray>");
                }
                rewardMsg.append(")");
            } else if (physicalCount > 0) {
                rewardMsg.append(" <gray>(" + physicalCount + " item" + (physicalCount > 1 ? "s" : "") + ")</gray>");
            }
            winner.sendMessage(mm.deserialize(rewardMsg.toString()));
        }
    }

    private Gui buildRevealGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();

        var builder = guiApi.createBuilder()
                .title("Auction Reveal")
                .rows(6)
                .closePolicy(ClosePolicy.CLOSE)
                .onClose(player -> {
                    // Player closed the reveal early — skip to prize awarding
                    if (!revealActive) return;
                    revealActive = false;
                    cancelActiveTask();
                    awardPrizes(player);
                    endSession();
                });

        // Layer for actual items (priority 3) — shown after flicker animation completes
        builder.createLayer("reveal_items", 3, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                PrizeState state = prizeStates.get(i);
                int[] pos = state.getPosition();
                int slot = pos[0] * 9 + pos[1];

                final int finalI = i;

                ItemStack prizeItem = buildContainerItemStack(state, true);
                GuiItem prizeGuiItem = guiApi.createItemBuilder()
                        .item(prizeItem)
                        .condition(player -> revealAnimationTick >= finalI * 4 + 4)
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();
                layer.setItem(slot, prizeGuiItem);
            }
        });

        // Layer for color flicker phase (priority 2) — shows during the two
        // color sub-steps of each item's flicker animation (localTick 1 and 3)
        builder.createLayer("reveal_flicker", 2, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                PrizeState state = prizeStates.get(i);
                int[] pos = state.getPosition();
                int itemWidth = state.getConfig().getWidth();
                int itemHeight = state.getConfig().getHeight();

                final int finalI = i;

                ItemStack flickerGlass = new ItemStack(
                        GlassPaneMapper.getMaterial(
                                getRarityColor(state.getConfig().getRarity())));
                ItemMeta flickerMeta = flickerGlass.getItemMeta();
                if (flickerMeta != null) {
                    flickerMeta.displayName(Component.text(" "));
                    flickerMeta.lore(List.of(Component.text(" ")));
                    flickerGlass.setItemMeta(flickerMeta);
                }
                GuiItem flickerGuiItem = guiApi.createItemBuilder()
                        .item(flickerGlass)
                        .condition(player -> {
                            if (revealAnimationTick < 0) return false;
                            int localTick = revealAnimationTick - finalI * 4;
                            return localTick >= 1 && localTick < 4 && localTick % 2 == 1;
                        })
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();

                for (int row = 0; row < itemHeight; row++) {
                    for (int col = 0; col < itemWidth; col++) {
                        int slot = (pos[0] + row) * 9 + (pos[1] + col);
                        layer.setItem(slot, flickerGuiItem);
                    }
                }
            }
        });

        // Layer for black glass placeholders (priority 1) — covers every slot that
        // hasn't been fully revealed yet (both waiting and black flicker phases).
        // Items in their color flicker phase are overridden by the priority-2 layer.
        builder.createLayer("reveal_placeholders", 1, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                PrizeState state = prizeStates.get(i);
                int[] pos = state.getPosition();
                int itemWidth = state.getConfig().getWidth();
                int itemHeight = state.getConfig().getHeight();

                final int finalI = i;

                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta glassMeta = blackGlass.getItemMeta();
                if (glassMeta != null) {
                    glassMeta.displayName(Component.text(" "));
                    glassMeta.lore(List.of(Component.text(" ")));
                    blackGlass.setItemMeta(glassMeta);
                }
                GuiItem paneGuiItem = guiApi.createItemBuilder()
                        .item(blackGlass)
                        .condition(player -> revealAnimationTick < finalI * 4 + 4)
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();

                for (int row = 0; row < itemHeight; row++) {
                    for (int col = 0; col < itemWidth; col++) {
                        int slot = (pos[0] + row) * 9 + (pos[1] + col);
                        layer.setItem(slot, paneGuiItem);
                    }
                }
            }
        });

        return builder.build();
    }

    @NotNull
    private static String getMultiplierColor(double multiplier) {
        if (multiplier >= 8.0) return "§c"; // red
        if (multiplier >= 6.0) return "§6"; // gold
        if (multiplier >= 3.0) return "§e"; // yellow
        return "§a"; // green
    }

    @NotNull
    private static String getTimeColor(int secondsLeft, int totalSeconds) {
        double pct = (double) secondsLeft / totalSeconds;
        if (pct <= 0.20) {
            return "red";
        } else if (pct <= 0.60) {
            return "yellow";
        }
        return "green";
    }

    /**
     * Resolve a rarity ID to its configured color name for glass mapping.
     * Falls back to the raw rarity ID if no RarityInfo is registered.
     */
    @NotNull
    private static String getRarityColor(@NotNull String rarityId) {
        var info = RarityRegistry.get(rarityId);
        return info != null ? info.getColor() : rarityId;
    }

    @NotNull
    private BukkitTask startCountdown(int totalSeconds, @NotNull java.util.function.Consumer<Integer> onTick) {
        return new BukkitRunnable() {
            int secondsLeft = totalSeconds;
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    this.cancel();
                    return;
                }
                onTick.accept(secondsLeft);
                secondsLeft--;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 20L);
    }

    private double getMultiplier() {
        double mult = 1.0;
        if (currentRound - 1 < arena.getMultipliers().size()) {
            mult = arena.getMultipliers().get(currentRound - 1);
        }
        return Math.max(1.0, Math.min(10.0, mult));
    }

    private String getRoundTitle(String suffix) {
        boolean isBonus = currentRound > arena.getMultipliers().size();
        String name = arena.getName().replaceAll("(?i)\\s+Arena$", "");
        if (isBonus) {
            return name + " \u00A7lBONUS" + suffix;
        } else {
            return name + " (" + currentRound + "/" + arena.getMultipliers().size() + ")" + suffix;
        }
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

    private void cancelBidCountdownTask() {
        if (bidCountdownTask != null) {
            bidCountdownTask.cancel();
            bidCountdownTask = null;
        }
    }

    private void cancelPreviewCountdownTask() {
        if (previewCountdownTask != null) {
            previewCountdownTask.cancel();
            previewCountdownTask = null;
        }
    }

    private void closePreviewGui() {
        if (previewGui != null) {
            previewGui.setClosePolicy(ClosePolicy.CLOSE);
            for (Player player : players) {
                if (manager.isBot(player)) continue;
                player.closeInventory();
            }
            previewGui = null;
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
        revealActive = false;
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
        double roll = botRandom.nextDouble();
        if (roll < 0.10) {
            return binPrice;
        } else if (roll < 0.85) {
            double minBid = currentBasePrice;
            double maxBid = binPrice - 1.0;
            if (maxBid <= minBid) {
                return minBid;
            }
            return Math.floor(minBid + botRandom.nextDouble() * (maxBid - minBid));
        } else {
            return 0.0;
        }
    }

    private void endSession() {
        cancelActiveTask();
        cancelBidCountdownTask();
        cancelPreviewCountdownTask();
        closePreviewGui();
        closeGraphGui();
        closeRevealGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            player.closeInventory();
        }
        manager.sessionEnded(this);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void handlePlayerDisconnect(@NotNull Player disconnectedPlayer) {
        boolean hasOtherRealPlayers = players.stream()
                .anyMatch(p -> !manager.isBot(p) && p.isOnline() && !p.getUniqueId().equals(disconnectedPlayer.getUniqueId()));

        if (!hasOtherRealPlayers) {
            broadcast("<red>All players left — auction cancelled.");
            endSession();
        } else {
            broadcast("<gray>" + disconnectedPlayer.getName() + " disconnected.");
            skipPlayerBid(disconnectedPlayer);
        }
    }

    /**
     * Handle a player's in-game death during an auction.
     * <p>
     * The player remains online (unlike disconnect), so the auction continues
     * with other players. Their current bid is forced to $1.0 if the bidding
     * phase is active and they haven't bid yet, and their open GUI inventory
     * is closed so the death screen displays correctly.
     *
     * @param deadPlayer the player who died
     */
    public void handlePlayerDeath(@NotNull Player deadPlayer) {
        broadcast("<gray>" + deadPlayer.getName() + " died.");

        // Set GUI close policies to CLOSE so the reopen handler does not
        // fight with the death screen when we close the inventory.
        if (previewGui != null) {
            previewGui.setClosePolicy(ClosePolicy.CLOSE);
        }
        if (graphGui != null) {
            graphGui.setClosePolicy(ClosePolicy.CLOSE);
        }
        if (revealGui != null) {
            revealGui.setClosePolicy(ClosePolicy.CLOSE);
        }

        skipPlayerBid(deadPlayer);
        deadPlayer.closeInventory();
    }

    /**
     * Force the player's bid to $1.0 if the bidding phase is active
     * and they have not yet submitted a bid.
     */
    private void skipPlayerBid(@NotNull Player player) {
        if (biddingActive && !currentBids.containsKey(player.getUniqueId())) {
            currentBids.put(player.getUniqueId(), 1.0);
            bidOrder.add(player.getUniqueId());
            if (currentBids.size() >= players.size()) {
                startGraphicsState();
            }
        }
    }
}
