# Gun Mayhem — Java2D demo

A 2–4 player online platform-fighter prototype (physics, one-way platforms,
shooting, knockback) built with **pure JDK graphics and networking** —
`javax.swing` + `java.awt` (Java2D) over plain TCP sockets. No game engine, no
external libraries. Runs on **Windows, macOS and Linux** with only a JDK
installed.

All play is client/server: the server owns the simulation and every player runs
a client. There is no local hot-seat mode.

## Requirements

- A JDK (Java 17 or newer). Check with `java -version`.

## Build & run

Start a server, then one client per player:

```bash
make run-server                # authoritative server (prints its LAN address)
make run                       # a client: asks for the server and your nickname
```

Or without Make:

```bash
javac -d out $(find src -name '*.java')
java -cp out gunmayhem.Main server [port]                 # server (default port 5010)
java -cp out gunmayhem.Main client <host> [port] [nick]   # client
java -cp out gunmayhem.Main                               # client, asks for host/nickname
```

`make run-client HOST=<addr> NICK=<name>` skips the join screen — handy when
opening several windows for testing.

## Joining a match

1. On the host machine: `make run-server`. It prints e.g.
   `LAN address: 192.168.1.20:5010`.
2. Each player runs a client (same machine in several terminals, or different
   machines on the same Wi-Fi/LAN) and **types a nickname** on the join screen:
   ```bash
   make run                              # then enter host + nickname
   make run-client HOST=192.168.1.20     # or pass the host on the command line
   ```
3. The **lobby** shows everyone who has joined and a shared countdown:
   - with one player it just waits;
   - as soon as a **2nd** player connects a **15-second countdown** starts and is
     broadcast to every client, so all windows show the same number;
   - up to **4 players** may join while it runs; the last five seconds turn red;
   - if the lobby drops back below two players the countdown is cancelled and
     resets.
4. When the countdown hits zero the roster is frozen, the match starts and
   **every further connection is rejected** until it ends.

Knock your opponents off the stage. Each hit raises their damage %, which makes
the next hit throw them further. Three lives each; last player standing wins.
Press `R` afterwards for a rematch — with fewer than two players connected the
server drops back to the lobby and waits for more.

## Controls

Each client controls exactly one player, and either scheme works, so several
windows are playable on one keyboard for testing.

| Action | Keys |
|--------|------|
| Move | `A` / `D`  or  `←` / `→` |
| Jump (double-jump) | `W`  or  `↑` |
| Shoot | `F`  or  `L` |
| Rematch (when the match is over) | `R` |

## Design

The code is split into packages by responsibility, with dependencies flowing one
way (`core` depends on nothing else):

| Package | Contents | Depends on |
|---------|----------|------------|
| `gunmayhem`        | `Main` (entry point / mode dispatch)                       | `ui`, `net` |
| `gunmayhem.core`   | `GameWorld`, `Player`, `PlayerSpec`, `Bullet`, `Platform`, `Stage`, `Aabb`, `GameConfig`, `PlayerStyle` | — |
| `gunmayhem.render` | `Renderer`, `Java2DRenderer`                               | `core`, `net` (snapshot/lobby) |
| `gunmayhem.ui`     | `ConnectDialog` (join screen), `NetworkGamePanel`          | `core`, `render`, `net` |
| `gunmayhem.net`    | `GameServer`, `ClientHandler`, `NetworkClient`, `StateSnapshot`, `LobbyView`, `Json` | `core` |

The game core (`gunmayhem.core`) contains all physics and collision logic and
knows nothing about rendering or networking. Drawing sits behind the `Renderer`
interface (`Java2DRenderer`), which draws the two things a client can be looking
at: a `LobbyView` or a `StateSnapshot`.

### Networking (`gunmayhem.net`)

Client–server over plain TCP sockets with newline-delimited JSON (encoded by a
tiny hand-written `Json` codec — no external library):

- **`GameServer`** is authoritative and doubles as the match-maker. It runs the
  lobby (roster, capacity, the shared countdown), then owns the single
  `GameWorld`, runs the ~60 Hz loop, applies each client's input and broadcasts
  a `StateSnapshot` every tick. `ClientHandler` wraps one connected client (a
  reader thread fills a thread-safe inbox the game loop drains).
- **`NetworkClient`** connects, announces the player's nickname, and reads
  whatever the server broadcasts on a background thread. It runs **no**
  simulation.
- **`NetworkGamePanel`** is the single Swing view; it renders the lobby before
  the match and the authoritative state during it.

The server is the single source of truth, so clients can never disagree about
the game state.

Wire protocol, one JSON object per line:

| Direction | Message |
|-----------|---------|
| client → server | `{"type":"join","name":"Alice"}` (first message) |
| server → client | `{"type":"welcome","playerId":N,"name":"Alice"}` |
| server → client | `{"type":"lobby","counting":b,"secondsLeft":f,"seats":[…],"minPlayers":2,"maxPlayers":4}` |
| server → client | `{"type":"state","players":[…],"bullets":[…],"over":b,"winnerId":N}` |
| client → server | `{"type":"input","left":b,"right":b}`, `{"type":"jump"}`, `{"type":"shoot"}`, `{"type":"restart"}` |
| server → client | `{"type":"rejected","reason":"full"\|"in_progress","message":"…"}` |

Nicknames are sanitised server-side (control characters stripped, trimmed,
capped at 12 characters, falling back to `P1`…`P4` when empty), so a client
cannot inject anything odd into the wire format or the HUD.
