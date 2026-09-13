package gunmayhem.ui;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.net.StateSnapshot;
import gunmayhem.render.Java2DRenderer;
import gunmayhem.render.Renderer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The Swing host: owns the ~60fps game loop and keyboard input, then hands
 * drawing off to a {@link Renderer}. Local two-player demo on one keyboard.
 */
public final class GamePanel extends JPanel {
    private GameWorld world = new GameWorld();
    private final Renderer renderer = new Java2DRenderer();

    private final Timer timer;
    private long lastNanos = System.nanoTime();

    // Keys currently held, so OS auto-repeat does not re-trigger jump/shoot.
    private final Set<Integer> held = new HashSet<>();

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKeyDown(e.getKeyCode()); }
            @Override public void keyReleased(KeyEvent e) { held.remove(e.getKeyCode()); }
        });

        timer = new Timer(16, e -> tick()); // ~60 fps
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;

        // Clamp dt so a stall (e.g. window drag) can't fling players off-stage.
        if (dt > 0.05f) {
            dt = 0.05f;
        }

        readHeldMovement();
        world.update(dt);
        repaint();
    }

    private void readHeldMovement() {
        Player p1 = world.players().get(0);
        p1.wantLeft = held.contains(KeyEvent.VK_A);
        p1.wantRight = held.contains(KeyEvent.VK_D);

        Player p2 = world.players().get(1);
        p2.wantLeft = held.contains(KeyEvent.VK_LEFT);
        p2.wantRight = held.contains(KeyEvent.VK_RIGHT);
    }

    private void onKeyDown(int code) {
        if (!held.add(code)) {
            return; // already held: ignore auto-repeat for edge actions
        }
        Player p1 = world.players().get(0);
        Player p2 = world.players().get(1);
        switch (code) {
            case KeyEvent.VK_W -> p1.jump();
            case KeyEvent.VK_F -> world.spawnBullet(p1.shoot());
            case KeyEvent.VK_UP -> p2.jump();
            case KeyEvent.VK_L -> world.spawnBullet(p2.shoot());
            case KeyEvent.VK_R -> restart();
            default -> { }
        }
    }

    private void restart() {
        world = new GameWorld();
        held.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.draw((Graphics2D) g, StateSnapshot.fromWorld(world));
    }
}
