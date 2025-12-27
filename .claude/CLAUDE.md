# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Oraxen is a Minecraft Spigot/Paper plugin that allows server administrators to add custom items, weapons, blocks, furniture, and more. It automatically generates resource packs from configuration files, simplifying the work for administrators. The plugin includes an extensive API for developers to extend functionality.

## Build Commands

### Building the Plugin
```bash
# Build the plugin (creates shadowed jar with all NMS versions)
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean build
./gradlew clean build

# Run a test server (Paper 1.21.11)
./gradlew runServer
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:test

# Run PackMerger debug tool
./gradlew :core:runPackMergerDebug --args="path/to/pack.zip"
```

### Development Workflow
```bash
# Build and copy to plugin directory (if oraxen_spigot_plugin_path is set in gradle.properties)
./gradlew build

# The build automatically copies to the configured plugin path and removes old versions
```

## Architecture

### Multi-Version NMS Support

Oraxen uses a multi-module architecture to support multiple Minecraft versions:
- **core**: Main plugin logic (io.th0rgal.oraxen.*)
- **v1_20_R1** through **v1_21_R6**: Version-specific NMS implementations
- **v1_21_R6_spigot**: Spigot-specific implementation for 1.21.11 (separate from Paper)

Each NMS module:
- Contains version-specific packet and entity handling code
- Uses paperweight for remapping (most versions)
- Gets embedded into the final jar via the reobf configuration
- Is registered in SUPPORTED_VERSIONS list in build.gradle.kts

**Paper vs Spigot Mapping Difference (1.21.11+)**:
- Paper 1.21.11+ uses Mojang mappings and no longer provides reobf mappings
- Spigot 1.21.11 uses Spigot mappings (obfuscated class names like `NBTTagCompound` instead of `CompoundTag`)
- This requires separate modules: `v1_21_R6` (Paper, Mojang-mapped) and `v1_21_R6_spigot` (Spigot-mapped)
- Runtime detection in `VersionUtil.NMSVersion.matchesServer()` chooses the correct module

When adding NMS-dependent code:
1. Define an interface in core (e.g., in io.th0rgal.oraxen.nms)
2. Implement it in each version module
3. Load the correct implementation at runtime based on server version

### Resource Pack Generation

The resource pack system (io.th0rgal.oraxen.pack) automatically generates Minecraft resource packs:
- **pack/generation**: Builds packs from configuration and assets
- **pack/upload**: Handles automatic upload to hosting services
- Resources are in core/src/main/resources/pack/ (models, textures, fonts, sounds)

The pack includes:
- Custom item models and textures (core/src/main/resources/pack/models, textures)
- Font glyphs for chat and UI (core/src/main/resources/glyphs/)
- Sound definitions (sound.yml)

### Mechanics System

Mechanics are modular features that can be attached to items (io.th0rgal.oraxen.mechanics):

1. **MechanicFactory**: Creates and manages mechanic instances for items
2. **Mechanic**: Item-specific mechanic instance (stores config for one item)
3. Registered in MechanicsManager.registerNativeMechanics()

Categories:
- **misc**: armor_effects, soulbound, consumable, commands, backpack, music_disc
- **gameplay**: durability, efficiency, block, noteblock, stringblock, furniture, repair
- **farming**: harvesting, smelting, watering, bigmining, bedrockbreak, bottledexp
- **combat**: lifeleech, bleeding, thor, energyblast, fireball, witherskull, spear
- **cosmetic**: hat, skin, skinnable, aura

To add a new mechanic:
1. Create XMechanic extending Mechanic
2. Create XMechanicFactory extending MechanicFactory
3. Create XMechanicListener if needed
4. Register in MechanicsManager.registerNativeMechanics()

### API Structure

Primary API classes (io.th0rgal.oraxen.api):
- **OraxenItems**: Item creation, retrieval, and identification
- **OraxenBlocks**: Custom block placement, breaking, and queries
- **OraxenFurniture**: Furniture entity management
- **OraxenPack**: Resource pack operations

Events in io.th0rgal.oraxen.api.events allow listening to plugin lifecycle and item operations.

### Configuration System

Configuration is managed through:
- **ConfigsManager**: Loads and manages all config files
- **ResourcesManager**: Extracts default configs from jar
- **Settings**: Type-safe settings access (from settings.yml)
- **Message**: Localized messages (from languages/)

Config files in core/src/main/resources:
- settings.yml: Main plugin configuration
- mechanics.yml: Mechanic-specific settings
- font.yml, hud.yml, sound.yml: Asset configuration
- items/: Item definitions
- recipes/: Crafting recipes

### Packet Adapters

