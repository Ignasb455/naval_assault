package gunmayhem.core;

import gunmayhem.core.prototype.Prototype;

/**
 * A projectile travelling horizontally. Owned by the player who fired it so it
 * cannot hit its shooter on the frame it spawns.
 *
 * <p>Bullets are also the prototype in the pattern of the same name: each
 * weapon keeps one fully configured bullet and clones it per shot instead of
 * spelling out the same constructor arguments again and again. The clone gets
 * a fresh position and owner; everything else comes from the template.
 */
public final class Bullet implements Prototype<Bullet> {
    public float x;
    public float y;
    public float vx;
    public int ownerId;
    public boolean alive = true;

    /** What kind of bullet this is. Shared or copied, depending on the copy mode. */
    public BulletSpec spec;

    public Bullet(float x, float y, float vx, int ownerId, BulletSpec spec) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.ownerId = ownerId;
        this.spec = spec;
    }

    /** New bullet sharing this one's {@link BulletSpec} instance. */
    @Override
    public Bullet shallowCopy() {
        return new Bullet(x, y, vx, ownerId, spec);
    }

    /** New bullet with its own copy of the {@link BulletSpec}. */
    @Override
    public Bullet deepCopy() {
        return new Bullet(x, y, vx, ownerId, new BulletSpec(spec));
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
