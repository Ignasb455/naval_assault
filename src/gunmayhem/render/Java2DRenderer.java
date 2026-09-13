package gunmayhem.render;

import gunmayhem.core.GameConfig;
import gunmayhem.core.Platform;
import gunmayhem.core.PlayerStyle;
import gunmayhem.core.Stage;
import gunmayhem.net.StateSnapshot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Draws a {@link StateSnapshot} with Java2D primitives: rectangles for platforms
 * and players, small rectangles for bullets, and text for the HUD. Platforms are
 * the static stage; everything else comes from the snapshot.
 */
public final class Java2DRenderer implements Renderer {
    private static final Color BACKGROUND = new Color(24, 26, 38);
    private static final Color PLATFORM = new Color(70, 78, 110);
    private static final Color PLATFORM_EDGE = new Color(120, 132, 170);
    private static final Color BULLET = new Color(255, 212, 0);

    private final Font hudFont = new Font("Monospaced", Font.BOLD, 13);
    private final Font bigFont = new Font("SansSerif", Font.BOLD, 44);

    private final List<Platform> platforms = Stage.createPlatforms();

    @Override
    public void draw(Graphics2D g, StateSnapshot state) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        drawPlatforms(g);
        drawBullets(g, state);
        drawPlayers(g, state);
        drawHud(g, state);

        if (state.over) {
            drawGameOver(g, state);
        }
    }

    private void drawPlatforms(Graphics2D g) {
        for (Platform plat : platforms) {
            g.setColor(PLATFORM);
            g.fillRect((int) plat.x, (int) plat.y, (int) plat.width, (int) plat.height);
            g.setColor(PLATFORM_EDGE);
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) plat.x, (int) plat.y, (int) (plat.x + plat.width), (int) plat.y);
        }
    }

    private void drawBullets(Graphics2D g, StateSnapshot state) {
        g.setColor(BULLET);
        for (StateSnapshot.BulletView b : state.bullets) {
            g.fillRect((int) b.x, (int) b.y,
                (int) GameConfig.BULLET_WIDTH, (int) GameConfig.BULLET_HEIGHT);
        }
    }

    private void drawPlayers(Graphics2D g, StateSnapshot state) {
        g.setFont(hudFont);
        FontMetrics fm = g.getFontMetrics();
        for (StateSnapshot.PlayerView p : state.players) {
            if (!p.alive) {
                continue;
            }
            int w = (int) GameConfig.PLAYER_WIDTH;
            int h = (int) GameConfig.PLAYER_HEIGHT;
            int cx = (int) (p.x + w / 2f);

            g.setColor(PlayerStyle.color(p.id));
            g.fillRect((int) p.x, (int) p.y, w, h);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawRect((int) p.x, (int) p.y, w, h);

            // Gun barrel showing facing direction.
            int gunX = p.facing > 0 ? (int) (p.x + w) : (int) (p.x - 14);
            g.fillRect(gunX, (int) (p.y + h / 2f - 2), 14, 4);

            // Damage % above the head.
            String dmg = (int) p.damage + "%";
            g.drawString(dmg, cx - fm.stringWidth(dmg) / 2, (int) p.y - 8);
        }
    }

    private void drawHud(Graphics2D g, StateSnapshot state) {
        g.setFont(hudFont);
        int x = 16;
        for (StateSnapshot.PlayerView p : state.players) {
            g.setColor(PlayerStyle.color(p.id));
            String line = PlayerStyle.name(p.id)
                + "  lives:" + Math.max(0, p.lives) + "  dmg:" + (int) p.damage + "%";
            g.drawString(line, x, 24);
            x += 280;
        }
        g.setColor(new Color(220, 220, 220, 150));
        g.drawString(
            "P1: A/D move  W jump(x2)  F shoot     P2: Left/Right move  Up jump(x2)  L shoot     R: restart",
            16, GameConfig.WORLD_HEIGHT - 16);
    }

    private void drawGameOver(Graphics2D g, StateSnapshot state) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        String text = state.winnerId >= 0 ? PlayerStyle.name(state.winnerId) + " WINS!" : "DRAW";
        g.setFont(bigFont);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(text,
            (GameConfig.WORLD_WIDTH - fm.stringWidth(text)) / 2,
            GameConfig.WORLD_HEIGHT / 2 - 8);

        g.setFont(hudFont);
        FontMetrics fm2 = g.getFontMetrics();
        String sub = "Press R to restart";
        g.drawString(sub,
            (GameConfig.WORLD_WIDTH - fm2.stringWidth(sub)) / 2,
            GameConfig.WORLD_HEIGHT / 2 + 30);
    }
}
