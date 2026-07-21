# MagicAuction

A PaperMC plugin (1.21+) that implements an exciting container-bidding auction minigame, inspired by mystery box auctions with escalating rounds.

## Statistics
<a href="https://modrinth.com/plugin/magicauction" target="_blank" rel="noopener noreferrer"><img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>

![](https://img.shields.io/bstats/players/32771?color=green&label=Players)
![](https://img.shields.io/bstats/servers/32771?color=green&label=Servers)

---

## Game Design & Auction Rounds

The core mechanic is a **container bidding game** with escalating tension:

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

### Game Rules
- **Players:** 4 per auction session
- **Rounds:** Configurable via multipliers list (default 5)
- **BIN (Buy It Now):** To win instantly, a player's bid must reach the BIN threshold: `highest opposing bid × round multiplier`. The first player to BIN wins immediately.
- **Winner:** The player who BINs takes the auction item with a reveal animation and rewards summary.
- **No winner:** If nobody BINs through all rounds, the auction ends with no winner.
- **Bonus round:** If no multiplier is set to 1.0, a bonus round with a 1.0× multiplier is forced.

### Default Round Multipliers
| Round | Overbid Multiplier | Vibe |
|-------|-------------------|------|
| 1     | 2.0× | "You *really* want it?" |
| 2     | 1.5× | Getting warmer |
| 3     | 1.3× | Tempting... |
| 4     | 1.1× | Sneaky territory |
| 5     | 1.0× | At cost — BIN or lose it |

---

## Build & Development

This project uses Java 21 and Gradle 8.13 with Kotlin DSL.

```bash
# Build all modules (produces shaded plugin JAR in core-plugin/build/libs/)
./gradlew clean build

# Build only the API module (for publishing)
./gradlew :core-api:build

# Build only the plugin shaded JAR (skips API module build)
./gradlew :core-plugin:shadowJar

# Generate Javadocs
./gradlew :core-api:javadoc
```

The output JAR will be located at `core-plugin/build/libs/MagicAuction-<version>.jar` and can be deployed directly to your PaperMC server's `plugins/` directory.

---

## Architecture & Layout

This project uses a multi-module structure to separate the API interface from the implementation:

- **[core-api](file:///f:/Github-Repository/MC-Magic-Auction/core-api)**: Provides public interfaces and constants. Other plugins can depend on this API without pulling in implementation logic.
- **[core-plugin](file:///f:/Github-Repository/MC-Magic-Auction/core-plugin)**: Contains the main plugin implementation, listeners, and shaded dependencies (including `core-api`, `mc-config-libs`, and `bStats`).
