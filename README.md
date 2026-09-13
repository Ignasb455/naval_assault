# Gun Mayhem — Java2D demo

A local 2-player platform-fighter prototype (physics, one-way platforms,
shooting, knockback) built with **pure JDK graphics** — `javax.swing` +
`java.awt` (Java2D). No game engine, no external libraries. Runs on
**Windows, macOS and Linux** with only a JDK installed.

## Requirements

- A JDK (Java 17 or newer). Check with `java -version`.

## Build & run

```bash
make run      # compiles to out/ and launches the game
```

Or without Make:

```bash
javac -d out $(find src -name '*.java')
java -cp out gunmayhem.Main
```

## Modes

```bash
make run                       # local hot-seat: 2 players share one keyboard
make run-server                # authoritative server (prints its LAN address)
make run-client HOST=<addr>    # a networked client window (default HOST=localhost)
```

Equivalent raw commands:

```bash
java -cp out gunmayhem.Main                       # local
java -cp out gunmayhem.Main server [port]         # server (default port 5010)
java -cp out gunmayhem.Main client <host> [port]  # client
```

### Testing two clients on the local network

1. On the host machine: `make run-server`. It prints e.g.
   `LAN address: 192.168.1.20:5010`.
2. Two players connect (same machine in two terminals, or two machines on the
   same Wi-Fi/LAN):
   ```bash
   make run-client HOST=192.168.1.20     # player 1
   make run-client HOST=192.168.1.20     # player 2
   ```
   The first client becomes **P1**, the second **P2**; a third is refused. The
   server owns the simulation and both windows render the same authoritative
   state.

## Controls

In **local** mode two players share the keyboard (P1 left group, P2 right group).
In **client** mode either scheme controls *your* player, so one keyboard is fine
for testing both windows.

| Action | Keys |
|--------|------|
| Move | `A` / `D`  or  `←` / `→` |
| Jump (double-jump) | `W`  or  `↑` |
| Shoot | `F`  or  `L` |
| Restart (when the match is over) | `R` |

Knock your opponent off the stage. Each hit raises their damage %, which makes
the next hit throw them further. Three lives each.

## Design

The code is split into packages by responsibility, with dependencies flowing one
way (`core` depends on nothing else):

| Package | Contents | Depends on |
|---------|----------|------------|
| `gunmayhem`        | `Main` (entry point / mode dispatch)                       | `ui`, `net` |
| `gunmayhem.core`   | `GameWorld`, `Player`, `Bullet`, `Platform`, `Stage`, `Aabb`, `GameConfig`, `PlayerStyle` | — |
| `gunmayhem.render` | `Renderer`, `Java2DRenderer`                               | `core`, `net` (snapshot) |
| `gunmayhem.ui`     | `GamePanel` (local), `NetworkGamePanel` (client)           | `core`, `render`, `net` |
| `gunmayhem.net`    | `GameServer`, `ClientHandler`, `NetworkClient`, `StateSnapshot`, `Json` | `core` |

The game core (`gunmayhem.core`) contains all physics and collision logic and
knows nothing about rendering or networking. Drawing sits behind the `Renderer`
interface (`Java2DRenderer`), which draws a `StateSnapshot` — so the *same*
renderer serves both local and networked play.

### Networking (`gunmayhem.net`)

Client–server over plain TCP sockets with newline-delimited JSON (encoded by a
tiny hand-written `Json` codec — no external library):

- **`GameServer`** is authoritative: it owns the single `GameWorld`, runs the
  ~60 Hz loop, applies each client's input and broadcasts a `StateSnapshot`
  every tick. `ClientHandler` wraps one connected client (a reader thread fills
  a thread-safe inbox the game loop drains).
- **`NetworkClient`** connects, reads the latest snapshot on a background
  thread, and sends this player's input. It runs **no** simulation.
- **`GamePanel`** (local) and **`NetworkGamePanel`** (client) are the two Swing
  views; both render through the same `Java2DRenderer`.

The server is the single source of truth, so the two clients can never
disagree about the game state.
