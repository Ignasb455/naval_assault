package gunmayhem.net;

import gunmayhem.core.audio.AudioManager;
import gunmayhem.core.audio.Sound;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side network endpoint. Connects to the {@link GameServer}, announces
 * the nickname the player chose, then reads whatever the server broadcasts on a
 * background thread — the lobby countdown before the match, state snapshots
 * during it. The client never simulates: it just draws what it receives.
 */
public final class NetworkClient {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final String requestedName;

    private volatile int playerId = -1;
    private volatile String name;
    private volatile StateSnapshot latest;
    private volatile LobbyView lobby;
    private volatile boolean inMatch;
    private volatile boolean connected = true;
    private volatile String rejectionMessage;

    public NetworkClient(String host, int port, String nickname) throws IOException {
        this.requestedName = nickname == null ? "" : nickname;
        this.name = this.requestedName;
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(this::readLoop, "server-reader");
        reader.setDaemon(true);
        reader.start();
        sendJoin();
    }

    private void sendJoin() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "join");
        m.put("name", requestedName);
        send(Json.write(m));
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    handle(line);
                }
            }
        } catch (IOException e) {
            // fall through to disconnect
        }
        connected = false;
    }

    private void handle(String line) {
        // One malformed or unexpected message must not kill the reader thread —
        // that would leave the client "connected" but permanently frozen.
        try {
            Map<String, Object> msg = Json.parseObject(line);
            if (!(msg.get("type") instanceof String t)) {
                return;
            }
            switch (t) {
                case "welcome" -> {
                    if (msg.get("playerId") instanceof Number n) {
                        playerId = n.intValue();
                    }
                    if (msg.get("name") instanceof String s) {
                        name = s;
                    }
                }
                case "lobby" -> {
                    lobby = LobbyView.fromJson(line);
                    inMatch = false;
                }
                case "state" -> {
                    StateSnapshot snapshot = StateSnapshot.fromJson(line);
                    latest = snapshot;
                    inMatch = true;
                    for (Sound sound : snapshot.sounds) {
                        AudioManager.getInstance().playSound(sound);
                    }
                }
                case "rejected" -> {
                    rejectionMessage = msg.get("message") instanceof String s
                        ? s : "The server refused the connection.";
                    connected = false;
                }
                default -> { /* ignore unknown message types */ }
            }
        } catch (RuntimeException badMessage) {
            // skip this line and keep reading
        }
    }

    public int playerId()            { return playerId; }
    public String name()             { return name; }
    public StateSnapshot latest()    { return latest; }
    public LobbyView lobby()         { return lobby; }
    public boolean isInMatch()       { return inMatch; }
    public boolean isConnected()     { return connected; }
    public String rejectionMessage() { return rejectionMessage; }

    public void sendInput(boolean left, boolean right) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "input");
        m.put("left", left);
        m.put("right", right);
        send(Json.write(m));
    }

    public void sendJump()    { send("{\"type\":\"jump\"}"); }
    public void sendShoot()   { send("{\"type\":\"shoot\"}"); }
    public void sendRestart() { send("{\"type\":\"restart\"}"); }

    private synchronized void send(String line) {
        if (!connected) {
            return;
        }
        try {
            out.write(line);
            out.write('\n');
            out.flush();
        } catch (IOException e) {
            connected = false;
        }
    }

    public void close() {
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing to do
        }
    }
}
