package gunmayhem.render;

import gunmayhem.core.GameConfig;
import gunmayhem.core.PlayerStyle;
import gunmayhem.net.LobbyView;
import gunmayhem.net.StateSnapshot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Draws the game with Java2D primitives: rectangles for platforms and players,
 * small rectangles for bullets, and text for the HUD, the lobby and the
 * game-over banner. Everything, platforms included, comes from the server: the
 * stage varies between matches and some of its platforms move during one.
 */
public final class Java2DRenderer implements Renderer {
    private static final Color BACKGROUND = new Color(24, 26, 38);
    private static final Color PLATFORM = new Color(70, 78, 110);
    private static final Color PLATFORM_EDGE = new Color(120, 132, 170);

    // Platform materials read at a glance: ice is pale blue, a moving platform
    // is warmer than the solid slate around it.
    private static final Color PLATFORM_ICE = new Color(86, 122, 156);
    private static final Color PLATFORM_ICE_EDGE = new Color(170, 214, 245);
    private static final Color PLATFORM_MOVING = new Color(96, 84, 118);
    private static final Color PLATFORM_MOVING_EDGE = new Color(186, 150, 210);
    private static final Color MUTED = new Color(150, 156, 180);
    private static final Color DIM = new Color(220, 220, 220, 150);

    // Crate colours by rarity, so a rare drop is worth running for.
    private static final Color RARITY_COMMON = new Color(170, 176, 190);
    private static final Color RARITY_UNCOMMON = new Color(90, 170, 255);
    private static final Color RARITY_RARE = new Color(255, 195, 60);

    // Power-up markers drawn on the body.
    private static final Color TRAIL = new Color(255, 255, 255);
    private static final Color CLOUD = new Color(215, 222, 240, 190);
    private static final Color SHIELD = new Color(90, 180, 255);

    private final Font hudFont = new Font("Monospaced", Font.BOLD, 13);
    private final Font nameFont = new Font("SansSerif", Font.BOLD, 13);
    private final Font titleFont = new Font("SansSerif", Font.BOLD, 40);
    private final Font bigFont = new Font("SansSerif", Font.BOLD, 44);
    private final Font countdownFont = new Font("SansSerif", Font.BOLD, 92);
    private final Font seatFont = new Font("SansSerif", Font.BOLD, 18);

    @Override
    public void draw(Graphics2D g, StateSnapshot state) {
        prepare(g);
        drawBackdrop(g, state);
        drawCrates(g, state);
        drawBullets(g, state);
        drawPlayers(g, state);
        drawHud(g, state);

        if (state.over) {
            drawGameOver(g, state);
        }
    }

    @Override
    public void drawLobby(Graphics2D g, LobbyView lobby, int myPlayerId) {
        prepare(g);
        // No stage preview here: faded platforms cut across the countdown digits
        // and read as artefacts rather than scenery.
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        centerString(g, "GUN MAYHEM", 108);

        // Caption, big number and hint always sit at the same three baselines, so
        // nothing jumps around when the countdown starts.
        g.setFont(nameFont);
        g.setColor(MUTED);
        centerString(g, lobby.counting ? "MATCH STARTS IN" : "WAITING FOR PLAYERS", 158);

        g.setFont(countdownFont);
        if (lobby.counting) {
            // The countdown is the server's clock, so every client shows the same
            // number; it turns red for the last five seconds.
            int seconds = lobby.displaySeconds();
            g.setColor(seconds <= 5 ? new Color(255, 99, 71) : Color.WHITE);
            centerString(g, Integer.toString(seconds), 252);
        } else {
            g.setColor(new Color(255, 255, 255, 55));
            centerString(g, Integer.toString((int) GameConfig.LOBBY_COUNTDOWN_SECONDS), 252);
        }

        g.setFont(nameFont);
        g.setColor(MUTED);
        centerString(g, lobbyHint(lobby), 290);

        drawSeats(g, lobby, myPlayerId);

        g.setFont(hudFont);
        g.setColor(DIM);
        centerString(g, "Minimum " + lobby.minPlayers + " players, maximum " + lobby.maxPlayers
            + " \u2014 once the countdown ends nobody else may join",
            GameConfig.WORLD_HEIGHT - 24);
    }

