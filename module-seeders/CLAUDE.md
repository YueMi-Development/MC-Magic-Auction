# module-seeders

**Seed generation module** — produces and resolves seed values for deterministic auction sessions.

## Purpose

Centralises seed logic so that callers don't need ad-hoc `Random` / `Math.abs` boilerplate. A seed governs prize shuffling, packing, event selection, and any other randomised process within a session — the same seed always produces the same auction.

## Build

- **Build output:** Plain Java library JAR, shaded into `core-plugin`
- **Build command:** `./gradlew :module-seeders:build`

## Dependencies

| Dependency | Scope |
|-----------|-------|
| JetBrains Annotations | compileOnly |

## Package

```
org.yuemi.magicauction.seed
```

## Key Classes

| Class | Description |
|-------|-------------|
| `SeedGenerator` | `@FunctionalInterface` — `generateSeed()` + default `resolve(long)` method |
| `RandomSeedGenerator` | Default impl using `ThreadLocalRandom`, bounded to 1B |

## Resolution Rules (`resolve(long)`)

| Input | Result |
|-------|--------|
| `0` | Delegates to `generateSeed()` for a random positive seed |
| Negative (`-42`) | `Math.abs(raw)` → `42` |
| Positive (`42`) | Returned unchanged |

## Conventions

- Pure Java — no Bukkit dependency
- Seeds are always positive longs
- `resolve(0)` calls *the instance's* `generateSeed()`, not a hard-coded generator
