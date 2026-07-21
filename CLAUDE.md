# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MagicAuction** is a PaperMC plugin (1.21+) that implements a container-bidding auction minigame inspired by games where players bid on mystery boxes/items across escalating rounds.

## Build & Development Commands

```bash
# Build all modules (produces shaded plugin JAR in core-plugin/build/libs/)
./gradlew clean build

# Build only the API module (for publishing)
./gradlew :core-api:build

# Build only the plugin shaded JAR (skip API module build)
./gradlew :core-plugin:shadowJar

# Publish API to Yuemi Maven
./gradlew :core-api:publish

# Generate Javadocs
./gradlew :core-api:javadoc

# Test compilation (no full build)
./gradlew clean compileJava compileTestJava
```

The output JAR is at `core-plugin/build/libs/MagicAuction-<version>.jar` — deployable directly to a PaperMC server's `plugins/` directory.

## Game Design — Auction Rounds

The core mechanic is a **container bidding game** with these rules:

```mermaid
flowchart TD
    A["Auction Starts<br/>(4 players)"] --> B["Preview phase<br/>Countdown + action bar"]
    B --> C["Bidding phase opens<br/>Anvil GUI to enter amount"]
    C --> D{"All bids in<br/>or time runs out?"}
    D -->|Yes| E["Bidding graph shows<br/>live standings"]
    E --> F{"Highest bid ≥<br/>BIN threshold?"}
    F -->|Yes| G["Player wins<br/>the auction!"]
    F -->|No| H["No winner this round<br/>Next round starts"]
    H --> B
    G --> I["Winner reveal<br/>+ rewards summary"]

    style I fill:#4a4,color:#fff
    style G fill:#44a,color:#fff
```

**BIN (Buy It Now):** To win instantly, a player's bid must be at least `(highest opposing bid) × round_multiplier`. The BIN threshold is checked at round evaluation: the highest bidder wins only if their bid ≥ (second-highest bid × multiplier). If only one player bids, the arena `base-price` serves as the opposing bid floor.

**Preview Phase:** After the round starts, a preview chest opens showing items. A live action bar countdown shows remaining thinking time (color-coded green/yellow/red). When it expires, the bidding phase begins.

**Bidding Phase:** Each player gets an anvil GUI showing their balance and current multiplier. They have `bid-time` seconds to enter an amount — the action bar shows a live countdown. If a player fails to bid in time, their bid is forced to $1. Once a player bids, they see the bid progress graph with current standings.

**Post-Graph Delay:** After all bids are collected, a graph animation plays showing the bidding progress bars (colored by BIN status), followed by round evaluation.

**Default round multipliers (configurable via `arena/*.yml`):**

| Round | Overbid Multiplier | Vibe |
|-------|-------------------|------|
| 1     | 2.0× | "You *really* want it?" |
| 2     | 1.5× | Getting warmer |
| 3     | 1.3× | Tempting... |
| 4     | 1.1× | Sneaky territory |
| 5     | 1.0× | At cost — BIN or lose it |

- **Players:** 4 per auction session (each opens their own container/chest to bid)
- **Rounds:** Determined by the number of entries in `multipliers` list (default 5)
- **BIN mechanic:** Each round has an overbid multiplier — to win instantly ("BIN"), a player must outbid the current highest opposing bid by at least that multiplier. The first player to BIN wins the auction — no going once, twice, gone.
- **Winner:** The player who successfully BINs on a round takes the auction item
- **The rising tension:** early rounds require a huge overbid, late rounds let players snipe at near-market price. If nobody BINs through all rounds, the auction ends with no winner.

All round counts and multipliers are configurable via `arena/*.yml` files. Server admins can also configure `thinking-time` (preview duration) and `bid-time` (bidding duration).

### Arena Config (`auction/*.yml`)

Each arena file supports:

