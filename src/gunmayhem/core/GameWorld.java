package gunmayhem.core;

import gunmayhem.core.audio.Sound;
import gunmayhem.core.drops.Drop;
import gunmayhem.core.drops.DropFactory;
import gunmayhem.core.drops.GunDropFactory;
import gunmayhem.core.drops.PowerUpDropFactory;
import gunmayhem.core.events.GameObserver;
import gunmayhem.core.events.KillTracker;
import gunmayhem.core.events.SoundEffects;
import gunmayhem.core.gamemodes.GameMode;
import gunmayhem.core.gamemodes.LastManStanding;
import gunmayhem.core.stages.Platform;
import gunmayhem.core.stages.Stage;
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
    private final Stage stage;      // owns the platforms and the spawn points
    private final List<LootCrate> crates = new ArrayList<>();
    private final GameMode gameMode;
    private final List<Sound> soundEvents = new ArrayList<>();

    // Observer pattern: the world is the subject and tells these, in order,
    // about shots, knockouts, crate pickups and the end of the match.
    private final List<GameObserver> observers = new ArrayList<>();

    private final Random rng = new Random();
    private float crateTimer = GameConfig.CRATE_INTERVAL;

    private boolean over;
    private Player winner;
    private float elapsedSeconds;

    /** Build a match for the given roster (2-4 players, ids need not be contiguous). */
    public GameWorld(List<PlayerSpec> roster, Stage stage) {
        this(roster, stage, new LastManStanding());
    }

    public GameWorld(List<PlayerSpec> roster, Stage stage, GameMode gameMode) {
        if (gameMode == null) {
            throw new IllegalArgumentException("gameMode must not be null");
        }
        this.gameMode = gameMode;
        this.stage = stage;
        for (int slot = 0; slot < roster.size(); slot++) {
            PlayerSpec spec = roster.get(slot);
            float[] spawn = stage.spawnPoint(slot);
            players.add(new Player(spec.id(), spec.name(), PlayerStyle.color(spec.id()),
                spawn[0], spawn[1]));
        }
        gameMode.configurePlayers(players);

        // Every match needs these two: the kill-based modes read the kill
        // counts, and clients expect a gunshot sound for every shot.
        addObserver(new KillTracker());
        addObserver(new SoundEffects(this));
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /** The registered observers, in the order they are notified. */
    public List<GameObserver> observers() {
        return List.copyOf(observers);
    }

    public List<Player> players()     { return players; }
    public List<Bullet> bullets()     { return bullets; }
    public List<Platform> platforms() { return stage.platforms(); }
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
        // Platforms move first: everything below reads their current position,
        // and riders are carried by the distance covered in this step.
        stage.update(dt);
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
            if (crate.landed && crate.ground != null) {
                crate.x += crate.ground.deltaX();
                crate.y += crate.ground.deltaY();
            }
            landCrate(crate);
            collectCrate(crate);
        }
        crates.removeIf(c -> !c.alive);
    }

    /**
     * The abstract factory in use: pick a family (guns or power-ups) once, then
     * ask it for a rarity. The world never names a concrete drop class.
     */
    private LootCrate spawnCrate() {
        DropFactory factory = rng.nextBoolean()
            ? new GunDropFactory()
            : new PowerUpDropFactory();

        int roll = rng.nextInt(100);
        Drop drop = roll < 60 ? factory.createCommon()
                  : roll < 90 ? factory.createUncommon()
                  :             factory.createRare();

        float x = 60f + rng.nextFloat() * (GameConfig.WORLD_WIDTH - 120f);
        LootCrate crate = new LootCrate(drop, x, -GameConfig.CRATE_SIZE);
        crates.add(crate);
        return crate;
    }

    /** Drop a random crate right now, outside the usual timer. */
    public LootCrate dropCrate() {
        return spawnCrate();
    }

    /** Stop a falling crate on the first platform top it crosses. */
    private void landCrate(LootCrate crate) {
        if (crate.landed) {
            return;
        }
        Aabb box = crate.bounds();
        for (Platform plat : stage.platforms()) {
            boolean horizontallyOver = box.right() > plat.x()
                && box.left() < plat.x() + plat.width();
            if (horizontallyOver && box.bottom() >= plat.y() && box.top() < plat.y()) {
                crate.y = plat.y() - GameConfig.CRATE_SIZE;
                crate.vy = 0f;
                crate.landed = true;
                crate.ground = plat;
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
                for (GameObserver o : observers) {
                    o.onCratePicked(p, crate.drop);
                }
                return;
            }
        }
    }

    private void updatePlayerPhysics(Player p, float dt) {
        // Ride the platform underfoot before anything else: it has already moved
        // this frame, and prevBottom below must be measured from where the feet
        // ended up. Skip this and a rising platform leaves the player behind,
        // fails the one-way landing test and passes straight through them.
        if (p.ground != null) {
            p.x += p.ground.deltaX();
            p.y += p.ground.deltaY();
        }

        int inputDir = (p.wantRight ? 1 : 0) - (p.wantLeft ? 1 : 0);

        // Horizontal: direct control at normal speeds; while being knocked back
        // (moving faster than run speed) let the impulse decay with friction, so
        // hits actually throw the player Gun-Mayhem style.
        float runSpeed = GameConfig.MOVE_SPEED * p.speedMultiplier;
        if (Math.abs(p.vx) <= runSpeed + 1f) {
            // Approach the intended speed at the rate the surface allows. Solid
            // ground clamps to 1 and so is instant, exactly as before; ice takes
            // a moment to get going and a moment to stop.
            float target = inputDir * runSpeed;
            float grip = p.ground != null ? p.ground.grip() : GameConfig.AIR_GRIP;
            p.vx += (target - p.vx) * Math.min(1f, grip * dt);
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
        p.ground = null;
        if (p.vy < 0) {
            return; // moving up: pass through one-way platforms
        }
        Aabb body = p.bounds();
        if (p.droppingThrough != null && body.top() > p.droppingThrough.y()) {
            p.droppingThrough = null; // fully below it: it is solid again
        }
        for (Platform plat : stage.platforms()) {
            if (plat == p.droppingThrough) {
                continue;
            }
            float top = plat.y();
            boolean horizontallyOver = body.right() > plat.x()
                && body.left() < plat.x() + plat.width();
            boolean crossedTop = prevBottom <= top + 1f && body.bottom() >= top;
            if (horizontallyOver && crossedTop) {
                p.y = top - GameConfig.PLAYER_HEIGHT;
                p.vy = 0;
                p.onGround = true;
                p.ground = plat;
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
                    p.takeHit(b.directionX(), b.spec);
                    b.alive = false;
                    break;
                }
            }
        }
        bullets.removeIf(b -> !b.alive);
    }

    private void checkOutOfBounds() {
        // By slot: the players list is in roster order, which is what the stage
        // hands out spawn points by.
        for (int slot = 0; slot < players.size(); slot++) {
            Player p = players.get(slot);
            if (!p.isAlive()) {
                continue;
            }
            Aabb b = p.bounds();
            boolean fell = b.top() > GameConfig.WORLD_HEIGHT + GameConfig.DEATH_MARGIN;
            boolean offSide = b.right() < -GameConfig.DEATH_MARGIN
                || b.left() > GameConfig.WORLD_WIDTH + GameConfig.DEATH_MARGIN;
            if (fell || offSide) {
                // Read before the respawn, which forgets who hit them last.
                Player killer = p.lastHitBy >= 0 ? playerById(p.lastHitBy) : null;
                // Asked now, not remembered from the start of the match:
                // the platform this slot spawns on may have moved since.
                float[] spawn = stage.spawnPoint(slot);
                p.loseLifeAndRespawn(spawn[0], spawn[1]);
                for (GameObserver o : observers) {
                    o.onKnockout(p, killer);
                }
            }
        }
    }

    private void checkGameOver() {
        GameMode.GameOverResult result = gameMode.checkGameOver(players, elapsedSeconds);
        if (result.over() && !over) {
            over = true;
            winner = result.winner();
            for (GameObserver o : observers) {
                o.onMatchOver(winner);
            }
        }
    }

    /**
     * Fire the player's weapon. The bullets it produced (a shotgun gives
     * several) join the world; an empty list means the weapon was still
     * cooling down, and nobody is told about a shot that did not happen.
     */
    public List<Bullet> fire(Player shooter) {
        List<Bullet> fired = shooter.shoot();
        if (!fired.isEmpty()) {
            bullets.addAll(fired);
            for (GameObserver o : observers) {
                o.onShot(shooter, fired);
            }
        }
        return fired;
    }

    /** Queue a sound for every client to play once. */
    public void addSound(Sound sound) {
        soundEvents.add(sound);
    }

    /**
     * Take the sounds queued since the last call and clear them. Draining is
     * the point: a sound must ride exactly one snapshot, otherwise the client
     * would replay it on every frame until something else happened.
     */
    public List<Sound> drainSounds() {
        if (soundEvents.isEmpty()) {
            return List.of();
        }

        List<Sound> drained = List.copyOf(soundEvents);
        soundEvents.clear();

        return drained;
    }
}
