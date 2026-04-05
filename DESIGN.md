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