| Field | Default | Description |
|-------|---------|-------------|
| `thinking-time` | 15 | Preview countdown duration in seconds |
| `bid-time` | 30 | Bidding phase duration in seconds before timeout |
| `base-price` | 100 | Starting price floor for BIN calculation |
| `multipliers` | — | Per-round overbid multipliers (list length = round count) |
| `rewards` | — | Prize pool for this arena |
| `min-items` | sum of rewards | Minimum number of items to select for the session chest preview |
| `max-items` | sum of rewards | Maximum number of items to select for the session chest preview |
| `start-events` | 0 | Number of events to run at the start of Round 1 before the preview opens |
| `events` | — | Shuffled round events config files (list length = round count) |

### Plugin Lifecycle

- **onEnable:** Initializes config, copies default resources for `auction/` and `items/`, starts `AuctionManager`, registers commands via `CommandRegistry`, and exposes `MagicAuctionApi` service.
- **onDisable:** Unregisters the API service.

### Config & Item Resolution

- **3x6 Grid Packing**: Inside the 6x9 chest preview GUI, items are packed in a 3x6 container offset (starts Row 1, Column 1) starting from the top-left available position. Items specify width and height and cannot overlap or exceed boundaries.
- **Seed System**: When starting an auction, an optional seed can be passed. If it is `<= 0` (including `-1` or other negative values) or omitted, a random positive seed is generated. Shuffling and packing of prizes is governed by a `java.util.Random` initialized with this seed to guarantee deterministic layout reproduction.
- **Round Events & Container Masking**: The bidding preview container is masked using stained glass panes. From the beginning of the round, all items are rendered inside the preview chest. By default, unrevealed items start in a **hide state** (rendering as a 1x1 black stained glass pane named `<red>???</red>` with placeholders `???` in lore). Once any property (rarity, size, or type) is revealed, the hide state is disabled, rendering the item as a partially-revealed shape. If size is revealed, it renders using the item's full width and height; if rarity is also revealed, it uses the rarity-colored glass pane.
- **Unique NBT Identifiers**: Every container item in the session is assigned a unique `java.util.UUID` stored in `PrizeState`. Rendered item stacks in preview and reveal GUIs are tagged with this UUID via NBT (`auction_item_uid`), ensuring duplicate items (e.g. multiple Diamond Blocks) are treated as distinct instances without slot or click conflicts.
- **Winner Reveal Animation**: Runs when a winner is decided. Opens the reveal GUI showing all items as black stained glass panes (blank display name) and remains in this initial state for a **5-second delay** (100 ticks). Afterward, items are revealed one-by-one in-place to their actual fully-revealed icons. This is powered by two conditional GUI layers: `reveal_items` (priority 2, condition `finalI <= currentRevealStep`) and `reveal_placeholders` (priority 1, no condition).
- **Events Configuration & Reproducibility**: The events list declared in the arena config is shuffled deterministically at session start using the session's random generator. Each round executes one event from this shuffled sequence (with fallback to `events/round_<N>.yml` based on round index). Event files must include `name` and `desc` fields, followed by an `actions` list (e.g. `type: "type"|"rarity"|"size"|"full"`, `selection: "random"|"all"`, `count: N`, and optional `conditions: {rarity: String, min-total-size: Int}`). To ensure deterministic and reproducible reveals, bot bidding simulation uses an isolated random generator (`botRandom`), leaving the primary session `Random` instance strictly dedicated to layout generation, event list shuffling, and event reveals.
- **Rarity Validation**: Rarity configuration files cannot use the color `black` as a valid color value to avoid collisions with the unknown/masked container states.
- **Base Item Resolution & Overrides**: Every custom item config defined under `items/` must specify a `base-item` resolved dynamically via YueMiLibs. Name, standard white `desc` (plain-text description), and custom model data overrides are conditionally applied to the base item stack.
- **Prizes & Rewards Resolution**: Arena rewards and container items are resolved strictly from the local `items/` directory configuration first.
- **Strict Reward Distribution**: Container item stacks are never given physically to the winning player.
  - If a custom item is virtual, the winner receives its `worth` directly in their economy balance.
  - If a custom item is non-virtual, its `rewards` list is processed, distributing either commands or clean item stacks resolved via YueMiLibs, avoiding leaking session NBT UUID keys to the player's inventory.
  - Rewards are summarized in a single chat message showing total worth with virtual/physical item breakdown.
