package gunmayhem.ui;

import gunmayhem.core.GameConfig;
import gunmayhem.core.audio.AudioManager;
import gunmayhem.net.LobbyView;
import gunmayhem.net.NetworkClient;
import gunmayhem.net.StateSnapshot;
import gunmayhem.render.Java2DRenderer;
import gunmayhem.render.Renderer;
import gunmayhem.ui.controls.Controls;
import gunmayhem.ui.controls.KeyboardControls;
import gunmayhem.ui.controls.Mouse;
import gunmayhem.ui.controls.MouseControlsAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The one and only game view. It renders whatever the server last sent — the
 * lobby countdown before the match, the authoritative {@link StateSnapshot}
 * during it — and forwards this player's input. It runs no simulation, so there
 * is no way for two clients to disagree about the game state.
 *
 * <p>The player is driven through {@link Controls}, asked once per frame: the
 * keyboard directly, or the mouse through {@link MouseControlsAdapter}. The
 * panel never sees a key code or a mouse button. {@code R} asks for a rematch
 * when the match is over, whichever controls are in use. The speaker button in
 * the bottom-right corner (or {@code M}) toggles audio, which starts muted.
 */
public final class NetworkGamePanel extends JPanel {
    private final NetworkClient client;
    private final Renderer renderer = new Java2DRenderer();
    private final Controls controls;
    private final Timer repaintTimer;
    /** Drawn by hand rather than a JButton, so clicking it never steals keyboard focus. */
    private final Rectangle muteButton = new Rectangle(
        GameConfig.WORLD_WIDTH - 52, GameConfig.WORLD_HEIGHT - 44, 40, 32);
    private boolean lastLeft;
    private boolean lastRight;

    public NetworkGamePanel(NetworkClient client, boolean mouseControls) {
        this.client = client;
        setPreferredSize(new Dimension(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        setBackground(new Color(24, 26, 38));

        Mouse mouse = mouseControls ? new Mouse() : null;
        if (mouse != null) {
            addMouseMotionListener(mouse);
            controls = new MouseControlsAdapter(mouse, this::ownPlayerCenterX);
        } else {
            KeyboardControls keyboard = new KeyboardControls();
            addKeyListener(keyboard);
            addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { keyboard.releaseAll(); }
            });
            controls = keyboard;
        }

        // Keys that are not player controls, so they work with either scheme.
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    client.sendRestart();
                } else if (e.getKeyCode() == KeyEvent.VK_M) {
                    AudioManager.getInstance().toggleMuted();
                }
            }
        });

        // The panel sees clicks first: one on the mute button is not passed on
        // to the mouse controls, or toggling audio would also fire a shot.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (muteButton.contains(e.getPoint())) {
                    AudioManager.getInstance().toggleMuted();
                } else if (mouse != null) {
                    mouse.mousePressed(e);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (mouse != null) {
                    mouse.mouseEntered(e);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (mouse != null) {
                    mouse.mouseExited(e);
                }
            }
        });

        repaintTimer = new Timer(16, e -> {
            pollControls();
            repaint();
        });
        repaintTimer.start();
    }

    /**
     * Stop repainting. A closed window whose timer still runs keeps posting
     * events sixty times a second, which also keeps the process alive.
     */
    public void stop() {
        repaintTimer.stop();
    }

    /**
     * Ask the controls what the player wants and forward it. Done every frame,
     * not on input events: with the mouse the direction changes as the player
     * runs past the cursor, without any event at all.
     */
    private void pollControls() {
        boolean left = controls.left();
        boolean right = controls.right();
        if (left != lastLeft || right != lastRight) {
            lastLeft = left;
            lastRight = right;
            client.sendInput(left, right);
        }
        if (controls.takeJump()) {
            client.sendJump();
        }
        if (controls.takeDrop()) {
            client.sendDrop();
        }
        if (controls.takeShoot()) {
            client.sendShoot();
        }
    }

    /** Where this client's player is drawn, or NaN when it has none on screen. */
    private double ownPlayerCenterX() {
        StateSnapshot state = client.latest();
        if (!client.isInMatch() || state == null) {
            return Double.NaN;
        }
        for (StateSnapshot.PlayerView p : state.players) {
            if (p.id == client.playerId() && p.alive) {
                return p.x + GameConfig.PLAYER_WIDTH / 2f;
            }
        }
        return Double.NaN;
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
        drawMuteButton(g2);
    }

    /** A small speaker; crossed out while muted. */
    private void drawMuteButton(Graphics2D g) {
        boolean muted = AudioManager.getInstance().isMuted();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = muteButton.x;
        int y = muteButton.y;

        g.setColor(new Color(255, 255, 255, 30));
        g.fillRoundRect(x, y, muteButton.width, muteButton.height, 8, 8);
        g.setColor(new Color(180, 186, 210));
        g.drawRoundRect(x, y, muteButton.width, muteButton.height, 8, 8);

        // Speaker body and cone.
        g.fillRect(x + 8, y + 12, 5, 8);
        g.fillPolygon(new int[] {x + 13, x + 20, x + 20, x + 13},
            new int[] {y + 12, y + 6, y + 26, y + 20}, 4);

        g.setStroke(new BasicStroke(2f));
        if (muted) {
            g.setColor(new Color(255, 99, 71));
            g.drawLine(x + 24, y + 11, x + 32, y + 21);
            g.drawLine(x + 32, y + 11, x + 24, y + 21);
        } else {
            g.drawArc(x + 16, y + 10, 10, 12, -50, 100);
            g.drawArc(x + 16, y + 5, 18, 22, -50, 100);
        }
        g.setStroke(new BasicStroke(1f));
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
