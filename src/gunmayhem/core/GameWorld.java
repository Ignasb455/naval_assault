package gunmayhem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns all game state and advances the simulation one step at a time. This class
 * is deliberately free of any rendering or windowing code, so the exact same
 * logic could later be driven by an authoritative server.
 */
public final class GameWorld {
    private final List<Player> players = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Platform> platforms = new ArrayList<>();

    private boolean over;
    private Player winner;

    public GameWorld() {
        buildStage();
        spawnPlayers();
    }

    public List<Player> players()     { return players; }
    public List<Bullet> bullets()     { return bullets; }
    public List<Platform> platforms() { return platforms; }
    public boolean isOver()           { return over; }
    public Player winner()            { return winner; }

    private void buildStage() {
        platforms.addAll(Stage.createPlatforms());
    }

    private void spawnPlayers() {
        players.add(new Player(0, PlayerStyle.name(0), PlayerStyle.color(0),
            GameConfig.WORLD_WIDTH / 2f - 200, 360));
        players.add(new Player(1, PlayerStyle.name(1), PlayerStyle.color(1),
            GameConfig.WORLD_WIDTH / 2f + 170, 360));
    }

    public void update(float dt) {
        if (over) {
            return;
        }
        for (Player p : players) {
            if (p.isAlive()) {
                p.tick(dt);
                updatePlayerPhysics(p, dt);
            }
        }
        updateBullets(dt);
        checkOutOfBounds();
        checkGameOver();
    }

    private void updatePlayerPhysics(Player p, float dt) {
        int inputDir = (p.wantRight ? 1 : 0) - (p.wantLeft ? 1 : 0);

        // Horizontal: direct control at normal speeds; while being knocked back
        // (moving faster than run speed) let the impulse decay with friction, so
        // hits actually throw the player Gun-Mayhem style.
        if (Math.abs(p.vx) <= GameConfig.MOVE_SPEED + 1f) {
            p.vx = inputDir * GameConfig.MOVE_SPEED;
        } else {
            p.vx -= p.vx * (GameConfig.KNOCKBACK_FRICTION * dt);
            p.vx += inputDir * GameConfig.MOVE_SPEED * 0.15f * dt; // slight air steering
        }

        // Gravity.
        p.vy += GameConfig.GRAVITY * dt;
        if (p.vy > GameConfig.MAX_FALL_SPEED) {
            p.vy = GameConfig.MAX_FALL_SPEED;
        }

        // Integrate horizontally (platforms are one-way, so no side blocking).
        p.x += p.vx * dt;

        // Integrate vertically, remembering where the feet were beforehand so we
        // can detect a one-way landing this frame.
        float prevBottom = p.bounds().bottom();
        p.y += p.vy * dt;

        resolvePlatformLanding(p, prevBottom);
    }

    private void resolvePlatformLanding(Player p, float prevBottom) {
        p.onGround = false;
        if (p.vy < 0) {
            return; // moving up: pass through one-way platforms
        }
        Aabb body = p.bounds();
        for (Platform plat : platforms) {
            float top = plat.y;
            boolean horizontallyOver = body.right() > plat.x && body.left() < plat.x + plat.width;
            boolean crossedTop = prevBottom <= top + 1f && body.bottom() >= top;
            if (horizontallyOver && crossedTop) {
                p.y = top - GameConfig.PLAYER_HEIGHT;
                p.vy = 0;
                p.onGround = true;
                p.jumpsLeft = GameConfig.MAX_JUMPS;
                break;
            }
        }
    }

    private void updateBullets(float dt) {
        for (Bullet b : bullets) {
            if (!b.alive) {
                continue;
            }
            b.update(dt);
            for (Player p : players) {
                if (!p.isAlive() || p.id == b.ownerId) {
                    continue;
                }
                if (b.bounds().intersects(p.bounds())) {
                    p.takeHit(b.directionX());
                    b.alive = false;
                    break;
                }
            }
        }
        bullets.removeIf(b -> !b.alive);
    }

    private void checkOutOfBounds() {
        for (Player p : players) {
            if (!p.isAlive()) {
                continue;
            }
            Aabb b = p.bounds();
            boolean fell = b.top() > GameConfig.WORLD_HEIGHT + GameConfig.DEATH_MARGIN;
            boolean offSide = b.right() < -GameConfig.DEATH_MARGIN
                || b.left() > GameConfig.WORLD_WIDTH + GameConfig.DEATH_MARGIN;
            if (fell || offSide) {
                p.loseLifeAndRespawn();
            }
        }
    }

    private void checkGameOver() {
        List<Player> alive = new ArrayList<>();
        for (Player p : players) {
            if (p.isAlive()) {
                alive.add(p);
            }
        }
        if (players.size() >= 2 && alive.size() <= 1) {
            over = true;
            winner = alive.size() == 1 ? alive.get(0) : null;
        }
    }

    public void spawnBullet(Bullet bullet) {
        if (bullet != null) {
            bullets.add(bullet);
        }
    }
}