- **Bot Players (`module-bot`)**: A separate subproject library shaded into the core plugin. Using `_BOT_` in arguments resolves to dynamic sequential bot players (`Bot 1`, `Bot 2`...). Bidding is simulated after a 1-3s delay with random realistic decisions, and inventory/economy operations are protected against bot winners.
- **Dynamic Bidding Progress Graph**: The bidding progress graph updates dynamically using `gui.update()` and conditional rendering (no more flickering). The progress graph stays open for 10 seconds post-calculation before proceeding to evaluation/next rounds.
- **Graph Visual Formatting**: Custom coloring rules on the bidding progress graph:
  - Lime/Green: Only for the winning bid if it meets the BIN threshold.
  - Yellow: For the highest bid if it fails to meet the BIN threshold, or for the second-highest (blocking) bid.
  - Red: For any other bids.
  - Progress bars show formatted bid amount on hover.
  - Graph title shows color-coded multiplier (green→yellow→gold→red by value).
  - Player heads show name + bid amount (non-italic).
- **Currency Formatting**: All currency outputs (BIN price, player balance, bid announcements, item worths, graph lore) use suffix formatting (e.g., `1.9k` instead of `1900`) using `NumberUtils.formatSuffix()`.
- **Player Disconnections & Death**: If a player disconnects or dies during an auction, a brief broadcast notifies others. Their bids are skipped for remaining rounds. If no real players remain, the auction is cancelled.
- **Tie-Breaker & Bonus Round**: If there is a tie on the last round, the first player to submit their bid wins. If the normal rounds end without a winner and no `1.0` multiplier is configured in the arena config, a bonus round with a `1.0` multiplier is forced.
- **Multiplier Clamping**: Multipliers are strictly clamped to a minimum of `1.0` and a maximum of `10.0` at runtime.

### GUI Lifecycle

All display GUIs (preview, bidding graph, reveal) use `ClosePolicy.REOPEN` during their active period so players cannot accidentally close them. **Before any state transition**, the current GUI must be explicitly set to `ClosePolicy.CLOSE` and the player's inventory closed. This prevents the REOPEN policy from fighting with the next GUI and corrupting the GUI framework state.

- **Preview → Bidding**: Preview GUI sets CLOSE before closing. A live action bar countdown (color-coded) runs during preview.
- **Bidding → Graph**: After all bids collected, any lingering graph GUI is closed via `closeGraphGui()`, then rebuilt and opened with animation started. Action bar countdown shows remaining bid time.
- **Graph → Preview/Reveal**: Graph GUI is closed with CLOSE policy before `evaluateRound()` transitions to the next state (after a 10-second stay delay)
- **Reveal → End**: Reveal GUI is closed with CLOSE policy via `closeRevealGui()` before `endSession()` closes all inventories

The anvil bidding GUI uses `ClosePolicy.CLOSE` (not REOPEN) to prevent the player from accidentally re-submitting and overwriting their bid. The `onClose` handler reopens the anvil only if the player has not yet bid. The anvil title shows balance and multiplier. All timers are shown via action bar only (not in GUI titles).

## Key Conventions

- **Package:** `org.yuemi.magicauction.(api|plugin).*`
- **Commands Packaging:** Root commands in `commands/`, subcommands in `commands/subcommands/` (one file per subcommand), registered via `CommandRegistry`.
- **YueMiLibs Integration:** Uses `YueMiLibsProvider.getApi()` to resolve economy, items, and layered GUI builders.
- **Messages:** Uses Adventure MiniMessage for rich text formatting.
- **Java 21, Gradle 8.13, Kotlin DSL**

## CI/CD

- **Build workflow** (`.github/workflows/build.yml`): On push to `main` / PR — `./gradlew clean build`, uploads JAR as artifact.
- **Publish workflow** (`.github/workflows/publish.yml`): On `v*` branch push — compiles, auto-updates contributors & version in `gradle.properties`, publishes API to Yuemi Maven, builds plugin JAR, generates Javadocs, deploys to GitHub Pages, publishes to Modrinth, and creates a GitHub Release with changelog.
