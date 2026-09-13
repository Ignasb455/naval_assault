package gunmayhem.core;

/**
 * A static platform. Platforms are "one-way": a player can rise up through them
 * from below and land on the top surface, exactly like Gun Mayhem stages.
 */
public final class Platform {
    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public Platform(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Aabb bounds() {
        return new Aabb(x, y, width, height);
    }
}
