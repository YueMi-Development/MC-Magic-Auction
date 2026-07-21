# module-matchs

**Auction match evaluation module** — pure logic that determines round outcomes from bids and configuration. No Bukkit dependency, easily testable.

## Purpose

Extracts the BIN threshold calculation, winner determination, tie-breaking, and bonus-round detection into a stateless evaluator. `AuctionSession` delegates to this module instead of doing the math inline.

## Build

- **Build output:** Plain Java library JAR, shaded into `core-plugin`
- **Build command:** `./gradlew :module-matchs:build`

## Dependencies

| Dependency | Scope |
|-----------|-------|
| JetBrains Annotations | compileOnly |

## Package

```
org.yuemi.magicauction.matchs
org.yuemi.magicauction.matchs.model
```

## Key Classes

| Class | Description |
|-------|-------------|
| `AuctionMatchEvaluator` | **Stateless static evaluator** — `evaluateRound(RoundContext)` returns a `RoundResult` |
| `Bid` | Record — `(UUID playerId, double amount)` |
| `RoundContext` | Input record: bids, bidOrder, basePrice, multiplier, round number, total rounds, hasMultiplierOne |
| `RoundResult` | Output — `outcome()` returns `PLAYER_WON`, `NO_WINNER_CONTINUE`, `BONUS_ROUND_REQUIRED`, or `AUCTION_ENDED` |

## Matching Rules

| Rule | Detail |
|------|--------|
| **BIN threshold** | `max(secondHighestBid, basePrice) × multiplier` |
| **Winner** | Highest bidder whose bid ≥ BIN threshold |
| **Last-round tie** | Among tied top bidders, the first to submit their bid wins |
| **Bonus round** | If final round has no winner and no 1.0× multiplier is configured, a forced bonus round (1.0×) is triggered |
| **Multiplier clamp** | Always clamped to `[1.0, 10.0]` |

## Conventions

- Pure Java — no Bukkit, no Paper, no Minecraft imports
- Stateless and thread-safe — one `evaluateRound` call produces one result
- All validation is in the constructor/record canonical builder (reject invalid states early)
