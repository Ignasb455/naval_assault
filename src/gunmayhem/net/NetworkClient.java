package gunmayhem.net;

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
 * Client-side network endpoint. Connects to the {@link GameServer}, reads state
 * snapshots on a background thread (exposing only the latest), and sends this
 * player's input. The client never simulates — it just draws what it receives.
 */
public final class NetworkClient {
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private volatile int playerId = -1;
    private volatile StateSnapshot latest;
    private volatile boolean connected = true;
    private volatile boolean rejectedFull;

    public NetworkClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(this::readLoop, "server-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                handle(line);
            }
        } catch (IOException e) {
            // fall through to disconnect
        }
        connected = false;
    }

    private void handle(String line) {
        Map<String, Object> msg;
        try {
            msg = Json.parseObject(line);
        } catch (RuntimeException badJson) {
            return;
        }
        Object type = msg.get("type");
        if (!(type instanceof String t)) {
            return;
        }
        switch (t) {
            case "welcome" -> {
                Object id = msg.get("playerId");
                if (id instanceof Number n) {
                    playerId = n.intValue();
                }
            }
            case "state" -> latest = StateSnapshot.fromJson(line);
            case "full" -> {
                rejectedFull = true;
                connected = false;
            }
            default -> { /* ignore */ }
        }
    }

    public int playerId()          { return playerId; }
    public StateSnapshot latest()  { return latest; }
    public boolean isConnected()   { return connected; }
    public boolean wasRejectedFull() { return rejectedFull; }

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
