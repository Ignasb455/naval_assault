package gunmayhem.net;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.stages.Stage;
import gunmayhem.core.stages.Stages;
import gunmayhem.core.PlayerSpec;
import gunmayhem.core.gamemodes.GameMode;
import gunmayhem.core.events.MatchLog;
import gunmayhem.core.gamemodes.LastManStanding;
import gunmayhem.net.commands.AdminConsole;
import gunmayhem.net.state.LobbyState;
import gunmayhem.net.state.ServerState;
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
 * <p>A session runs through three states, each of which decides for itself
 * what the server allows while it is current (see {@link ServerState}):
 * <ol>
 *   <li>{@link gunmayhem.net.state.LobbyState} — clients connect and send the
 *       nickname their player chose. As soon as {@link GameConfig#MIN_PLAYERS}
 *       are connected a {@link GameConfig#LOBBY_COUNTDOWN_SECONDS}-second
 *       countdown starts and is broadcast to everyone, so all clients see the
 *       same clock. Late joiners are welcome while it runs, up to
 *       {@link GameConfig#MAX_PLAYERS}. If the lobby drops back below the
 *       minimum the countdown is cancelled.</li>
 *   <li>{@link gunmayhem.net.state.RunningState} — the countdown hit zero: the
 *       roster is frozen, the world is built and every further connection is
 *       rejected. The server applies each client's input, steps the simulation
 *       at ~60 Hz and broadcasts a {@link StateSnapshot} each tick.</li>
 *   <li>{@link gunmayhem.net.state.MatchOverState} — the game mode declared a
 *       winner. The world is still broadcast so the result stays on screen,
 *       but it is no longer stepped and player actions no longer reach it;
 *       only a rematch request means anything.</li>
 * </ol>
 *
 * <p>Wire protocol (one JSON object per line):
 * <ul>
 *   <li>client &rarr; server on connect: {@code {"type":"join","name":"Alice"}}</li>
 *   <li>server &rarr; client: {@code {"type":"welcome","playerId":N,"name":"Alice"}}</li>
 *   <li>server &rarr; client each tick: a {@link LobbyView} or a {@link StateSnapshot}</li>
 *   <li>client &rarr; server: {@code {"type":"input","left":b,"right":b}},
 *       {@code {"type":"jump"}}, {@code {"type":"drop"}}, {@code {"type":"shoot"}},
 *       {@code {"type":"restart"}}</li>
 *   <li>server &rarr; client when refused: {@code {"type":"rejected","reason":...}}</li>
 * </ul>
 */
public final class GameServer {
    public static final int DEFAULT_PORT = 5010;

    private final int port;
    private final GameMode gameMode;

    // Each match is played on a stage drawn at random, so the lobby never
    // knows in advance which one it will get. A stage id given on the command
    // line pins every match to that one instead, which is what you want when
    // demonstrating or debugging a particular stage.
    private final java.util.Random rng = new java.util.Random();
    private final String fixedStageId;

    // Command pattern: admin commands typed into this server's console.
    private final AdminConsole adminConsole = new AdminConsole(System.out);

    /** Connected clients. Every field below is guarded by this list's monitor. */
    private final List<ClientHandler> clients = new ArrayList<>();

    /**
     * What the server is doing right now. Each state carries the data only it
     * needs — the lobby its countdown, a match its world — so this is the one
     * field that used to be four. Read and written only under the clients lock.
     */
    private ServerState state = new LobbyState(this);

    private volatile boolean running = true;

    public GameServer(int port) {
        this(port, new LastManStanding(), null);
    }

    /**
     * @param gameMode decides when each match is over and who won
     * @param stageId  pins every match to that stage, or null to pick one at
     *                 random for each match.
     */
    public GameServer(int port, GameMode gameMode, String stageId) {
        this.port = port;
        if (gameMode == null) {
            throw new IllegalArgumentException("gameMode must not be null");
        }
        this.gameMode = gameMode;
        this.fixedStageId = stageId;
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

            adminConsole.listen(System.in);
            System.out.println("Admin commands: give <slot> <weapon>, lives <slot> <n>,"
                + " crate, stage save|rewind, group <label>, group end, undo, history");
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
                if (!state.admitsConnections()) {
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
                adminConsole.runPending(state.world());
                // A tick may hand over to another state — the lobby starting a
                // match, a match ending — so the broadcast asks again which
                // state is current rather than assuming the one that ticked.
                state.tick(dt);
                broadcast(state.broadcastJson());
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

    private Stage pickStage() {
        return fixedStageId == null ? Stages.random(rng) : Stages.byId(fixedStageId);
    }

    // ---- What the states are allowed to ask of the server --------------------

    /** Swap in the next state. Caller must hold the {@code clients} lock. */
    public void setState(ServerState next) {
        this.state = next;
    }

    /** The admin console, so the state demo can type into it. */
    public AdminConsole admin() {
        return adminConsole;
    }

    /** Name of the current state, for logs and the state demo. */
    public String stateName() {
        synchronized (clients) {
            return state.name();
        }
    }

    /** How many clients are connected. Caller must hold the {@code clients} lock. */
    public int clientCount() {
        return clients.size();
    }

    /**
     * Build a match for everyone currently connected, on a stage picked the
     * usual way, and log it under the given heading.
     *
     * <p>Caller must hold the {@code clients} lock.
     */
    public GameWorld createWorld(String logLabel) {
        List<PlayerSpec> roster = new ArrayList<>();
        for (ClientHandler c : clients) {
            roster.add(new PlayerSpec(c.playerId(), c.name()));
        }
        Stage stage = pickStage();
        GameWorld created = new GameWorld(roster, stage, gameMode);
        created.addObserver(new MatchLog(System.out)); // narrate on this console
        System.out.println(logLabel + " with " + roster.size() + " players on "
            + stage.displayName() + ": " + names());
        return created;
    }

    /** The lobby view for the countdown the lobby state is running. */
    public LobbyView lobbyView(boolean counting, float secondsLeft) {
        synchronized (clients) {
            List<LobbyView.Seat> seats = new ArrayList<>();
            for (ClientHandler c : clients) {
                seats.add(new LobbyView.Seat(c.playerId(), c.name()));
            }
            return new LobbyView(seats, counting, secondsLeft,
                GameConfig.MIN_PLAYERS, GameConfig.MAX_PLAYERS);
        }
    }

    /** Drop back to an empty lobby. Caller must hold the {@code clients} lock. */
    public void returnToLobby(String why) {
        state = new LobbyState(this);
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

        GameWorld world = state.world();
        if (world == null) {
            return; // in the lobby: nobody has a character yet
        }
        Player player = world.playerById(client.playerId());
        if (player == null) {
            return; // a slot with nothing to drive in this match
        }
        // Whether an action still counts is the state's call, not a flag read
        // off the world at each case.
        boolean acting = state.acceptsPlayerActions();
        switch (t) {
            case "input" -> {
                player.wantLeft = Boolean.TRUE.equals(msg.get("left"));
                player.wantRight = Boolean.TRUE.equals(msg.get("right"));
            }
            case "jump" -> {
                if (acting) {
                    player.jump();
                }
            }
            case "drop" -> {
                if (acting) {
                    player.dropThrough();
                }
            }
            case "shoot" -> {
                if (acting) {
                    // The world tells its observers; SoundEffects queues the gunshot.
                    world.fire(player);
                }
            }
            case "restart" -> state.onRestartRequested();
            default -> { /* ignore unknown message types */ }
        }
    }

    // ---- Broadcasting -------------------------------------------------------

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
        GameWorld world = state.world();
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
        if (!dead.isEmpty()) {
            state.onClientsChanged();
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
        new GameServer(port, new LastManStanding(), args.length > 1 ? args[1] : null).run();
    }
}
