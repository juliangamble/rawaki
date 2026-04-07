# Orona → Rawaki Porting Plan

Porting the Orona Bolo game from CoffeeScript/Node.js to Java 21 / Spring Boot 4.

## Architecture Overview

### Source (Orona - CoffeeScript)
- **Core**: `src/map.coffee`, `src/world_map.coffee`, `src/constants.coffee`, `src/helpers.coffee`, `src/struct.coffee`
- **Game Objects**: `src/objects/` — Tank, Shell, Builder, Explosion, Fireball, FloodFill, MineExplosion, WorldBase, WorldPillbox
- **Networking**: `src/net.coffee`, `src/server/application.coffee` — WebSocket-based client/server with binary protocol
- **Client**: `src/client/` — Browser rendering (Canvas 2D / WebGL), sound, input handling
- **Shared**: `src/object.coffee` (base class), `src/world_mixin.coffee`, `src/sounds.coffee`, `src/team_colors.coffee`

### Target (Rawaki - Java 21 / Spring Boot 4)
- **Package**: `org.rawaki`
- **Build**: Maven, Spring Boot 4.0.5, Lombok, Java 21
- **Server**: Spring WebSocket for game networking
- **Client**: Serve static HTML5/JS client via Spring (or port to JavaFX for desktop)

---

## Phase 1: Core Domain Model (No Dependencies on Networking or Rendering)

Port the pure game logic — the map, terrain, cells, and map objects. This is self-contained and testable in isolation.

### 1.1 Constants ✅
- **Source**: `src/constants.coffee`
- **Target**: `org.rawaki.core.Constants`
- **Content**: `PIXEL_SIZE_WORLD`, `TILE_SIZE_PIXELS`, `TILE_SIZE_WORLD`, `MAP_SIZE_TILES`, `MAP_SIZE_PIXELS`, `MAP_SIZE_WORLD`, `TICK_LENGTH_MS`
- **Tests**: `ConstantsTest.java` — 7 tests

### 1.2 Terrain Types ✅
- **Source**: `src/map.coffee` — `TERRAIN_TYPES` array + `TERRAIN_TYPE_ATTRIBUTES` from `src/world_map.coffee`
- **Target**: `org.rawaki.core.TerrainType` (enum)
- **Fields**: `ascii` (char), `description`, `tankSpeed`, `tankTurn`, `manSpeed`
- **Lookup**: by char and by ordinal
- **Tests**: `TerrainTypeTest.java` — 43 tests

### 1.3 Helpers ✅
- **Source**: `src/helpers.coffee`
- **Target**: `org.rawaki.core.Helpers`
- **Methods**: `distance(x1, y1, x2, y2)`, `heading(x1, y1, x2, y2)` — pure math, no dependencies
- **Note**: `extend()` not ported — Java doesn't use prototype mixins
- **Tests**: `HelpersTest.java` — 12 tests

### 1.4 Struct (Binary Packing) ✅
- **Source**: `src/struct.coffee`
- **Target**: `org.rawaki.core.BinaryPacker`
- **Content**: `Packer`, `Unpacker` (streaming), `pack()`, `unpack()` (convenience)
- **Formats**: `B` (uint8), `H` (uint16 big-endian), `I` (uint32 big-endian), `f` (bit field)
- **Note**: Uses `ByteArrayOutputStream` for packing, `byte[]` with offset for unpacking
- **Tests**: `BinaryPackerTest.java` — 26 tests

### 1.5 MapCell ✅
- **Source**: `src/map.coffee` — `MapCell` class
- **Target**: `org.rawaki.core.map.MapCell`
- **Key methods**: `neigh()`, `isType()` (by TerrainType and by ascii char), `isEdgeCell()`, `getNumericType()`, `setType()` (by TerrainType, char, int), `retile()` (stub)
- **Note**: Also created minimal `GameMap` (cellAtTile, retile) needed by MapCell. Full GameMap implementation in Phase 1.6.
- **Deferred**: Retile rendering methods (`retileDeepSea`, `retileBuilding`, `retileRiver`, `retileRoad`, `retileForest`, `retileBoat`, `setTile`) — these depend on the view/rendering system and will be ported in Phase 6 (Client).
- **Tests**: `MapCellTest.java` — 32 tests

