package gunmayhem.ui;

import gunmayhem.core.GameConfig;
import gunmayhem.core.PlayerStyle;
import gunmayhem.net.NetworkClient;
import gunmayhem.net.StateSnapshot;
import gunmayhem.render.Java2DRenderer;
import gunmayhem.render.Renderer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The networked client view: renders the latest server {@link StateSnapshot} and
 * forwards this player's input to the server. Runs no simulation of its own.
 *
 * <p>Either control scheme drives your player, so both windows are playable on
 * one keyboard for testing: move {@code A/D} or {@code arrows}, jump {@code W}
 * or {@code Up}, shoot {@code F} or {@code L}, {@code R} to restart when over.
 */
public final class NetworkGamePanel extends JPanel {
    private final NetworkClient client;
    private final Renderer renderer = new Java2DRenderer();
    private final Set<Integer> held = new HashSet<>();
    private boolean lastLeft;
    private boolean lastRight;

    public NetworkGamePanel(NetworkClient client) {
        this.client = client;
        setPreferredSize(new Dimension(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        setBackground(new Color(24, 26, 38));

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKeyDown(e.getKeyCode()); }
            @Override public void keyReleased(KeyEvent e) { onKeyUp(e.getKeyCode()); }
        });

        Timer timer = new Timer(16, e -> repaint());
        timer.start();
    }

    private boolean isLeft(int c)  { return c == KeyEvent.VK_A || c == KeyEvent.VK_LEFT; }
    private boolean isRight(int c) { return c == KeyEvent.VK_D || c == KeyEvent.VK_RIGHT; }
    private boolean isJump(int c)  { return c == KeyEvent.VK_W || c == KeyEvent.VK_UP; }
    private boolean isShoot(int c) { return c == KeyEvent.VK_F || c == KeyEvent.VK_L; }

    private void onKeyDown(int code) {
        boolean isRepeat = !held.add(code);
        if (!isRepeat) {
            if (isJump(code)) {
                client.sendJump();
            } else if (isShoot(code)) {
                client.sendShoot();
            } else if (code == KeyEvent.VK_R) {
                client.sendRestart();
            }
        }
        sendMovementIfChanged();
    }

    private void onKeyUp(int code) {
        held.remove(code);
        sendMovementIfChanged();
    }

    private void sendMovementIfChanged() {
        boolean left = held.stream().anyMatch(this::isLeft);
        boolean right = held.stream().anyMatch(this::isRight);
        if (left != lastLeft || right != lastRight) {
            lastLeft = left;
            lastRight = right;
            client.sendInput(left, right);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        StateSnapshot state = client.latest();
        if (state != null) {
            renderer.draw(g2, state);
            drawYouTag(g2);
        } else {
            drawStatus(g2, client.wasRejectedFull()
                ? "Server is full (2 players already connected)."
                : client.isConnected() ? "Connecting to server..." : "Disconnected from server.");
        }
    }

    private void drawYouTag(Graphics2D g) {
        int id = client.playerId();
        if (id < 0) {
            return;
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(PlayerStyle.color(id));
        String tag = "YOU: " + PlayerStyle.name(id);
        int w = g.getFontMetrics().stringWidth(tag);
        g.drawString(tag, GameConfig.WORLD_WIDTH - w - 16, 24);
        if (!client.isConnected()) {
            g.setColor(Color.WHITE);
            g.drawString("(disconnected)", GameConfig.WORLD_WIDTH - w - 16, 42);
        }
    }

    private void drawStatus(Graphics2D g, String message) {
        g.setColor(new Color(24, 26, 38));
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        int w = g.getFontMetrics().stringWidth(message);
        g.drawString(message, (GameConfig.WORLD_WIDTH - w) / 2, GameConfig.WORLD_HEIGHT / 2);
    }
}