Oraxen supports both ProtocolLib and PacketEvents:
- **PacketAdapter**: Abstract interface for packet operations
- **ProtocolLibAdapter**: Implementation using ProtocolLib
- **PacketEventsAdapter**: Implementation using PacketEvents
- Plugin detects available library at runtime and uses appropriate adapter

### Dependency Management

Dependencies are managed via gradle/oraxenLibs.versions.toml:
- **libraries-bukkit**: Loaded via plugin.yml libraries (Adventure, Gson, etc.)
- **libraries-shade**: Shaded and relocated (BStats, ProtectionLib, InventoryFramework)
- **plugins**: Soft dependencies (ProtocolLib, PlaceholderAPI, MythicMobs, etc.)

All shaded dependencies are relocated to io.th0rgal.oraxen.shaded.* to avoid conflicts.

### Compatibilities

Integration with third-party plugins (io.th0rgal.oraxen.compatibilities.provided):
- Each compatibility has a provider class that checks if the plugin is available
- Wrapped classes (e.g., WrappedMMOItem) provide type-safe access to external APIs
- CompatibilitiesManager loads all compatible plugins at runtime

Supported: MythicMobs, MythicCrucible, MMOItems, WorldEdit, PlaceholderAPI, EcoItems, BlockLocker, BossShopPro, Skript, ModelEngine

## Key Paths

| Purpose | Location |
|---------|----------|
| Plugin entry | `core/src/main/java/io/th0rgal/oraxen/OraxenPlugin.java` |
| NMS interface | `core/src/main/java/io/th0rgal/oraxen/nms/NMSHandler.java` |
| NMS version detection | `core/src/main/java/io/th0rgal/oraxen/utils/VersionUtil.java` |
| NMS handler loading | `core/src/main/java/io/th0rgal/oraxen/nms/NMSHandlers.java` |
| Pack generation | `core/src/main/java/io/th0rgal/oraxen/pack/generation/ResourcePack.java` |
| Mechanics | `core/src/main/java/io/th0rgal/oraxen/mechanics/provided/` |
| Commands | `core/src/main/java/io/th0rgal/oraxen/commands/` |
| Compatibility | `core/src/main/java/io/th0rgal/oraxen/compatibilities/provided/` |
| Item configs | `core/src/main/resources/items/` |
| YAML resources | `core/src/main/resources/` (glyphs/, recipes/, languages/, pack/, hud.yml, mechanics.yml) |
| Spigot NMS generator | `scripts/generate-spigot-nms.py` |

## Gradle Properties

Set in gradle.properties for development:
- `oraxen_spigot_plugin_path`: Auto-copy built jar to this directory
- `oraxen_dev_plugin_path`, `oraxen_folia_plugin_path`: Alternative plugin paths
- `oraxen_compiled`: Whether this is a compiled/production build

## Coding Conventions

### Java Style
- **Language**: Java 21 via Gradle toolchain
- **Naming**: Classes `PascalCase`, methods/fields `camelCase`, constants `SCREAMING_SNAKE_CASE`
- **Nullability**: Use `@NotNull`/`@Nullable` (org.jetbrains) on public APIs and NMS boundaries
- **Logging**: Use `io.th0rgal.oraxen.utils.logs.Logs` for console, `Message` enum for user strings
- **Exceptions**: Fail fast with guard clauses; log via `Logs`, don't swallow exceptions
- **Imports**: Keep explicit (no wildcards)
- **Text Components**: Use Adventure for text components (not legacy chat)

### Code Organization
- Public APIs and shared logic in `core`
- Server-version-specific code in `v1_xx_Ry` modules
- Match existing code style and formatting

### Over-Engineering Avoidance
- Only make changes directly requested
- Keep solutions simple and focused
- Don't add features, refactor code, or make "improvements" beyond what was asked
- Don't create abstractions for one-time operations
- Trust internal code and framework guarantees

## Adding Support for New MC Versions

### Standard Version (Mojang-mapped, Paper 1.21.11+ style)
This is the current standard for new Minecraft versions. Paper uses Mojang mappings and no longer provides reobf mappings.

**Paper Module** (e.g., `v1_xx_Ry`):
1. Create module directory with `build.gradle.kts`:
   ```kotlin
   plugins {
       id("java")
       id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
       id("io.github.goooler.shadow") version "8.1.8"
   }
   dependencies {
       compileOnly(project(":core"))
       paperweight.paperDevBundle("X.XX.X-R0.1-SNAPSHOT")
   }
   // Note: reobfJar task is NOT used - code ships Mojang-mapped
   ```
2. Implement `NMSHandler` and `GlyphHandler` using Mojang class names
3. Update `SUPPORTED_VERSIONS` in root `build.gradle.kts`
4. Add module to `settings.gradle.kts`
5. Add version mapping in `VersionUtil.java` versionMap