### 1.6 Map ✅
- **Source**: `src/map.coffee` — `Map` class, `MapObject`, `Pillbox`, `Base`, `Start`
- **Target**: `org.rawaki.core.map.GameMap`, `Pillbox` (record), `Base` (record), `Start` (record)
- **Key methods**: `cellAtTile()`, `each()`, `clear()`, `retile()`, `findCenterCell()`, `dump()` (BMAP binary format with options), `load()` (static factory)
- **Note**: Map objects are Java records. `dump()` supports `noPills`, `noBases`, `noStarts` flags.
- **Tests**: `GameMapTest.java` — 23 tests

### 1.7 Sounds (enum) ✅
- **Source**: `src/sounds.coffee`
- **Target**: `org.rawaki.core.SoundEffect` (enum)
- **Content**: 12 sound effect IDs — ordinals match network protocol IDs
- **Tests**: `SoundEffectTest.java` — 2 tests

### 1.8 TeamColors ✅
- **Source**: `src/team_colors.coffee`
- **Target**: `org.rawaki.core.TeamColor` (enum)
- **Content**: 6 team colors with RGB values (red, blue, green, cyan, yellow, magenta)
- **Tests**: `TeamColorTest.java` — 13 tests

### 1.9 Tests for Phase 1 ✅
- Port the 131 existing map/tile Mocha tests from `test/map.test.js` and `test/map-extended.test.js` to JUnit 5
- Port the 17 helpers tests from `test/helpers.test.js`
- Port the 15 net protocol tests from `test/net.test.js`
- Test BMAP load/dump round-trip
- Test terrain type lookups
- Test cell retiling logic
- Test `findCenterCell()`
- **Result**: 159 JUnit tests across 8 test classes, all passing
- **Note**: Revisit at the end of the port to ensure full parity with the 393 Orona Mocha tests

---

## Phase 2: World Map & Game Object Base

Extend the core map with game-specific logic and establish the base class for world objects.

### 2.1 WorldMapCell ✅
- **Source**: `src/world_map.coffee` — `WorldMapCell`
- **Target**: `org.rawaki.core.map.WorldMapCell extends MapCell`
- **Key methods**: `isObstacle()`, `hasTankOnBoat()`, `getTankSpeed()`, `getTankTurn()`, `getManSpeed()`, `getPixelCoordinates()`, `getWorldCoordinates()`, `takeShellHit()`, `takeExplosionHit()`
- **Note**: `setType()` override adds `life` tracking. Defines `PillLike`, `BaseLike`, `TankLike`, `ManLike` interfaces to avoid circular dependencies with game object package.
- **Tests**: `WorldMapCellTest.java` — 38 tests

### 2.2 WorldMap ✅
- **Source**: `src/world_map.coffee` — `WorldMap`
- **Target**: `org.rawaki.core.map.WorldMap extends GameMap`
- **Key methods**: `cellAtPixel()`, `cellAtWorld()`, `getRandomStart()`
- **Note**: Overrides `createCell()` to produce `WorldMapCell` instances. Holds a `world` back-reference.
- **Tests**: `WorldMapTest.java` — 10 tests

### 2.3 BoloObject (Base) ✅
- **Source**: `src/object.coffee` + `villain/world/object.coffee` + `villain/world/net/object.coffee`
- **Target**: `org.rawaki.core.objects.BoloObject` (abstract class)
- **Fields**: `world`, `idx`, `updatePriority`, `styled`, `team`, `x`, `y`
- **Methods**: `spawn()`, `update()`, `destroy()`, `anySpawn()`, `soundEffect()`, `getTile()`
- **Events**: Simple `on()`/`emit()`/`removeListener()` system replacing Node's EventEmitter
- **Note**: Collapses Villain's `WorldObject` → `NetWorldObject` → `BoloObject` into a single class. Also created `World` interface (`org.rawaki.core.world.World`).
- **Tests**: `BoloObjectTest.java` — 22 tests

### 2.4 NetWorldObject Equivalent ✅
- **Target**: `BoloObject.Ref<T>` (inner class of `org.rawaki.core.objects.BoloObject`)
- **Responsibilities**: Tracked object references with auto-cleanup on `finalize` event — replaces Villain's `ref()` system
- **Methods**: `get()`, `set()`, `clear()`, `isPresent()`
- **Note**: Merged into BoloObject rather than a separate class. The `emit()` method uses `List.copyOf()` to avoid ConcurrentModificationException when listeners modify the list during iteration.
- **Tests**: Added 7 Ref tests to `BoloObjectTest.java` (total now 29)

