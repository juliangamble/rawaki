# Rawaki Design

## Overview

Rawaki is a Java 21 port of Orona, a browser-based top-down tank warfare game. The original is written in CoffeeScript/Node.js.

## Why Not Spring Boot

The original Orona server is ~400 lines of CoffeeScript: an HTTP server for static files, a WebSocket handler for game connections, and a 20ms game loop timer. Spring Boot's dependency injection, auto-configuration, component scanning, and ~5-second startup time add complexity with no benefit for this use case.

## Architecture

### Runtime Dependencies

| Concern | Library | Rationale |
|---|---|---|
| HTTP server | Javalin 6 | Lightweight, embedded Jetty, static file serving built-in |
| WebSocket | Javalin 6 (built-in) | Same library, no additional dependency |
| Game loop | `ScheduledExecutorService` | JDK built-in, no framework needed |
| Binary packing | `java.nio.ByteBuffer` | JDK built-in, replaces Orona's `struct.coffee` |
| JSON | Jackson (via Javalin) | Bundled with Javalin for WebSocket JSON messages |

### Test Dependencies

| Concern | Library |
|---|---|
| Test framework | JUnit 5 |
| Assertions | JUnit 5 (built-in) |
| Build tool | Maven |

### What We Don't Need

- **Spring Boot** — no DI, no auto-config, no component scanning
- **Lombok** — Java 21 records cover DTOs; game objects are mutable with explicit fields
- **Servlet API** — Javalin uses Jetty directly
- **ORM / Database** — game state is in-memory only

## Server Components

```
RawakiServer (main)
├── Javalin HTTP server
│   ├── Static file serving (index.html, css/, images/, js/, sounds/)
│   ├── /match/<id> redirect middleware
│   └── WebSocket endpoints (/demo, /match/<id>, /lobby)
├── GameLoop (ScheduledExecutorService, 20ms tick)
├── Application
│   ├── Map index (scans maps/ directory)
│   ├── Game registry (create/close games)
│   └── Demo game (auto-created on Everard Island)
└── BoloServerWorld (per game)
    ├── World objects (tanks, shells, builders, pillboxes, bases, etc.)
    ├── Client connections (WebSocket sessions)
    └── Packet serialization (binary over Base64 text frames)
```

## Package Structure

```
src/main/java/org/rawaki/
├── RawakiServer.java                   (main, Javalin setup)
├── core/
│   ├── Constants.java
│   ├── Helpers.java
│   ├── Struct.java
│   ├── SoundEffect.java               (enum)
│   ├── TeamColor.java                  (enum)
│   ├── TerrainType.java               (enum)
│   ├── map/
│   │   ├── GameMap.java
│   │   ├── MapCell.java
│   │   ├── MapView.java               (interface)
│   │   ├── MapObject.java
│   │   ├── Pillbox.java
│   │   ├── Base.java
│   │   ├── Start.java
│   │   ├── WorldMap.java
│   │   └── WorldMapCell.java
│   ├── objects/
│   │   ├── BoloObject.java            (abstract)
│   │   ├── NetWorldObject.java        (abstract)
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
    ├── Application.java
    ├── BoloServerWorld.java
    ├── GameLoop.java
    ├── MapIndex.java
    └── PacketSerializer.java

src/main/resources/
├── static/
│   ├── index.html
│   ├── css/
│   ├── images/
│   ├── js/
│   └── sounds/
└── maps/
    └── Everard Island.map
```

## Client Strategy

Serve the original Orona HTML5/JS client as static files. The browser client speaks the same WebSocket binary protocol (Base64-encoded over text frames), so it works unchanged against the Java server. No client porting needed for MVP.

## Configuration

Use a simple `config.json` file (matching Orona's format) rather than Spring's `application.properties`:

```json
{
  "general": {
    "base": "http://localhost:8124",
    "maxgames": 5
  },
  "web": {
    "port": 8124
  }
}
```

Parsed with Jackson at startup — no framework magic.

## Game Loop

```java
var executor = Executors.newSingleThreadScheduledExecutor();
executor.scheduleAtFixedRate(() -> {
    for (var game : games.values()) {
        game.tick();
    }
}, 0, Constants.TICK_LENGTH_MS, TimeUnit.MILLISECONDS);
```

Single-threaded to match Orona's model — all game state mutation happens on the loop thread. WebSocket message handlers queue commands for processing on the next tick.

## Object Reference Tracking (Ref)

Orona uses a Villain engine pattern called `ref()` to track references between game objects. For example, a shell references its owner tank, a builder references the pillbox it's carrying, and a base references the tank it's refueling.

The problem `ref()` solves: when an object is destroyed, any other objects holding references to it need to be notified so they can clean up. In CoffeeScript/Villain, this is done via event listeners on a `finalize` event, wrapped in a `{ $: target }` object.

In Rawaki, this is implemented as `BoloObject.Ref<T>` — a typed wrapper that:
- Holds a reference to another `BoloObject`
- Automatically clears itself when the target or owner emits `finalize`
- Cleans up old listeners when the reference is reassigned

```java
// CoffeeScript: @ref 'owner', tank
// Java equivalent:
private Ref<Tank> owner = ref(null);
owner.set(tank);       // track reference
owner.get();           // dereference (was .$ in CoffeeScript)
owner.clear();         // release reference
owner.set(null);       // also releases
```

This avoids memory leaks and stale references in the game object graph without requiring a garbage-collection-aware weak reference system.
