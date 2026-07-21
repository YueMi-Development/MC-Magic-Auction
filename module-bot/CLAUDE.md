# module-bot

**Bot players module** — simulates fake players that behave as auction participants so sessions can run with fewer than 4 real players.

## Purpose

Provides a `BotHandler` interface and a `BotProviderImpl` that creates `Player` proxies (via `BotPlayerProxy`). Bots bid with realistic timing (1–3s random delay) and amounts based on the round's BIN price. Economy operations and reward distribution are guarded against bot winners.

## Build

- **Build output:** Library JAR, shaded into `core-plugin`
- **Build command:** `./gradlew :module-bot:build`

## Dependencies

| Dependency | Scope |
|-----------|-------|
| Paper API | compileOnly |
| YueMiLibs-api | compileOnly |

## Package

```
org.yuemi.magicauction.bot
```

## Key Classes

| Class | Description |
|-------|-------------|
| `BotHandler` | Interface — `createBot(String name)` and `isBot(Player)` |
| `BotProviderImpl` | Implementation backed by `BotPlayerProxy` |
| `BotPlayerProxy` | Dynamic proxy wrapping a `Player` with bot identity |

## Conventions

- Bot bidding is simulated via `botRandom` (isolated `Random` instance in `AuctionSession`) — separate from the main session random for deterministic reproducibility
- Bot names follow the pattern `Bot 1`, `Bot 2`, etc. (resolved from `_BOT_` arguments)
- If only bots remain after a player disconnects, the auction is cancelled
