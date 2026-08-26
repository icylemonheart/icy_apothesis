# Icy Apotheosis

An Apotheosis addon that forces specified entities to become boss mobs with custom rarity tiers via configuration.

Supports both **Forge 1.20.1** and **NeoForge 1.21.1**.

## Features

- Configure entities by rarity tier with weighted random assignment
- 9 tiers: Common → Uncommon → Rare → Epic → Mythic → Ancient → Guixu → Icycinder End
- Custom rarities (ordinal 6/7) with unique colors, materials, and name effects
- Auto-apotheosized boss equipment with guaranteed affixes and gem sockets
- Triple-protected creative/spectator mode target filtering
- In-game commands: `/icyapotheosis reload|list|help`
- Cross-mod compatibility (L2Hostility, KubeJS, Fallen-Gems-Affixes)

## Repository Structure

```
icy_apotheosis/
├── forge-1.20.1/          # Minecraft 1.20.1 · Forge · Java 17
│   ├── src/main/java/com/icyapotheosis/
│   ├── src/main/resources/
│   ├── data/apotheosis/rarities/
│   ├── build.gradle
│   ├── gradle.properties
│   ├── settings.gradle
│   └── gradlew / gradlew.bat
│
└── neoforge-1.21.1/       # Minecraft 1.21.1 · NeoForge · Java 21
    ├── src/main/java/com/icyapotheosis/
    ├── src/main/resources/
    ├── data/apotheosis/rarities/
    ├── build.gradle
    ├── gradle.properties
    ├── settings.gradle
    └── gradlew / gradlew.bat
```

## Usage

1. Pick the version directory matching your modloader (Forge or NeoForge)
2. Build with Gradle:
   ```bash
   ./gradlew build        # Linux/macOS
   gradlew.bat build      # Windows
   ```
3. Place `build/libs/icy_apotheosis-*.jar` into your Minecraft `mods/` folder
4. Launch the game, then edit `config/icy_apotheosis-common.toml` to configure entity tiers

## Configuration Example

```toml
# config/icy_apotheosis-common.toml

[icy_apotheosis]
# Epic tier: Warden and Wither
epicEntities = ["minecraft:warden", "minecraft:wither"]

# Mythic tier: Ender Dragon
mythicEntities = ["minecraft:ender_dragon"]

# Multiple tiers (weighted random)
guixuEntities = ["minecraft:warden|7"]
icycinderEndEntities = []

# Toggles
announceBoss = true
bossAutoAggro = true
```

## Rarity Tiers

| Tier | Ordinal | Color | Equipment Affixes |
|------|---------|------|-------------------|
| Common | 0 | - | Standard |
| Uncommon | 1 | - | Speed + Fire Resist |
| Rare | 2 | - | +20% |
| Epic | 3 | - | +40% |
| Mythic | 4 | - | +60% |
| Ancient | 5 | - | +80% |
| **Guixu** | 6 | 🔴 Red | 6 STAT / 4 ABILITY / 5 Sockets min |
| **Icycinder End** | 7 | 🔵 Ice Blue Glitch | 7 STAT / 4 ABILITY / 5 Sockets min |

## Commands

Requires OP level 2:

```
/icyapotheosis reload   # Reload configuration file
/icyapotheosis list    # Show configured entities
/icyapotheosis help    # Display help
```

## License

This project is licensed under the MIT License.
