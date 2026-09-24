package gunmayhem.core;

import gunmayhem.core.drops.Drop;
import gunmayhem.core.drops.DropFactory;
import gunmayhem.core.drops.GunDropFactory;
import gunmayhem.core.drops.PowerUpDropFactory;
import gunmayhem.core.gamemodes.GameMode;
import gunmayhem.core.gamemodes.LastManStanding;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Owns all game state and advances the simulation one step at a time. This class
 * is deliberately free of any rendering or windowing code: the authoritative
 * server is the only thing that drives it.
 */
public final class GameWorld {
    private final List<Player> players = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Platform> platforms = new ArrayList<>();
    private final List<LootCrate> crates = new ArrayList<>();
    private final GameMode gameMode;

    private final Random rng = new Random();
    private float crateTimer = GameConfig.CRATE_INTERVAL;

    private boolean over;
    private Player winner;
    private float elapsedSeconds;

    /** Build a match for the given roster (2-4 players, ids need not be contiguous). */
    public GameWorld(List<PlayerSpec> roster) {
        this(roster, new LastManStanding());
    }

    public GameWorld(List<PlayerSpec> roster, GameMode gameMode) {
        if (gameMode == null) {
            throw new IllegalArgumentException("gameMode must not be null");
        }
        this.gameMode = gameMode;
        platforms.addAll(Stage.createPlatforms());
        for (int slot = 0; slot < roster.size(); slot++) {
            PlayerSpec spec = roster.get(slot);
            float[] spawn = Stage.spawnPoint(slot);
            players.add(new Player(spec.id(), spec.name(), PlayerStyle.color(spec.id()),
                spawn[0], spawn[1]));
        }
        gameMode.configurePlayers(players);
    }

    public List<Player> players()     { return players; }
    public List<Bullet> bullets()     { return bullets; }
    public List<Platform> platforms() { return platforms; }
    public List<LootCrate> crates()   { return crates; }
    public boolean isOver()           { return over; }
    public Player winner()            { return winner; }

    /** The player holding that server slot, or null if they are not in this match. */
    public Player playerById(int id) {
        for (Player p : players) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    /** Knock a player out for good — used when their client drops mid-match. */
    public void eliminate(int id) {
        Player p = playerById(id);
        if (p != null) {
            p.eliminate();
        }
        checkGameOver();
    }

    public void update(float dt) {
        if (over) {
            return;
        }
        elapsedSeconds += Math.max(0f, dt);
        for (Player p : players) {
            p.tick(dt); // cooldowns keep ticking even for eliminated players
            if (p.isAlive()) {
                updatePlayerPhysics(p, dt);
            }
        }
        updateBullets(dt);
        updateCrates(dt);
        checkOutOfBounds();
        checkGameOver();
    }

    // ---- Loot crates --------------------------------------------------------

    /**
     * Drop a crate every so often, let the ones already falling settle on a
     * platform, and hand their contents to whoever walks into them.
     */
    private void updateCrates(float dt) {
        crateTimer -= dt;
        if (crateTimer <= 0f) {
            crateTimer = GameConfig.CRATE_INTERVAL;
            spawnCrate();
        }
        for (LootCrate crate : crates) {
            crate.update(dt);
            landCrate(crate);
            collectCrate(crate);
        }
        crates.removeIf(c -> !c.alive);
    }

    /**
     * The abstract factory in use: pick a family (guns or power-ups) once, then
     * ask it for a rarity. The world never names a concrete drop class.
     */
    private void spawnCrate() {
        DropFactory factory = rng.nextBoolean()
            ? new GunDropFactory()
            : new PowerUpDropFactory();

        int roll = rng.nextInt(100);
        Drop drop = roll < 60 ? factory.createCommon()
                  : roll < 90 ? factory.createUncommon()
                  :             factory.createRare();

        float x = 60f + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 120f);
        crates.add(new LootCrate(drop, x, -GameConfig.CRATE_SIZE));
    }

    /** Stop a falling crate on the first platform top it crosses. */
    private void landCrate(LootCrate crate) {
        if (crate.landed) {
            return;
        }
        Aabb box = crate.bounds();
        for (Platform plat : platforms) {
            boolean horizontallyOver = box.right() > plat.x
                && box.left() < plat.x + plat.width;
            if (horizontallyOver && box.bottom() >= plat.y && box.top() < plat.y) {
                crate.y = plat.y - GameConfig.CRATE_SIZE;
                crate.vy = 0f;
                crate.landed = true;
                return;
            }
        }
        if (crate.y > GameConfig.WORLD_HEIGHT + GameConfig.DEATH_MARGIN) {
            crate.alive = false; // missed every platform
        }
    }

    private void collectCrate(LootCrate crate) {
        if (!crate.alive) {
            return;
        }
        for (Player p : players) {
            if (p.isAlive() && crate.bounds().intersects(p.bounds())) {
                crate.drop.applyTo(p);
                crate.alive = false;
                return;
            }
        }
    }

    private void updatePlayerPhysics(Player p, float dt) {
        int inputDir = (p.wantRight ? 1 : 0) - (p.wantLeft ? 1 : 0);

        // Horizontal: direct control at normal speeds; while being knocked back
        // (moving faster than run speed) let the impulse decay with friction, so
        // hits actually throw the player Gun-Mayhem style.
        float runSpeed = GameConfig.MOVE_SPEED * p.speedMultiplier;
        if (Math.abs(p.vx) <= runSpeed + 1f) {
            p.vx = inputDir * runSpeed;
        } else {
            p.vx -= p.vx * (GameConfig.KNOCKBACK_FRICTION * dt);
            p.vx += inputDir * runSpeed * 0.15f * dt; // slight air steering
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
                p.jumpsLeft = p.maxJumps;
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
                    p.lastHitBy = b.ownerId;
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
                int livesBefore = p.lives;
                    int killerId = p.lastHitBy;
                    p.loseLifeAndRespawn();
                    if (livesBefore > 0 && killerId >= 0) {
                        Player killer = playerById(killerId);
                    if (killer != null && killer != p) {
                        killer.kills++;
                    }
                }
            }
        }
    }

    private void checkGameOver() {
        GameMode.GameOverResult result = gameMode.checkGameOver(players, elapsedSeconds);
        if (result.over()) {
            over = true;
            winner = result.winner();
        }
    }

    /** Add the bullets a weapon just produced (a shotgun gives several). */
    public void spawnBullets(List<Bullet> fired) {
        bullets.addAll(fired);
    }
}