### 2.5 WorldMixin ✅
- **Source**: `src/world_mixin.coffee`
- **Target**: `org.rawaki.core.world.BoloWorldMixin` (interface with default methods, extends `World`)
- **Methods**: `addTank()`, `removeTank()`, `getAllMapObjects()`, `spawnMapObjects()`, `resolveMapObjectOwners()`
- **Note**: `resolveMapObjectOwners()` and `getAllMapObjects()` are overridable defaults — concrete world implementations provide the typed map object lists.
- **Tests**: `BoloWorldMixinTest.java` — 14 tests

---

## Phase 3: Game Objects

Port each game object. These all extend BoloObject and implement `serialization()`, `spawn()`, `update()`, `destroy()`.

### 3.1 WorldPillbox ✅
- **Source**: `src/objects/world_pillbox.coffee`
- **Target**: `org.rawaki.core.objects.WorldPillbox`
- **Logic**: Placement, cell management, owner tracking, aggravation, shell/explosion damage, repair, cooldown/reload cycle
- **Implements**: `WorldMapCell.PillLike`
- **Deferred**: AI targeting with lead calculation and shell spawning — requires Tank and Shell classes (Phase 3.3, 3.4)
- **Tests**: `WorldPillboxTest.java` — 31 tests

### 3.2 WorldBase ✅
- **Source**: `src/objects/world_base.coffee`
- **Target**: `org.rawaki.core.objects.WorldBase`
- **Logic**: Owner tracking, refueling tanks (armour → shells → mines priority), shell damage
- **Implements**: `WorldMapCell.BaseLike`
- **Defines**: `RefuelTarget` interface for typed tank interaction during refueling
- **Deferred**: `findSubject()` tank discovery and `takeShellHit()` pillbox aggravation — requires typed tank/pill lists (Phase 3.3)
- **Tests**: `WorldBaseTest.java` — 17 tests

### 3.3 Tank
- **Source**: `src/objects/tank.coffee`
- **Target**: `org.rawaki.core.objects.Tank`
- **Logic**: Movement (acceleration, turning, sliding), shooting, boat mechanics, collision detection, death/respawn, pillbox dropping
- **Note**: Largest object — ~300 lines of game logic. Port method by method.

### 3.4 Shell
- **Source**: `src/objects/shell.coffee`
- **Target**: `org.rawaki.core.objects.Shell`
- **Logic**: Movement, collision detection (pillbox, tank, base, terrain), explosion on impact

### 3.5 Builder
- **Source**: `src/objects/builder.coffee`
- **Target**: `org.rawaki.core.objects.Builder`
- **Logic**: State machine (inTank, waiting, returning, parachuting, actions), building actions (forest, road, repair, boat, building, pillbox, mine), pathfinding to target

### 3.6 Explosion
- **Source**: `src/objects/explosion.coffee`
- **Target**: `org.rawaki.core.objects.Explosion`
- **Logic**: Simple animation timer with tile selection

### 3.7 Fireball
- **Source**: `src/objects/fireball.coffee`
- **Target**: `org.rawaki.core.objects.Fireball`
- **Logic**: Moving trail of fire from dying tank, terrain destruction, final explosion

### 3.8 FloodFill
- **Source**: `src/objects/flood_fill.coffee`
- **Target**: `org.rawaki.core.objects.FloodFill`
- **Logic**: Delayed flooding of craters adjacent to water

### 3.9 MineExplosion
- **Source**: `src/objects/mine_explosion.coffee`
- **Target**: `org.rawaki.core.objects.MineExplosion`
- **Logic**: Delayed mine detonation, chain reaction spreading to neighbours

### 3.10 Object Registry
- **Source**: `src/objects/all.coffee`
- **Target**: `org.rawaki.core.objects.ObjectRegistry`
- **Content**: Register all object types with the world (for network serialization type indices)

### 3.11 Tests for Phase 3
- Port the existing Mocha tests to JUnit 5 (393 total tests available as reference):
  - `test/tank.test.js` — 54 tests: reset, direction, range, allies, combat, turning, acceleration, shooting
  - `test/builder.test.js` — 45 tests: states, performOrder, kill, reached, build actions, update
  - `test/shell.test.js` — 33 tests: direction, move, collision detection, asplode, update
  - `test/world_pillbox.test.js` — 40 tests: aggravate, takeShellHit, repair, update/targeting
  - `test/world_base.test.js` — 25 tests: refueling, findSubject, takeShellHit
  - `test/world_mixin.test.js` — 19 tests: addTank, removeTank, spawnMapObjects, resolveMapObjectOwners
