# core-plugin

**Plugin implementation module** — the actual PaperMC plugin JAR that gets deployed to servers. Shades `core-api`, `module-bot`, `module-matchs`, and `module-seeders` into a single deployable artifact.

## Purpose

Contains all server-side logic: lifecycle management, commands, GUI rendering, auction sessions, config loading, and event handling. This is the runtime module — nothing here is published for external consumption.

## Build

- **Build output:** `MagicAuction-<version>.jar` (shaded, deployable)
- **Build command:** `./gradlew :core-plugin:shadowJar`
- **Output path:** `build/libs/MagicAuction-<version>.jar`

## Dependencies (shaded)

| Dependency | Source |
|-----------|--------|
| core-api | local project |
| module-bot | local project |
| module-matchs | local project |
| module-seeders | local project |
| bstats-bukkit | Maven (relocated to `org.yuemi.libs.bstats`) |
| mc-config-libs | Maven (relocated to `org.yuemi.libs.config`) |

## Package

```
org.yuemi.magicauction.plugin
```

## Key Classes

| Class | Description |
|-------|-------------|
| `MagicAuctionPlugin` | Main plugin class — `onEnable`/`onDisable`, initialises `AuctionManager`, registers commands and API service |
| `MagicAuctionApiImpl` | Implementation of `MagicAuctionApi` registered as a Bukkit service |

### `game/` — Session & auction lifecycle

| Class | Description |
|-------|-------------|
| `AuctionManager` | Loads configs (items, arenas, rarities, types, events), manages active `AuctionSession`s, provides `BotHandler` |
| `AuctionSession` | Full auction lifecycle: preview → bidding → graph → evaluate → reveal → end. Owns round state, prizes, GUIs |
| `PrizeState` | Per-prize mutable state: position, reveal flags, hide state, NBT UUID |

### `config/` — Configuration loading

| Class | Description |
|-------|-------------|
| `ArenaConfig` | Arena YAML model: multipliers, base-price, thinking/bid time, rewards, events |
| `ItemConfig` | Custom item YAML model: base-item, display name, rarity, type, size, worth, rewards |
| `EventConfig` | Round event YAML model: type/rarity/size reveal actions with conditions |
| `RarityRegistry` | Loads and caches rarity definitions from `rarities/*.yml` |
| `TypeRegistry` | Loads and caches type definitions from `types/*.yml` |
| `EventRegistry` | Loads and caches event definitions from `events/*.yml` |

### `commands/` — Command handling

| Class | Description |
|-------|-------------|
| `CommandRegistry` | Registers the root `/magicauction` command and its subcommands |
| `MagicAuctionCommand` | Root command dispatcher |
| `StartSubCommand` | `/magicauction start <arena> [seed] <p1> <p2> <p3> <p4>` — parses args, resolves players/bots, starts session |

### `bstats/` — Metrics

| Class | Description |
|-------|-------------|
| `BStatsService` | Initialises bStats metrics on enable |

## Conventions

- All classes are implementation details — not part of the public API
- Uses `YueMiLibsProvider.getApi()` for economy, items, and GUI builders
- Adventure MiniMessage for all chat formatting
- GUI lifecycle follows strict `ClosePolicy.REOPEN` → `CLOSE` → close pattern before state transitions
