package gunmayhem.core;

/**
 * A projectile travelling horizontally. Owned by the player who fired it so it
 * cannot hit its shooter on the frame it spawns.
 */
public final class Bullet {
    public float x;
    public float y;
    public float vx;
    public final int ownerId;
    public boolean alive = true;

    public Bullet(float x, float y, float vx, int ownerId) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.ownerId = ownerId;
    }

    public Aabb bounds() {
        return new Aabb(x, y, GameConfig.BULLET_WIDTH, GameConfig.BULLET_HEIGHT);
    }

    /** Direction of travel: +1 right, -1 left. */
    public int directionX() {
        return vx >= 0 ? 1 : -1;
    }

    public void update(float dt) {
        x += vx * dt;
        if (x < -50 || x > GameConfig.WORLD_WIDTH + 50) {
            alive = false;
        }
    }
}
