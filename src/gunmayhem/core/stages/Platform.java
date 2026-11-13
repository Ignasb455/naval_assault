package gunmayhem.core.stages;

import gunmayhem.core.Aabb;

/**
 * Product of the {@link Stage} factory method: a one-way platform. Players rise
 * through it from below and land on the top surface; the concrete material
 * decides how much grip the surface gives and whether it travels.
 *
 * <p>
 * Only universally relevant operations belong here — the landing resolution
 * itself is identical for every material and stays in {@code GameWorld}.
 *
 * <p>
 * {@link #bounds()} is built from the accessors rather than the fields, so a
 * material that moves only has to override {@link #x()}/{@link #y()} and its
 * collision box follows automatically.
 */
public abstract class Platform {
    private final float originX;
    private final float originY;
    private final float width;
    private final float height;

    protected Platform(float x, float y, float width, float height) {
        this.originX = x;
        this.originY = y;
        this.width = width;
        this.height = height;
    }

    public float x() {
        return originX;
    }

    public float y() {
        return originY;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public Aabb bounds() {
        return new Aabb(x(), y(), width(), height());
    }

    /** Material tag: used by the renderer and sent in the snapshot. */
    public abstract String kind();

    /** How fast a player reaches their intended run speed here, per second. */
    public abstract float grip();

    /** How far the platform travelled this frame — riders are carried by this. */
    public float deltaX() {
        return 0f;
    }

    public float deltaY() {
        return 0f;
    }

    /** Advance per-platform state. Most materials have none. */
    public void update(float dt) {
    }

    /**
     * Remember where this platform is, so {@link #restore()} can bring it
     * back. Materials that stand still have nothing to remember.
     *
     * <p>Neither method takes or returns the saved state: a platform keeps its
     * own, which is what makes it impossible for a caller to restore anything
     * other than what was actually saved here.
     */
    public void save() {
    }

    /** Put back what the last {@link #save()} remembered. */
    public void restore() {
    }

    /** Whether this material moves, and so has anything to save at all. */
    public boolean travels() {
        return false;
    }
}
