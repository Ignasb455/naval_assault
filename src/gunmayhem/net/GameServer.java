package gunmayhem.net;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.PlayerSpec;
import gunmayhem.core.gamemodes.GameMode;
import gunmayhem.core.gamemodes.LastManStanding;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative game server and match-maker. It is the only place the game is
 * simulated — there is no local play mode.
 *
 * <p>A session runs through two phases:
 * <ol>
 *   <li><b>Lobby</b> — clients connect and send the nickname their player chose.
 *       As soon as {@link GameConfig#MIN_PLAYERS} are connected a
 *       {@link GameConfig#LOBBY_COUNTDOWN_SECONDS}-second countdown starts and
 *       is broadcast to everyone, so all clients see the same clock. Late
 *       joiners are welcome while it runs, up to {@link GameConfig#MAX_PLAYERS}.
 *       If the lobby drops back below the minimum the countdown is cancelled.</li>
 *   <li><b>Running</b> — the countdown hit zero: the roster is frozen, the world
 *       is built and every further connection is rejected. The server applies
 *       each client's input, steps the simulation at ~60 Hz and broadcasts a
 *       {@link StateSnapshot} each tick.</li>
 * </ol>
 *
 * <p>Wire protocol (one JSON object per line):
 * <ul>
 *   <li>client &rarr; server on connect: {@code {"type":"join","name":"Alice"}}</li>
 *   <li>server &rarr; client: {@code {"type":"welcome","playerId":N,"name":"Alice"}}</li>
 *   <li>server &rarr; client each tick: a {@link LobbyView} or a {@link StateSnapshot}</li>
 *   <li>client &rarr; server: {@code {"type":"input","left":b,"right":b}},
 *       {@code {"type":"jump"}}, {@code {"type":"shoot"}}, {@code {"type":"restart"}}</li>
 *   <li>server &rarr; client when refused: {@code {"type":"rejected","reason":...}}</li>
 * </ul>
 */
public final class GameServer {
    public static final int DEFAULT_PORT = 5010;

    private enum Phase { LOBBY, RUNNING }

    private final int port;
    private final GameMode gameMode;

    /** Connected clients. Every field below is guarded by this list's monitor. */
    private final List<ClientHandler> clients = new ArrayList<>();

    private Phase phase = Phase.LOBBY;
    private GameWorld world;                  // null while in the lobby
    private boolean counting;
    private float countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
    private boolean restartRequested;

    private volatile boolean running = true;

    public GameServer(int port) {
        this(port, new LastManStanding());
    }

    public GameServer(int port, GameMode gameMode) {
        this.port = port;
        if (gameMode == null) {
            throw new IllegalArgumentException("gameMode must not be null");
        }
        this.gameMode = gameMode;
    }

    public void run() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Gun Mayhem server listening on port " + port);
            System.out.println("LAN address: " + lanHint() + ":" + port
                + "   (clients run:  java -cp out gunmayhem.Main client <that-address>)");
            System.out.println("Waiting for players: the match starts "
                + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + "s after the "
                + GameConfig.MIN_PLAYERS + "nd one connects (max "
                + GameConfig.MAX_PLAYERS + ").");

            Thread loop = new Thread(this::gameLoop, "game-loop");
            loop.setDaemon(true);
            loop.start();

            acceptLoop(serverSocket);
        }
    }

    // ---- Accepting connections ---------------------------------------------

    private void acceptLoop(ServerSocket serverSocket) throws IOException {
        while (running) {
            Socket socket = serverSocket.accept();
            synchronized (clients) {
                if (phase != Phase.LOBBY) {
                    reject(socket, "in_progress", "A match is already in progress.");
                } else if (clients.size() >= GameConfig.MAX_PLAYERS) {
                    reject(socket, "full", "The lobby is full ("
                        + GameConfig.MAX_PLAYERS + " players).");
                } else {
                    admit(socket);
                }
            }
        }
    }

    /** Caller must hold the {@code clients} lock. */
    private void admit(Socket socket) {
        int playerId = freeSlot();
        try {
            ClientHandler handler = new ClientHandler(socket, playerId);
            clients.add(handler);
            handler.startReading();
            System.out.println("Client connected in slot " + playerId
                + " from " + handler.remoteAddress()
                + "  (" + clients.size() + "/" + GameConfig.MAX_PLAYERS + ")");
        } catch (IOException e) {
            System.out.println("Failed to set up client: " + e.getMessage());
        }
    }

    /**
     * Lowest id not currently held by a connected client. Reusing
     * {@code clients.size()} would hand out an id that a surviving client
     * already owns once somebody in the middle disconnects.
     *
     * <p>Caller must hold the {@code clients} lock.
     */
    private int freeSlot() {
        for (int id = 0; id < GameConfig.MAX_PLAYERS; id++) {
            boolean taken = false;
            for (ClientHandler c : clients) {
                if (c.playerId() == id) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return id;
            }
        }
        return GameConfig.MAX_PLAYERS - 1; // unreachable: capacity is checked first
    }

    private void reject(Socket socket, String reason, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "rejected");
        m.put("reason", reason);
        m.put("message", message);
        try (socket) {
            var w = new java.io.OutputStreamWriter(socket.getOutputStream(),
                java.nio.charset.StandardCharsets.UTF_8);
            w.write(Json.write(m));
            w.write('\n');
            w.flush();
        } catch (IOException ignored) {
            // the client will just see the socket close
        }
        System.out.println("Rejected a client: " + message);
    }

    // ---- The tick -----------------------------------------------------------

    private void gameLoop() {
        final long frameMillis = 16; // ~60 fps
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;
            if (dt > 0.05f) {
                dt = 0.05f;
            }

            synchronized (clients) {
                pruneDisconnected();
                applyInputs();
                if (phase == Phase.LOBBY) {
                    updateLobby(dt);
                }
                // updateLobby may have started the match, so re-check the phase.
                if (phase == Phase.RUNNING) {
                    stepMatch(dt);
                }
                broadcast(phase == Phase.LOBBY ? lobbyView().toJson()
                                               : StateSnapshot.fromWorld(world).toJson());
                pruneDisconnected();
            }

            try {
                Thread.sleep(frameMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Caller must hold the {@code clients} lock. */
    private void updateLobby(float dt) {
        int joined = clients.size();
        if (!counting && joined >= GameConfig.MIN_PLAYERS) {
            counting = true;
            countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
            System.out.println(joined + " players connected — starting in "
                + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + "s.");
        } else if (counting && joined < GameConfig.MIN_PLAYERS) {
            counting = false;
            countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
            System.out.println("Countdown cancelled — waiting for at least "
                + GameConfig.MIN_PLAYERS + " players.");
        }
        if (counting) {
            countdown -= dt;
            if (countdown <= 0f) {
                startMatch();
            }
        }
    }

    /** Caller must hold the {@code clients} lock. */
    private void startMatch() {
        List<PlayerSpec> roster = new ArrayList<>();
        for (ClientHandler c : clients) {
            roster.add(new PlayerSpec(c.playerId(), c.name()));
        }
        world = new GameWorld(roster, gameMode);
        phase = Phase.RUNNING;
        counting = false;
        countdown = 0f;
        restartRequested = false;
        System.out.println("Match started with " + roster.size() + " players: " + names());
        System.out.println("Further connections are now rejected until the match ends.");
    }

    /** Caller must hold the {@code clients} lock. */
    private void stepMatch(float dt) {
        if (restartRequested) {
            restartRequested = false;
            if (clients.size() >= GameConfig.MIN_PLAYERS) {
                List<PlayerSpec> roster = new ArrayList<>();
                for (ClientHandler c : clients) {
                    roster.add(new PlayerSpec(c.playerId(), c.name()));
                }
                world = new GameWorld(roster, gameMode);
                System.out.println("Rematch started with " + roster.size()
                    + " players: " + names());
            } else {
                returnToLobby("not enough players for a rematch");
                return;
            }
        }
        world.update(dt);
    }

    /** Caller must hold the {@code clients} lock. */
    private void returnToLobby(String why) {
        phase = Phase.LOBBY;
        world = null;
        counting = false;
        countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
        restartRequested = false;
        System.out.println("Back to the lobby (" + why + ") — accepting connections again.");
    }

    // ---- Client messages ----------------------------------------------------

    /** Caller must hold the {@code clients} lock. */
    private void applyInputs() {
        for (ClientHandler client : clients) {
            for (Map<String, Object> msg : client.drainInbox()) {
                applyMessage(client, msg);
            }
        }
    }

    /** Caller must hold the {@code clients} lock. */
    private void applyMessage(ClientHandler client, Map<String, Object> msg) {
        if (!(msg.get("type") instanceof String t)) {
            return;
        }
        if (t.equals("join")) {
            Object raw = msg.get("name");
            client.setName(raw instanceof String s ? s : null);
            client.send(welcomeJson(client));
            System.out.println("\"" + client.name() + "\" joined as slot " + client.playerId()
                + "  (" + clients.size() + "/" + GameConfig.MAX_PLAYERS + ")");
            return;
        }

        Player player = world != null ? world.playerById(client.playerId()) : null;
        if (player == null) {
            return; // lobby phase, or a spectatorless slot: nothing to drive
        }
        switch (t) {
            case "input" -> {
                player.wantLeft = Boolean.TRUE.equals(msg.get("left"));
                player.wantRight = Boolean.TRUE.equals(msg.get("right"));
            }
            // Actions are ignored once the match is decided, so nothing moves
            // behind the game-over overlay.
            case "jump" -> {
                if (!world.isOver()) {
                    player.jump();
                }
            }
            case "shoot" -> {
                if (!world.isOver()) {
                    world.spawnBullets(player.shoot());
                }
            }
            case "restart" -> {
                if (world.isOver()) {
                    restartRequested = true;
                }
            }
            default -> { /* ignore unknown message types */ }
        }
    }

    // ---- Broadcasting -------------------------------------------------------

    /** Caller must hold the {@code clients} lock. */
    private LobbyView lobbyView() {
        List<LobbyView.Seat> seats = new ArrayList<>();
        for (ClientHandler c : clients) {
            seats.add(new LobbyView.Seat(c.playerId(), c.name()));
        }
        return new LobbyView(seats, counting, Math.max(0f, countdown),
            GameConfig.MIN_PLAYERS, GameConfig.MAX_PLAYERS);
    }

    /** Caller must hold the {@code clients} lock. */
    private void broadcast(String json) {
        for (ClientHandler client : clients) {
            client.send(json);
        }
    }

    /** Caller must hold the {@code clients} lock. */
    private void pruneDisconnected() {
        List<ClientHandler> dead = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (!client.isConnected()) {
                dead.add(client);
            }
        }
        for (ClientHandler client : dead) {
            System.out.println("Client left: \"" + client.name() + "\" (slot "
                + client.playerId() + ")");
            if (world != null) {
                // Their character stops drifting on its last input and is out of
                // the match, so the remaining players can still win it.
                world.eliminate(client.playerId());
            }
            client.close();
            clients.remove(client);
        }
        if (!dead.isEmpty() && phase == Phase.RUNNING && clients.isEmpty()) {
            returnToLobby("everyone disconnected");
        }
    }

    private String welcomeJson(ClientHandler client) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "welcome");
        m.put("playerId", client.playerId());
        m.put("name", client.name());
        return Json.write(m);
    }

    /** Caller must hold the {@code clients} lock. */
    private String names() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : clients) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(c.name());
        }
        return sb.toString();
    }

    /** Best-effort LAN IPv4 address (e.g. 192.168.x.x), so a second machine can connect. */
    private static String lanHint() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(
                    java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            // fall through
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "localhost";
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new GameServer(port).run();
    }
}
