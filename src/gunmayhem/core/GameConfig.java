package gunmayhem.core;

/**
 * Central tuning values for the demo. Units are pixels and seconds.
 * Keeping them in one place makes the physics easy to tweak.
 */
public final class GameConfig {
    private GameConfig() { }

    // World size (logical pixels).
    public static final int WORLD_WIDTH = 960;
    public static final int WORLD_HEIGHT = 600;

    // Physics.
    public static final float GRAVITY = 2000f;       // downward acceleration
    public static final float MOVE_SPEED = 320f;      // horizontal run speed
    public static final float JUMP_SPEED = 720f;      // initial upward velocity on a jump
    public static final int MAX_JUMPS = 2;            // ground jump + one air jump
    public static final float MAX_FALL_SPEED = 1400f; // terminal velocity

    // Player body.
    public static final float PLAYER_WIDTH = 34f;
    public static final float PLAYER_HEIGHT = 46f;
    public static final int START_LIVES = 3;
    public static final float TIME_LIMIT_SECONDS = 180f;

    // Shooting.
    public static final float BULLET_SPEED = 900f;
    public static final float BULLET_WIDTH = 12f;
    public static final float BULLET_HEIGHT = 5f;
    public static final float SHOOT_COOLDOWN = 0.18f; // seconds between shots

    // Knockback: like a Smash-style damage meter, hits push harder as damage grows.
    // Kept above MOVE_SPEED so even a first hit overpowers the victim's own
    // movement input and visibly throws them (below it, control cancels the hit).
    public static final float KNOCKBACK_BASE = 420f;       // horizontal impulse at 0% damage
    public static final float KNOCKBACK_PER_DAMAGE = 14f;  // extra impulse per damage point
    public static final float KNOCKBACK_UP = 260f;         // small upward pop on hit
    public static final float DAMAGE_PER_HIT = 8f;

    // How far past a world edge a player falls before losing a life.
    public static final float DEATH_MARGIN = 120f;

    // Per-second horizontal decay applied while being knocked back.
    public static final float KNOCKBACK_FRICTION = 3.5f;

    // Lobby: the match starts once MIN_PLAYERS are connected and the countdown
    // expires; late joiners are welcome until then, up to MAX_PLAYERS.
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    public static final float LOBBY_COUNTDOWN_SECONDS = 15f;

    // Loot crates: one falls every CRATE_INTERVAL seconds, lands on the first
    // platform below it and waits to be picked up.
    public static final float CRATE_INTERVAL = 12f;
    public static final float CRATE_SIZE = 28f;
    public static final float CRATE_FALL_SPEED = 260f;

    // Drop effects. They last until the player loses a life.
    public static final float SPEED_BOOST_MULTIPLIER = 1.6f;
    public static final int SHOTGUN_PELLETS = 3;
    public static final float SHOTGUN_SPREAD = 16f; // vertical gap between pellets

    // Nicknames are player-supplied, so they are length-capped before display.
    public static final int MAX_NAME_LENGTH = 12;
}
