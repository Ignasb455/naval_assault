package gunmayhem.net;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.PlayerStyle;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative game server. Owns the single {@link GameWorld}, accepts up to
 * {@code MAX_PLAYERS} TCP clients, applies their inputs, steps the simulation at
 * ~60 Hz and broadcasts a {@link StateSnapshot} to every client each tick.
 *
 * <p>Wire protocol (one JSON object per line):
 * <ul>
 *   <li>server &rarr; client on join: {@code {"type":"welcome","playerId":N}}</li>
 *   <li>server &rarr; client each tick: the state snapshot</li>
 *   <li>client &rarr; server: {@code {"type":"input","left":b,"right":b}},
 *       {@code {"type":"jump"}}, {@code {"type":"shoot"}}, {@code {"type":"restart"}}</li>
 * </ul>
 */
public final class GameServer {
    public static final int DEFAULT_PORT = 5010;

    private final int port;
    private final List<ClientHandler> clients = new ArrayList<>();
    private GameWorld world = new GameWorld();
    private volatile boolean running = true;
    private boolean restartRequested;

    private final int maxPlayers;

    public GameServer(int port) {
        this.port = port;
        this.maxPlayers = world.players().size();
    }

    public void run() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Gun Mayhem server listening on port " + port);
        System.out.println("LAN address: " + lanHint() + ":" + port
            + "   (clients run:  java -cp out gunmayhem.Main client <that-address>)");

        Thread loop = new Thread(this::gameLoop, "game-loop");
        loop.setDaemon(true);
        loop.start();

        while (running) {
            Socket socket = serverSocket.accept();
            synchronized (clients) {
                if (clients.size() >= maxPlayers) {
                    reject(socket);
                    continue;
                }
                int playerId = clients.size();
                try {
                    ClientHandler handler = new ClientHandler(socket, playerId);
                    clients.add(handler);
                    handler.startReading();
                    handler.send(welcomeJson(playerId));
                    System.out.println("Client joined as " + describe(playerId)
                        + " from " + handler.remoteAddress()
                        + "  (" + clients.size() + "/" + maxPlayers + ")");
                } catch (IOException e) {
                    System.out.println("Failed to set up client: " + e.getMessage());
                }
            }
        }
    }

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

            applyInputs();
            if (restartRequested) {
                world = new GameWorld();
                restartRequested = false;
            }
            world.update(dt);
            broadcastState();

            try {
                Thread.sleep(frameMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void applyInputs() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (!client.isConnected()) {
                    continue;
                }
                Player player = playerFor(client.playerId());
                for (Map<String, Object> msg : client.drainInbox()) {
                    applyMessage(player, msg);
                }
            }
            pruneDisconnected();
        }
    }

    private void applyMessage(Player player, Map<String, Object> msg) {
        Object type = msg.get("type");
        if (!(type instanceof String t) || player == null) {
            return;
        }
        switch (t) {
            case "input" -> {
                player.wantLeft = Boolean.TRUE.equals(msg.get("left"));
                player.wantRight = Boolean.TRUE.equals(msg.get("right"));
            }
            case "jump" -> player.jump();
            case "shoot" -> world.spawnBullet(player.shoot());
            case "restart" -> {
                if (world.isOver()) {
                    restartRequested = true;
                }
            }
            default -> { /* ignore unknown message types */ }
        }
    }

    private Player playerFor(int playerId) {
        List<Player> players = world.players();
        return playerId >= 0 && playerId < players.size() ? players.get(playerId) : null;
    }

    private void broadcastState() {
        String json = StateSnapshot.fromWorld(world).toJson();
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.send(json);
            }
            pruneDisconnected();
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
            System.out.println("Client left: " + describe(client.playerId()));
            // Stop the vacated player from drifting on its last input.
            Player p = playerFor(client.playerId());
            if (p != null) {
                p.wantLeft = false;
                p.wantRight = false;
            }
            client.close();
            clients.remove(client);
        }
    }

    private void reject(Socket socket) {
        try (socket) {
            var w = new java.io.OutputStreamWriter(socket.getOutputStream(),
                java.nio.charset.StandardCharsets.UTF_8);
            w.write("{\"type\":\"full\"}\n");
            w.flush();
        } catch (IOException ignored) {
            // client will just see the socket close
        }
        System.out.println("Rejected a client: game is full (" + maxPlayers + " players).");
    }

    private String welcomeJson(int playerId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "welcome");
        m.put("playerId", playerId);
        return Json.write(m);
    }

    private static String describe(int playerId) {
        return PlayerStyle.name(playerId) + " (id " + playerId + ")";
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