    private String lobbyHint(LobbyView lobby) {
        if (lobby.counting) {
            boolean full = lobby.seats.size() >= lobby.maxPlayers;
            return full
                ? "Lobby full (" + lobby.seats.size() + "/" + lobby.maxPlayers
                  + ") \u2014 no more players can join"
                : "Others can still join until it hits zero  ("
                  + lobby.seats.size() + "/" + lobby.maxPlayers + " players)";
        }
        int missing = lobby.minPlayers - lobby.seats.size();
        return missing > 0
            ? "Need " + missing + " more player" + (missing == 1 ? "" : "s")
              + " to start the " + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + "s countdown"
            : "Starting shortly...";
    }

    private void drawSeats(Graphics2D g, LobbyView lobby, int myPlayerId) {
        int slots = lobby.maxPlayers;
        int boxW = 180;
        int boxH = 104;
        int gap = 18;
        int totalW = slots * boxW + (slots - 1) * gap;
        int x0 = (GameConfig.WORLD_WIDTH - totalW) / 2;
        int y = 340;

        for (int i = 0; i < slots; i++) {
            int x = x0 + i * (boxW + gap);
            LobbyView.Seat seat = i < lobby.seats.size() ? lobby.seats.get(i) : null;

            if (seat == null) {
                g.setColor(new Color(16, 18, 28, 220));
                g.fillRect(x, y, boxW, boxH);
                g.setColor(new Color(255, 255, 255, 45));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(x, y, boxW, boxH);
                g.setFont(seatFont);
                g.setColor(MUTED);
                centerStringIn(g, "open", x, boxW, y + boxH / 2 + 6);
                continue;
            }

            Color color = PlayerStyle.color(seat.id);
            g.setColor(new Color(16, 18, 28, 235));
            g.fillRect(x, y, boxW, boxH);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
            g.fillRect(x, y, boxW, boxH);
            g.setColor(color);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(x, y, boxW, boxH);

            // A little avatar in the player's colour, matching their in-game body.
            int avatarW = (int) GameConfig.PLAYER_WIDTH;
            int avatarH = (int) GameConfig.PLAYER_HEIGHT / 2;
            g.fillRect(x + boxW / 2 - avatarW / 2, y + 14, avatarW, avatarH);

            g.setFont(seatFont);
            g.setColor(Color.WHITE);
            centerStringIn(g, seat.name, x, boxW, y + boxH - 32);

            g.setFont(hudFont);
            g.setColor(seat.id == myPlayerId ? color : MUTED);
            centerStringIn(g, seat.id == myPlayerId ? "YOU" : PlayerStyle.name(seat.id),
                x, boxW, y + boxH - 12);
        }
    }

    // ---- Match rendering ----------------------------------------------------