**Spigot Module** (e.g., `v1_xx_Ry_spigot`):
1. Create separate module with `build.gradle.kts`:
   ```kotlin
   plugins {
       id("java")
       id("io.github.goooler.shadow") version "8.1.8"
   }
   repositories {
       mavenLocal() // For Spigot BuildTools artifacts
   }
   dependencies {
       compileOnly(project(":core"))
       compileOnly("org.spigotmc:spigot:X.XX.X-R0.1-SNAPSHOT")
   }
   tasks {
       register("reobfJar") { dependsOn(jar) } // No-op for compatibility
   }
   ```
2. Requires running Spigot BuildTools first: `java -jar BuildTools.jar --rev X.XX.X`
3. Implement using Spigot class names (see mapping table below)
4. Provide simplified/stub implementations for Paper-only features
5. Update `VersionUtil.NMSVersion` enum with both variants
6. Update `matchesServer()` logic to detect Paper vs Spigot at runtime

### Legacy Version (Paper with reobf support, pre-1.21.11)
For older versions where Paper provides reobf mappings (v1_20_R1 through v1_21_R5):

1. Create `v1_xx_Ry` module with paperweight
2. Code is written in Mojang mappings, paperweight remaps to Spigot mappings via `reobfJar`
3. Single module works for both Paper and Spigot
4. No separate Spigot module needed

### Spigot Class Name Mappings (1.21.11)
Key Mojang -> Spigot class name differences:
- `CompoundTag` -> `NBTTagCompound`
- `ListTag` -> `NBTTagList`
- `StringTag` -> `NBTTagString`
- `Tag` -> `NBTBase`
- `ServerPlayer` -> `EntityPlayer`
- `ServerLevel` -> `WorldServer`
- `BlockPos` -> `BlockPosition`
- `FriendlyByteBuf` -> `PacketDataSerializer`
- `Connection` -> `NetworkManager`
- `ResourceLocation` -> `MinecraftKey`
- `InteractionHand` -> `EnumHand`
- `InteractionResult` -> `EnumInteractionResult`
- CraftBukkit packages: `org.bukkit.craftbukkit.v1_21_R7.*` (version varies)

A script `scripts/generate-spigot-nms.py` exists to help transform source files, but manual review is needed.

### Spigot Module Limitations
The Spigot module (`v1_21_R6_spigot`) provides limited functionality:
- No GlobalConfiguration access (block update settings always return false)
- No ChannelInitializeListenerHolder (packet injection for glyphs not supported)
- No advanced consumable component configuration
- No jukebox song playing via NMS
- Warnings are logged recommending Paper for full feature support

## Important Notes

- The main plugin class is io.th0rgal.oraxen.OraxenPlugin
- NMS handlers are initialized in io.th0rgal.oraxen.nms.NMSHandlers and GlyphHandlers
- Item builders use creative-api for resource pack generation
- Custom block data uses Jeff Media's CustomBlockData library
- Folia support is enabled
- Paper API version: 1.18+

## Security

- Never paste secrets into code, docs, or commits
- Wire code to read from environment variables
- Validate external input at boundaries

## Deployment

### Prerequisites

1. Copy `~/minecraft/secrets.json` to project root (already in `.gitignore`)
2. SSH key at `~/.ssh/cursor` with access to the dedicated server

### Deploy to Production Server

```bash
# Build the plugin
./gradlew build -x test

# Deploy (using secrets.json)
HOST="$(jq -r '.servers.test_server.ssh.host' secrets.json)"
USER="$(jq -r '.servers.test_server.ssh.user' secrets.json)"
PORT="$(jq -r '.servers.test_server.ssh.port' secrets.json)"
KEY="$(jq -r '.servers.dedicated.ssh.identity_file' secrets.json)"
PLUGINS_DIR="$(jq -r '.servers.test_server.paths.plugins_dir' secrets.json)"
JAR="build/libs/oraxen-*.jar"

rsync -avP -e "ssh -i ${KEY/#\~/$HOME} -p ${PORT}" $JAR "${USER}@${HOST}:${PLUGINS_DIR}/"

# Restart server
UNIT="$(jq -r '.servers.test_server.systemd.unit' secrets.json)"
ssh -i "${KEY/#\~/$HOME}" -p "$PORT" "$USER@$HOST" "systemctl restart $UNIT"
```

### Server Details

- **Host**: Dedicated VPS (see `secrets.json` for IP)
- **Minecraft**: Paper 1.21.4 at `/root/minecraft/paper-1.21`
- **Systemd unit**: `minecraft-test.service`
- **Plugins dir**: `/root/minecraft/paper-1.21/plugins/`