- Integration test: spawn tank, fire shell, verify terrain damage
- Integration test: builder builds road, building, places pillbox

---

## Phase 4: World & Game Loop

### 4.1 World Interface
- **Target**: `org.rawaki.core.world.World` (interface)
- **Methods**: `spawn()`, `destroy()`, `insert()`, `tick()`, `soundEffect()`, `mapChanged()`
- **Fields**: `map`, `tanks`, `objects`, `authority`

### 4.2 ServerWorld
- **Source**: `src/server/application.coffee` — `BoloServerWorld`
- **Target**: `org.rawaki.server.BoloServerWorld implements World`
- **Logic**: Game tick loop, object lifecycle, change tracking, packet generation
- **Note**: The Villain engine's `ServerWorld` handles object indexing, creation/destruction tracking, and serialization. Reimplement this in Java.

### 4.3 Game Loop
- **Source**: Villain's `createLoop` + `TICK_LENGTH_MS` (20ms ticks = 50 FPS)
- **Target**: `org.rawaki.server.GameLoop` — use `ScheduledExecutorService` or Spring's `@Scheduled`

### 4.4 Tests for Phase 4
- Test full game tick cycle
- Test object spawn/destroy lifecycle
- Test change tracking

---

## Phase 5: Networking

### 5.1 Network Protocol
- **Source**: `src/net.coffee`
- **Target**: `org.rawaki.net.NetProtocol`
- **Content**: Message type constants, client command constants

### 5.2 WebSocket Server
- **Source**: `src/server/application.coffee` — connection handling
- **Target**: `org.rawaki.server.GameWebSocketHandler extends TextWebSocketHandler`
- **Dependencies**: `spring-boot-starter-websocket`
- **Logic**: Handle connect, message routing (JSON commands + binary game commands), disconnect

### 5.3 Packet Serialization
- **Source**: `src/server/application.coffee` — `changesPacket()`, `updatePacket()`, `sendPackets()`
- **Target**: `org.rawaki.server.PacketSerializer`
- **Format**: Binary packets encoded as Base64 over WebSocket text frames (matching original protocol)

### 5.4 Map Index
- **Source**: `src/server/map_index.coffee`
- **Target**: `org.rawaki.server.MapIndex`
- **Logic**: Scan maps directory, fuzzy name matching

### 5.5 Application / Configuration
- **Source**: `src/server/application.coffee` — `Application` class, `src/server/command.coffee`
- **Target**: `org.rawaki.server.GameServerConfig` (Spring `@Configuration`)
- **Content**: WebSocket config, game creation, static file serving

---

## Phase 6: Client

### Option A: Serve Original HTML5 Client (Recommended for MVP)
- Copy `index.html`, `css/`, `images/`, `sounds/`, `js/` into `src/main/resources/static/`
- The existing JS client speaks the same WebSocket protocol
- Minimal changes needed — just update WebSocket URL if needed

### Option B: Port Client to JavaFX (Desktop)
- Only if a desktop client is desired
- Would require porting all rendering (`src/client/renderer/`), input handling, and sound

### 6.1 Everard Island Map
- **Source**: `src/client/everard.coffee` (Base64-encoded BMAP)
- **Target**: `src/main/resources/maps/Everard Island.map` (binary file) or keep as Base64 string constant
- **Note**: The binary `.map` file already exists at `maps/Everard Island.map` in Orona

---

## Phase 7: Polish & Parity

### 7.1 Visual Integration Tests
- Port the 9 visual test scenarios from `test/visual/` if desired
- Use Java2D / `BufferedImage` for headless tile rendering

### 7.2 Configuration
- Use `application.properties` / `application.yml` instead of `config.json`
- Properties: `game.base-url`, `game.max-games`, `server.port`

### 7.3 Static Assets
- Copy from Orona: `images/`, `sounds/`, `css/`, `maps/`, `index.html`
- Place in `src/main/resources/static/`

---

## Target Package Structure

