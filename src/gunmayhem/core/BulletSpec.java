package gunmayhem.core;

/**
 * The part of a bullet that describes what kind of bullet it is: how hard it
 * hits and what colour it is drawn in. Deliberately a separate, mutable object
 * — that is what makes a shallow copy of a {@link Bullet} behave differently
 * from a deep one.
 */
public final class BulletSpec {
    public float damage;
    public float knockback;
    public int colorRgb;
    public String label;

    public BulletSpec(float damage, float knockback, int colorRgb, String label) {
        this.damage = damage;
        this.knockback = knockback;
        this.colorRgb = colorRgb;
        this.label = label;
    }

    /** Copy constructor — used by {@link Bullet#deepCopy()}. */
    public BulletSpec(BulletSpec other) {
        this(other.damage, other.knockback, other.colorRgb, other.label);
    }

    @Override
    public String toString() {
        return label + "(dmg=" + damage + ", kb=" + knockback + ")";
    }
}
