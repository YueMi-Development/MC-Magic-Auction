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
    private int currentRevealStep = -1;
    private int currentGraphProgress = 0;
    private boolean biddingActive = false;
    private Gui graphGui;
    private Gui revealGui;
    
    // Bids for the current round
    private final Map<UUID, Double> currentBids = new HashMap<>();
    private final List<UUID> bidOrder = new ArrayList<>();
    
    private BukkitTask activeTask;
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
        broadcast("<green>Auction starting for arena: <yellow>" + arena.getName() + "</yellow> (Seed: " + seed + ")!");
        
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
        broadcast("<gray>------------------------------------</gray>");
        broadcast("<light_purple><bold>Event triggered:</bold> <yellow>" + event.getId().replace("_", " ") + "</yellow></light_purple>");
        for (EventConfig.ActionEntry action : event.getActions()) {
            executeAction(action);
        }
        broadcast("<gray>------------------------------------</gray>");
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
            String revealedDetail = "";
            switch (action.getType()) {
                case "type" -> {
                    state.setTypeRevealed(true);
                    var typeInfo = TypeRegistry.get(state.getConfig().getType());
                    String typeName = typeInfo != null ? typeInfo.getName() : state.getConfig().getType();
                    revealedDetail = "Type: " + typeName;
                }
                case "rarity" -> {
                    state.setRarityRevealed(true);
                    var rarityInfo = RarityRegistry.get(state.getConfig().getRarity());
                    String rarityName = rarityInfo != null ? rarityInfo.getName() : state.getConfig().getRarity();
                    revealedDetail = "Rarity: " + rarityName;
                }
                case "size" -> {
                    state.setSizeRevealed(true);
                    revealedDetail = "Size: " + state.getConfig().getWidth() + "x" + state.getConfig().getHeight();
                }
                case "full" -> {
                    state.setFullyRevealed(true);
                    revealedDetail = "Item: " + state.getConfig().getDisplayName() + " (Full Info)";
                }
            }

            int row = state.getPosition()[0] + 1;
            int col = state.getPosition()[1] + 1;
            broadcast("<gray> - Revealed " + revealedDetail + " at Row " + row + ", Column " + col + "</gray>");
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
                material = GlassPaneMapper.getMaterial(state.getConfig().getRarity());
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
        currentBids.clear();
        bidOrder.clear();

        double multiplier = getMultiplier();
        double binPrice = currentBasePrice * multiplier;

        // Apply round event
        EventConfig event = getRoundEvent();
        if (event != null) {
            triggeredEvents.add(event.getId().toLowerCase());
            executeEvent(event);
        }

        broadcast("<gray>------------------------------------");
        broadcast("<green>Round " + currentRound + " Preview Starts!");
        broadcast("<gray>BIN (Buy It Now) Price: <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(binPrice) + "</gold>");
        broadcast("<gray>------------------------------------");

        this.previewGui = buildPreviewGui();
        for (Player player : players) {
            if (manager.isBot(player)) continue;
            previewGui.open(player);
        }

        activeTask = new BukkitRunnable() {
            int timeLeft = 15;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    closePreviewGui();
                    startBiddingState();
                    return;
                }

                for (Player player : players) {
                    if (manager.isBot(player)) continue;
                    if (previewGui != null) {
                        previewGui.updateTitle(player, getRoundTitle(" | Time: " + timeLeft + "s"));
                    }
                }

                timeLeft--;
            }
        }.runTaskTimer(manager.getPlugin(), 0L, 20L);
    }

    private Gui buildPreviewGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();
        
        var builder = guiApi.createBuilder()
                .title(getRoundTitle(" | Time: " + arena.getThinkingTime() + "s"))
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        // Add prizes layer packed in 3x6 starting at row 1, col 1
        builder.createLayer("prizes", 1, layer -> {
            for (PrizeState state : prizeStates) {
                int[] pos = state.getPosition();
                int startY = pos[0];
                int startX = pos[1];
                
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
        biddingActive = true;
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
                        bidOrder.add(player.getUniqueId());
                        broadcast("<gray>" + player.getName() + " has submitted their bid.");
                        
                        if (currentBids.size() >= players.size()) {
                            startGraphicsState();
                        }
                    }
                }, 20L + (long) (Math.random() * 40L));
            } else if (!player.isOnline()) {
                currentBids.put(player.getUniqueId(), 1.0);
                bidOrder.add(player.getUniqueId());
                broadcast("<gray>" + player.getName() + " is offline — skipped and bid set to $1.");
                if (currentBids.size() >= players.size()) {
                    startGraphicsState();
                }
            } else {
                openSinglePlayerBidding(player, binPrice);
            }
        }

        // Bid timeout: force any player who has not bid to bid $1
        activeTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean forced = false;
                for (Player player : players) {
                    if (!currentBids.containsKey(player.getUniqueId())) {
                        currentBids.put(player.getUniqueId(), 1.0);
                        bidOrder.add(player.getUniqueId());
                        broadcast("<gray>" + player.getName() + " did not bid in time — forced bid of <gold>$1</gold>.");
                        forced = true;
                    }
                }
                if (forced) {
                    broadcast("<gray>Bidding time ended.");
                }
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
                .title("Balance: $" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(playerBalance))
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
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid bid! Must be a number or valid format (e.g. 10k)."));
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
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Bid of <yellow>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(bid) + "</yellow> registered."));

                    if (currentBids.size() >= players.size()) {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), this::startGraphicsState);
                    } else {
                        Bukkit.getScheduler().runTask(manager.getPlugin(), this::showGraphView);
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
        biddingActive = false;
        closeGraphGui();
        broadcast("<green>All bids collected! Simulating bid graphics...");

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

        String graphTitle = "Bidding Progress (" + (currentRound > arena.getMultipliers().size() ? "\u00A7lBONUS" : "Round " + currentRound) + ")";
        var builder = guiApi.createBuilder()
                .title(graphTitle)
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
                    skullMeta.displayName(mm.deserialize("<yellow>" + p.getName()));
                    skullMeta.lore(List.of(
                            mm.deserialize("<gray>Bid: <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(bid))
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
                        paneMeta.displayName(mm.deserialize("<gray>Progress"));
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
                    broadcast("<red>Winner disconnected unexpectedly. Auction cancelled.");
                    endSession();
                    return;
                }

                // Withdraw money from winner
                EconomyApi econ = YueMiLibsProvider.getApi().getEconomy();
                var provider = econ.getActiveProvider();
                if (provider != null && !manager.isBot(winner)) {
                    provider.withdraw(winner, result.highestBid());
                }

                broadcast("<gold><bold>WINNER!</bold> <yellow>" + winner.getName()
                        + "</yellow> won the auction with a bid of <gold>$"
                        + org.yuemi.libs.api.util.NumberUtils.formatSuffix(result.highestBid()) + "</gold>!");
                startWinnerRevealAnimation(winner);
            }
            case NO_WINNER_CONTINUE -> {
                broadcast("<red>No players matched the required BIN price of <gold>$"
                        + org.yuemi.libs.api.util.NumberUtils.formatSuffix(result.binThreshold())
                        + "</gold> in round " + currentRound + ".");
                currentRound++;
                startPreviewState();
            }
            case BONUS_ROUND_REQUIRED -> {
                broadcast("<red>No players matched the required BIN price of <gold>$"
                        + org.yuemi.libs.api.util.NumberUtils.formatSuffix(result.binThreshold())
                        + "</gold> in round " + currentRound + ".");
                broadcast("<light_purple><bold>BONUS ROUND!</bold></light_purple> <yellow>No winner yet and multiplier 1.0 is not configured. Starting a bonus round with multiplier 1.0!</yellow>");
                currentRound++;
                startPreviewState();
            }
            case AUCTION_ENDED -> {
                broadcast("<red><bold>AUCTION OVER!</bold> No winner. Items have been locked away.");
                endSession();
            }
        }
    }

    private void startWinnerRevealAnimation(@NotNull Player winner) {
        var mm = MiniMessage.miniMessage();

        currentRevealStep = -1;
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
                    for (PrizeState prizeState : prizeStates) {
                        ItemStack stack = prizeState.getOriginalStack();
                        ItemConfig config = prizeState.getConfig();
                        if (config != null) {
                            if (config.isVirtualItem()) {
                                // Virtual Item: Award worth to economy
                                double totalWorth = config.getWorth() * stack.getAmount();
                                if (provider != null && !isBotWinner) {
                                    provider.deposit(winner, totalWorth);
                                    winner.sendMessage(mm.deserialize("<green>Awarded <gold>$" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(totalWorth) + "</gold> for virtual item: <yellow>" + config.getDisplayName() + "</yellow> (Worth: $" + org.yuemi.libs.api.util.NumberUtils.formatSuffix(config.getWorth()) + " each)"));
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
        }.runTaskTimer(manager.getPlugin(), 100L, 15L);
    }

    private Gui buildRevealGui() {
        GuiApi guiApi = YueMiLibsProvider.getApi().getGui();

        var builder = guiApi.createBuilder()
                .title("Auction Reveal")
                .rows(6)
                .closePolicy(ClosePolicy.REOPEN);

        // Layer for actual items (priority 2)
        builder.createLayer("reveal_items", 2, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                PrizeState state = prizeStates.get(i);
                int[] pos = state.getPosition();
                int slot = pos[0] * 9 + pos[1];

                final int finalI = i;

                ItemStack prizeItem = buildContainerItemStack(state, true);
                GuiItem prizeGuiItem = guiApi.createItemBuilder()
                        .item(prizeItem)
                        .condition(player -> finalI <= currentRevealStep)
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();
                layer.setItem(slot, prizeGuiItem);
            }
        });

        // Layer for black glass placeholders (priority 1)
        builder.createLayer("reveal_placeholders", 1, layer -> {
            for (int i = 0; i < generatedPrizes.size(); i++) {
                PrizeState state = prizeStates.get(i);
                int[] pos = state.getPosition();
                int slot = pos[0] * 9 + pos[1];

                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta meta = blackGlass.getItemMeta();
                if (meta != null) {
                    meta.displayName(MiniMessage.miniMessage().deserialize("<red>???</red>"));
                    blackGlass.setItemMeta(meta);
                }
                GuiItem paneGuiItem = guiApi.createItemBuilder()
                        .item(blackGlass)
                        .onClick((p, ctx) -> ctx.getEvent().setCancelled(true))
                        .build();
                layer.setItem(slot, paneGuiItem);
            }
        });

        return builder.build();
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
        if (isBonus) {
            return "\u00A7lBONUS" + suffix;
        } else {
            return "Round " + currentRound + "/" + arena.getMultipliers().size() + suffix;
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
            broadcast("<red>No real players remaining in the auction. Auction cancelled.");
            endSession();
        } else {
            broadcast("<gray>" + disconnectedPlayer.getName() + " disconnected. Skipping their bids for the rest of the auction.");
            if (biddingActive && !currentBids.containsKey(disconnectedPlayer.getUniqueId())) {
                currentBids.put(disconnectedPlayer.getUniqueId(), 1.0);
                bidOrder.add(disconnectedPlayer.getUniqueId());
                if (currentBids.size() >= players.size()) {
                    startGraphicsState();
                }
            }
        }
    }
}
