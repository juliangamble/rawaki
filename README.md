# Rawaki

Rawaki is a Java 21 port of [Orona](https://github.com/stephank/orona), a browser-based top-down tank warfare game originally written by Stuart Cheshire (Bolo) and rewritten by John Morrison (WinBolo).

The name comes from an uninhabited atoll in the central Pacific Ocean, near Orona.

## Status

Early development — porting core game logic from CoffeeScript to Java. See [PORTING_PLAN.md](PORTING_PLAN.md) for progress and [DESIGN.md](DESIGN.md) for architecture decisions.

## Requirements

- Java 21 or higher
- Maven 3.9+

## Build

```bash
mvn package
```

## Test

```bash
mvn test
```

## Run

```bash
java -jar target/rawaki-0.0.1-SNAPSHOT.jar config.json
```

The server will start on the port specified in `config.json` (default 8124). Open `http://localhost:8124/` in your browser to play.

## Configuration

Create a `config.json` in the project root:

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

## License

GNU GPL version 2 (inherited from WinBolo/Orona).
