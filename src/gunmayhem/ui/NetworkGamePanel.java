package gunmayhem.ui;

import gunmayhem.core.GameConfig;
import gunmayhem.net.LobbyView;
import gunmayhem.net.NetworkClient;
import gunmayhem.net.StateSnapshot;
import gunmayhem.render.Java2DRenderer;
import gunmayhem.render.Renderer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The one and only game view. It renders whatever the server last sent — the
 * lobby countdown before the match, the authoritative {@link StateSnapshot}
 * during it — and forwards this player's input. It runs no simulation, so there
 * is no way for two clients to disagree about the game state.
 *
 * <p>Either control scheme drives your player, so testing several windows on one
 * keyboard is fine: move {@code A/D} or {@code arrows}, jump {@code W} or
 * {@code Up}, drop through a platform {@code S} or {@code Down}, shoot {@code F} or {@code L}, {@code R} for a rematch when over.
 */
public final class NetworkGamePanel extends JPanel {
    private final NetworkClient client;
    private final Renderer renderer = new Java2DRenderer();
    private final Set<Integer> held = new HashSet<>();
    private final Timer repaintTimer;
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

        // Without this, keys held when the window loses focus never produce a
        // release event and the player runs off the stage on their own.
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { releaseAllKeys(); }
        });

        repaintTimer = new Timer(16, e -> repaint());
        repaintTimer.start();
    }

    /**
     * Stop repainting. A closed window whose timer still runs keeps posting
     * events sixty times a second, which also keeps the process alive.
     */
    public void stop() {
        repaintTimer.stop();
    }

    private boolean isLeft(int c)  { return c == KeyEvent.VK_A || c == KeyEvent.VK_LEFT; }
    private boolean isRight(int c) { return c == KeyEvent.VK_D || c == KeyEvent.VK_RIGHT; }
    private boolean isJump(int c)  { return c == KeyEvent.VK_W || c == KeyEvent.VK_UP; }
    private boolean isDrop(int c)  { return c == KeyEvent.VK_S || c == KeyEvent.VK_DOWN; }
    private boolean isShoot(int c) { return c == KeyEvent.VK_F || c == KeyEvent.VK_L; }

    private void onKeyDown(int code) {
        boolean isRepeat = !held.add(code);
        if (!isRepeat) {
            if (isJump(code)) {
                client.sendJump();
            } else if (isDrop(code)) {
                client.sendDrop();
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

    private void releaseAllKeys() {
        held.clear();
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

        String rejection = client.rejectionMessage();
        StateSnapshot state = client.latest();
        LobbyView lobby = client.lobby();

        if (rejection != null) {
            drawStatus(g2, rejection, "Try again once the current match ends.");
        } else if (client.isInMatch() && state != null) {
            renderer.draw(g2, state);
            drawYouTag(g2);
        } else if (lobby != null) {
            renderer.drawLobby(g2, lobby, client.playerId());
            if (!client.isConnected()) {
                drawStatus(g2, "Disconnected from server.", "");
            }
        } else {
            drawStatus(g2, client.isConnected()
                ? "Connecting to server..." : "Disconnected from server.", "");
        }
    }

    private void drawYouTag(Graphics2D g) {
        if (client.playerId() < 0) {
            return;
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(gunmayhem.core.PlayerStyle.color(client.playerId()));
        String tag = "YOU: " + client.name();
        int w = g.getFontMetrics().stringWidth(tag);
        g.drawString(tag, GameConfig.WORLD_WIDTH - w - 16, 44);
        if (!client.isConnected()) {
            g.setColor(Color.WHITE);
            g.drawString("(disconnected)", GameConfig.WORLD_WIDTH - w - 16, 62);
        }
    }

    private void drawStatus(Graphics2D g, String message, String detail) {
        g.setColor(new Color(24, 26, 38));
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(message, (GameConfig.WORLD_WIDTH - fm.stringWidth(message)) / 2,
            GameConfig.WORLD_HEIGHT / 2);

        if (detail != null && !detail.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            FontMetrics fm2 = g.getFontMetrics();
            g.setColor(new Color(180, 186, 210));
            g.drawString(detail, (GameConfig.WORLD_WIDTH - fm2.stringWidth(detail)) / 2,
                GameConfig.WORLD_HEIGHT / 2 + 32);
        }
    }
}
