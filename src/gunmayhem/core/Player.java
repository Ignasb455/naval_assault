package gunmayhem.core;

import gunmayhem.core.drops.Pistol;
import gunmayhem.core.drops.Weapon;
import gunmayhem.core.stages.Platform;
import java.awt.Color;
import java.util.List;

/**
 * A controllable character. Holds physics state (position/velocity), the
 * Gun-Mayhem-style damage/knockback meter, and the input intent set each frame
 * by the input layer. All movement resolution lives in {@link GameWorld}.
 */
public final class Player {
    public final int id;
    public final String name;
    public final Color color;

    public float x;
    public float y;
    public float vx;
    public float vy;

    public int lives = GameConfig.START_LIVES;
    public int kills;
    public int lastHitBy = -1;
    public float damage;               // grows on hits, scales knockback
    public int facing = 1;             // +1 right, -1 left

    public boolean onGround;
    public int jumpsLeft = GameConfig.MAX_JUMPS;

    // The platform currently underfoot, or null in the air. It decides how much
    // grip the surface gives and carries the player when it moves.
    public Platform ground;

    // Loot-crate state. Everything here resets when the player loses a life.
    public Weapon weapon = new Pistol();
    public float speedMultiplier = 1f;
    public int maxJumps = GameConfig.MAX_JUMPS;
    public boolean shielded;

    // Input intent for the current frame, set by the server from the owning
    // client's input messages.
    public boolean wantLeft;
    public boolean wantRight;

    private float shootTimer;

    public Player(int id, String name, Color color, float x, float y) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public boolean isAlive() {
        return lives > 0;
    }

    public Aabb bounds() {
        return new Aabb(x, y, GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT);
    }

    /** Attempt a jump. Works from the ground or as an air (double) jump. */
    public void jump() {
        if (!isAlive() || jumpsLeft <= 0) {
            return;
        }
        vy = -GameConfig.JUMP_SPEED;
        jumpsLeft--;
        onGround = false;
        ground = null;
    }

    /**
     * Fire the currently held weapon if its cooldown has elapsed. Returns the
     * bullets it produced — a pistol gives one, a shotgun a handful.
     */
    public List<Bullet> shoot() {
        if (!isAlive() || shootTimer > 0f) {
            return List.of();
        }
        shootTimer = weapon.cooldown();
        return weapon.fire(this);
    }

    /**
     * Apply knockback from a hit and bump the damage meter. How hard the hit
     * lands comes from the bullet's own spec, so a shotgun pellet stings less
     * than a pistol round.
     */
    public void takeHit(int fromDirectionX, BulletSpec spec) {
        if (shielded) {
            shielded = false; // the shield swallows exactly one hit
            return;
        }
        float impulse = spec.knockback + damage * GameConfig.KNOCKBACK_PER_DAMAGE;
        vx = fromDirectionX * impulse;
        vy = -GameConfig.KNOCKBACK_UP;
        damage += spec.damage;
    }

    /** Advance per-player timers and resolve facing from movement intent. */
    public void tick(float dt) {
        if (shootTimer > 0f) {
            shootTimer -= dt;
        }
        if (wantLeft && !wantRight) {
            facing = -1;
        } else if (wantRight && !wantLeft) {
            facing = 1;
        }
    }

    /** Drop all movement intent (used when a player's client disconnects). */
    public void clearInput() {
        wantLeft = false;
        wantRight = false;
    }

    /** Remove the player from the match for good, without a respawn. */
    public void eliminate() {
        lives = 0;
        clearInput();
        vx = 0f;
        vy = 0f;
    }

    /**
     * Lose a life and reappear at the given position. Where that is belongs to
     * the stage, which is asked at the moment of respawn; the player does not
     * remember a spawn point of its own.
     */
    public void loseLifeAndRespawn(float x, float y) {
        lives--;
        lastHitBy = -1;
        damage = 0f;
        vx = 0f;
        vy = 0f;
        this.x = x;
        this.y = y;
        onGround = false;
        ground = null;

        // Crate loot is lost on death.
        weapon = new Pistol();
        speedMultiplier = 1f;
        maxJumps = GameConfig.MAX_JUMPS;
        shielded = false;
        jumpsLeft = maxJumps;
    }
}