    private void prepare(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawBackdrop(Graphics2D g, StateSnapshot state) {
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        for (StateSnapshot.PlatformView plat : state.platforms) {
            g.setColor(platformColor(plat.kind));
            g.fillRect((int) plat.x, (int) plat.y, (int) plat.width, (int) plat.height);
            g.setColor(platformEdgeColor(plat.kind));
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) plat.x, (int) plat.y,
                (int) (plat.x + plat.width), (int) plat.y);
        }
    }

    private static Color platformColor(String kind) {
        return switch (kind) {
            case "ice" -> PLATFORM_ICE;
            case "moving" -> PLATFORM_MOVING;
            default -> PLATFORM;
        };
    }

    private static Color platformEdgeColor(String kind) {
        return switch (kind) {
            case "ice" -> PLATFORM_ICE_EDGE;
            case "moving" -> PLATFORM_MOVING_EDGE;
            default -> PLATFORM_EDGE;
        };
    }

    private void drawBullets(Graphics2D g, StateSnapshot state) {
        for (StateSnapshot.BulletView b : state.bullets) {
            // Each weapon's template carries its own colour, so pellets and
            // pistol rounds are told apart at a glance.
            g.setColor(new Color(b.color));
            g.fillRect((int) b.x, (int) b.y,
                (int) GameConfig.BULLET_WIDTH, (int) GameConfig.BULLET_HEIGHT);
        }
    }

    /** Loot crates: a coloured box marked G for a gun or P for a power-up. */
    private void drawCrates(Graphics2D g, StateSnapshot state) {
        int size = (int) GameConfig.CRATE_SIZE;
        for (StateSnapshot.CrateView c : state.crates) {
            Color color = rarityColor(c.rarity);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g.fillRect((int) c.x, (int) c.y, size, size);
            g.setColor(color);
            g.setStroke(new BasicStroke(2f));
            g.drawRect((int) c.x, (int) c.y, size, size);

            g.setFont(hudFont);
            FontMetrics fm = g.getFontMetrics();
            String mark = "gun".equals(c.kind) ? "G" : "P";
            g.drawString(mark, (int) c.x + size / 2 - fm.stringWidth(mark) / 2,
                (int) c.y + size / 2 + 5);
        }
    }

    private Color rarityColor(String rarity) {
        return switch (rarity) {
            case "rare" -> RARITY_RARE;
            case "uncommon" -> RARITY_UNCOMMON;
            default -> RARITY_COMMON;
        };
    }

    private void drawPlayers(Graphics2D g, StateSnapshot state) {
        for (StateSnapshot.PlayerView p : state.players) {
            if (!p.alive) {
                continue;
            }
            int w = (int) GameConfig.PLAYER_WIDTH;
            int h = (int) GameConfig.PLAYER_HEIGHT;
            int cx = (int) (p.x + w / 2f);

            // Effects go under the body so the player stays readable.
            if (p.boosted && p.moving) {
                drawSpeedTrail(g, p, w, h);
            }
            if (p.tripleJump) {
                drawJumpCloud(g, p, w, h);
            }

            g.setColor(PlayerStyle.color(p.id));
            g.fillRect((int) p.x, (int) p.y, w, h);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawRect((int) p.x, (int) p.y, w, h);

            drawWeapon(g, p, w, h);

            if (p.shielded) {
                drawShieldRing(g, p, w, h);
            }

            // Damage % right above the head, nickname above that.
            g.setFont(hudFont);
            FontMetrics fm = g.getFontMetrics();
            String dmg = (int) p.damage + "%";
            // Lift the label when the shield ring would otherwise cross it.
            int dmgY = (int) p.y - (p.shielded ? 18 : 8);
            g.drawString(dmg, cx - fm.stringWidth(dmg) / 2, dmgY);

            g.setFont(nameFont);
            FontMetrics nfm = g.getFontMetrics();
            g.setColor(PlayerStyle.color(p.id));
            g.drawString(p.name, cx - nfm.stringWidth(p.name) / 2,
                dmgY - 16);
        }
    }

    // ---- Held weapon and power-up markers -----------------------------------

    /** The gun in the player's hands, shaped after whatever they picked up. */
    private void drawWeapon(Graphics2D g, StateSnapshot.PlayerView p, int w, int h) {
        g.setColor(Color.WHITE);
        int dir = p.facing > 0 ? 1 : -1;
        int handX = p.facing > 0 ? (int) (p.x + w) : (int) p.x;
        int handY = (int) (p.y + h / 2f);

        switch (p.weapon) {
            case "Semi-auto" -> {
                barrel(g, handX, handY - 2, 18, 4, dir);
                barrel(g, handX + dir * 2, handY + 2, 5, 6, dir); // grip
            }
            case "SMG" -> {
                barrel(g, handX, handY - 3, 24, 5, dir);
                barrel(g, handX + dir * 5, handY + 2, 6, 9, dir); // magazine
            }
            case "Shotgun" -> {
                barrel(g, handX, handY - 8, 30, 3, dir);
                barrel(g, handX, handY - 1, 30, 3, dir); // second barrel, clear gap
                barrel(g, handX + dir * 3, handY + 2, 7, 5, dir); // stock
            }
            default -> barrel(g, handX, handY - 2, 12, 4, dir); // Pistol
        }
    }

    /** Rectangle growing away from the body, so it mirrors with the facing. */
    private void barrel(Graphics2D g, int x, int y, int length, int thickness, int dir) {
        g.fillRect(dir > 0 ? x : x - length, y, length, thickness);
    }

    /** Speed boost: dashes left behind the runner. */
    private void drawSpeedTrail(Graphics2D g, StateSnapshot.PlayerView p, int w, int h) {
        int behind = p.facing > 0 ? (int) p.x : (int) (p.x + w);
        int dir = p.facing > 0 ? -1 : 1;
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 3; i++) {
            int x = behind + dir * (6 + i * 12);
            int alpha = 190 - i * 55;
            g.setColor(new Color(TRAIL.getRed(), TRAIL.getGreen(), TRAIL.getBlue(), alpha));
            g.drawLine(x, (int) (p.y + h * 0.35f), x + dir * 8, (int) (p.y + h * 0.35f));
            g.drawLine(x, (int) (p.y + h * 0.7f), x + dir * 8, (int) (p.y + h * 0.7f));
        }
    }

    /** Triple jump: a little cloud puffing under the feet. */
    private void drawJumpCloud(Graphics2D g, StateSnapshot.PlayerView p, int w, int h) {
        g.setColor(CLOUD);
        int baseY = (int) (p.y + h - 4);
        int cx = (int) (p.x + w / 2f);
        g.fillOval(cx - 20, baseY, 18, 12);
        g.fillOval(cx - 9, baseY - 4, 20, 14);
        g.fillOval(cx + 4, baseY, 18, 12);
    }

    /** Shield: a blue zig-zag ring that vanishes with the first hit taken. */
    private void drawShieldRing(Graphics2D g, StateSnapshot.PlayerView p, int w, int h) {
        int cx = (int) (p.x + w / 2f);
        int cy = (int) (p.y + h / 2f);
        int points = 16;
        double outer = Math.max(w, h) * 0.68;
        int[] xs = new int[points];
        int[] ys = new int[points];
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double r = (i % 2 == 0) ? outer : outer * 0.82;
            xs[i] = cx + (int) (Math.cos(angle) * r);
            ys[i] = cy + (int) (Math.sin(angle) * r);
        }
        g.setColor(SHIELD);
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(xs, ys, points);
    }

    private void drawHud(Graphics2D g, StateSnapshot state) {
        g.setFont(hudFont);
        int count = Math.max(1, state.players.size());
        int margin = 16;
        int column = (GameConfig.WORLD_WIDTH - 2 * margin) / count;
        int i = 0;
        for (StateSnapshot.PlayerView p : state.players) {
            // Columns are sized to the roster so a full four-player HUD still
            // fits on screen.
            int x = margin + i * column;
            g.setColor(p.alive ? PlayerStyle.color(p.id) : MUTED);
            g.drawString(p.alive
                ? p.name + "  LIVES " + Math.max(0, p.lives) + "  " + (int) p.damage + "%"
                : p.name + "  OUT", x, 24);
            if (p.alive) {
                // Second line keeps the weapon readable even with four long nicknames.
                g.setColor(DIM);
                g.drawString(p.weapon + powerTags(p), x, 40);
            }
            i++;
        }
        g.setColor(DIM);
        g.drawString("A/D or ←/→ move    W or ↑ jump (x2)    S or ↓ drop    F or L shoot    R rematch",
            margin, GameConfig.WORLD_HEIGHT - 16);
    }

    /** Short markers for the power-ups a player is carrying. */
    private String powerTags(StateSnapshot.PlayerView p) {
        StringBuilder sb = new StringBuilder();
        if (p.boosted) {
            sb.append("  +SPD");
        }
        if (p.tripleJump) {
            sb.append("  +JMP");
        }
        if (p.shielded) {
            sb.append("  +SHLD");
        }
        return sb.toString();
    }

    private void drawGameOver(Graphics2D g, StateSnapshot state) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        String winner = state.nameOf(state.winnerId);
        String text = state.winnerId >= 0 && !winner.isEmpty() ? winner + " WINS!" : "DRAW";
        g.setFont(bigFont);
        g.setColor(state.winnerId >= 0 ? PlayerStyle.color(state.winnerId) : Color.WHITE);
        centerString(g, text, GameConfig.WORLD_HEIGHT / 2 - 8);

        g.setFont(hudFont);
        g.setColor(Color.WHITE);
        centerString(g, "Press R for a rematch", GameConfig.WORLD_HEIGHT / 2 + 30);
    }

    // ---- Small helpers ------------------------------------------------------

    private void centerString(Graphics2D g, String text, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (GameConfig.WORLD_WIDTH - fm.stringWidth(text)) / 2, baselineY);
    }

    private void centerStringIn(Graphics2D g, String text, int x, int width, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (width - fm.stringWidth(text)) / 2, baselineY);
    }
}
