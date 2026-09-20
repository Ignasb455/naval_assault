package gunmayhem.core;

import gunmayhem.core.drops.Drop;

/**
 * A crate that falls from the sky, settles on the first platform it meets and
 * waits there. The player who touches it gets whatever {@link Drop} it holds.
 */
public final class LootCrate {
    public final Drop drop;

    public float x;
    public float y;
    public float vy;
    public boolean landed;
    public boolean alive = true;

    public LootCrate(Drop drop, float x, float y) {
        this.drop = drop;
        this.x = x;
        this.y = y;
    }

    public Aabb bounds() {
        return new Aabb(x, y, GameConfig.CRATE_SIZE, GameConfig.CRATE_SIZE);
    }

    /** Fall until a platform stops us; {@link GameWorld} does the landing test. */
    public void update(float dt) {
        if (landed) {
            return;
        }
        vy += GameConfig.GRAVITY * dt;
        if (vy > GameConfig.CRATE_FALL_SPEED) {
            vy = GameConfig.CRATE_FALL_SPEED;
        }
        y += vy * dt;
    }
}
