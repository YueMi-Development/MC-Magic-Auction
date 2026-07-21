# core-api

**Public API module** — interfaces and types that external plugins can consume without pulling in the full plugin implementation.

## Purpose

Provides the contract for MagicAuction's public-facing features. Other plugins depend on this module (via Maven or shading) to interact with the auction system without coupling to internal details.

## Build

- **Build output:** `MagicAuction-api-<version>.jar` (published to Yuemi Maven)
- **Additional artifacts:** Sources JAR, Javadoc JAR
- **Build command:** `./gradlew :core-api:build`
- **Publish:** `./gradlew :core-api:publish`
- **Javadoc:** `./gradlew :core-api:javadoc`

## Dependencies

| Dependency | Scope |
|-----------|-------|
| Paper API | compileOnly |
| YueMiLibs-api | compileOnly |

## Package

```
org.yuemi.magicauction.api
```

## Key Classes

| Class | Description |
|-------|-------------|
| `MagicAuctionApi` | Public API interface — methods for messaging, feature checks |
| `MagicAuctionApiProvider` | Entry point for consumers to get the active `MagicAuctionApi` instance (registered as a Bukkit service) |

## Conventions

- No server-specific logic — pure contracts only
- `compileOnly` Paper API (consumers provide their own)
- Versioned and published independently from the plugin
- API breaking changes require a major version bump