```
src/main/java/org/rawaki/
├── RawakiApplication.java              (existing Spring Boot entry point)
├── core/
│   ├── Constants.java
│   ├── Helpers.java
│   ├── Struct.java
│   ├── SoundEffect.java                (enum)
│   ├── TeamColor.java                  (enum)
│   ├── TerrainType.java                (enum)
│   ├── map/
│   │   ├── GameMap.java
│   │   ├── MapCell.java
│   │   ├── MapView.java                (interface)
│   │   ├── MapObject.java
│   │   ├── Pillbox.java
│   │   ├── Base.java
│   │   ├── Start.java
│   │   ├── WorldMap.java
│   │   └── WorldMapCell.java
│   ├── objects/
│   │   ├── BoloObject.java             (abstract)
│   │   ├── NetWorldObject.java         (abstract)
│   │   ├── ObjectRegistry.java
│   │   ├── Tank.java
│   │   ├── Shell.java
│   │   ├── Builder.java
│   │   ├── Explosion.java
│   │   ├── Fireball.java
│   │   ├── FloodFill.java
│   │   ├── MineExplosion.java
│   │   ├── WorldBase.java
│   │   └── WorldPillbox.java
│   └── world/
│       ├── World.java                  (interface)
│       └── BoloWorldMixin.java
├── net/
│   └── NetProtocol.java
└── server/
    ├── BoloServerWorld.java
    ├── GameLoop.java
    ├── GameServerConfig.java
    ├── GameWebSocketHandler.java
    ├── MapIndex.java
    └── PacketSerializer.java

src/main/resources/
├── application.properties
├── static/
│   ├── index.html
│   ├── css/
│   ├── images/
│   ├── js/
│   └── sounds/
└── maps/
    └── Everard Island.map

src/test/java/org/rawaki/
├── RawakiApplicationTests.java         (existing)
├── core/
│   ├── HelpersTest.java                (port 17 tests from test/helpers.test.js)
│   ├── map/
│   │   ├── GameMapTest.java            (port 131 map tests from test/map.test.js + test/map-extended.test.js)
│   │   ├── MapCellTest.java
│   │   └── WorldMapCellTest.java
│   ├── objects/
│   │   ├── TankTest.java               (port 54 tests from test/tank.test.js)
│   │   ├── ShellTest.java              (port 33 tests from test/shell.test.js)
│   │   ├── BuilderTest.java            (port 45 tests from test/builder.test.js)
│   │   ├── WorldPillboxTest.java       (port 40 tests from test/world_pillbox.test.js)
│   │   └── WorldBaseTest.java          (port 25 tests from test/world_base.test.js)
│   └── world/
│       └── BoloWorldMixinTest.java     (port 19 tests from test/world_mixin.test.js)
├── net/
│   └── NetProtocolTest.java            (port 15 tests from test/net.test.js)
└── server/
    ├── BoloServerWorldTest.java
    ├── MapIndexTest.java               (port 10 tests from test/map_index.test.js)
    └── CommandTest.java                (port 4 tests from test/command.test.js)
```

---

## Maven Dependencies to Add

```xml
<!-- WebSocket support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Serve static content -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## Key CoffeeScript → Java Translation Notes

| CoffeeScript Pattern | Java Equivalent |
|---|---|
| `exports.X = value` | `public static final` or enum |
| `class X extends Y` | `class X extends Y` |
| Mixins via `helpers.extend(proto, mixin)` | Interface with default methods, or composition |
| `@field` (instance var) | Private field + getter/setter (or Lombok `@Data`) |
| `=>` (bound function) | Lambda or method reference |
| `for x in array when condition` | Stream filter + forEach |
| `x?` (existential) | `x != null` |
| `x?.method()` | `Optional` or null check |
| Splat args `(args...)` | Varargs |
| Destructuring `[a, b] = array` | Explicit index access |
| `ref('owner', tank)` (Villain) | Direct field assignment + event notification |
| Event system `@on`, `@emit` | Java `EventListener` pattern or simple callback lists |
| Binary packing (`struct.coffee`) | `java.nio.ByteBuffer` |

---

## Execution Order Summary

1. **Phase 1** — Core domain (Constants, TerrainType, MapCell, GameMap, Helpers, Struct) + tests
2. **Phase 2** — WorldMapCell, WorldMap, BoloObject base, WorldMixin
3. **Phase 3** — All 9 game objects (Tank, Shell, Builder, etc.) + tests
4. **Phase 4** — World interface, ServerWorld, game loop
5. **Phase 5** — WebSocket networking, protocol, packet serialization
6. **Phase 6** — Static client assets, Everard Island map, configuration
7. **Phase 7** — Visual tests, polish, full parity verification

Each phase is independently testable. Phase 1 alone gives you a working BMAP parser and map model.
