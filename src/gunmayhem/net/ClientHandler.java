package gunmayhem.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import gunmayhem.core.PlayerStyle;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Server-side representation of one connected client. A background thread reads
 * newline-delimited JSON messages into a thread-safe inbox; the server's game
 * loop drains that inbox each tick. Writes happen from the game-loop thread.
 */
public final class ClientHandler {
    private final Socket socket;
    private final int playerId;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final Queue<Map<String, Object>> inbox = new ConcurrentLinkedQueue<>();
    private volatile boolean connected = true;
    private volatile String name;

    public ClientHandler(Socket socket, int playerId) throws IOException {
        this.socket = socket;
        this.playerId = playerId;
        this.name = PlayerStyle.name(playerId);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public int playerId() {
        return playerId;
    }

    /** The nickname this player chose (defaults to the slot name until they join). */
    public String name() {
        return name;
    }

    public void setName(String raw) {
        this.name = PlayerStyle.sanitizeName(raw, playerId);
    }

    public boolean isConnected() {
        return connected;
    }

    public String remoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }

    /** Start the reader thread that fills the inbox. */
    public void startReading() {
        Thread t = new Thread(this::readLoop, "client-" + playerId + "-reader");
        t.setDaemon(true);
        t.start();
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    inbox.add(Json.parseObject(line));
                } catch (RuntimeException badJson) {
                    // Ignore a malformed message rather than dropping the client.
                }
            }
        } catch (IOException e) {
            // fall through to disconnect
        }
        connected = false;
    }

    /** Drain everything received since the last tick. Called on the game-loop thread. */
    public java.util.List<Map<String, Object>> drainInbox() {
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        Map<String, Object> m;
        while ((m = inbox.poll()) != null) {
            out.add(m);
        }
        return out;
    }

    /** Send one line of text. Marks the client disconnected on failure. */
    public synchronized void send(String line) {
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
            // nothing useful to do
        }
    }
}
