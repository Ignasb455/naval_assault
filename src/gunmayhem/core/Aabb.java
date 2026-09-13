package gunmayhem.core;

/**
 * Axis-aligned bounding box (top-left origin) used for all collision tests.
 */
public final class Aabb {
    public final float x;
    public final float y;
    public final float width;
    public final float height;

    public Aabb(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float left()    { return x; }
    public float right()   { return x + width; }
    public float top()     { return y; }
    public float bottom()  { return y + height; }
    public float centerX() { return x + width / 2f; }
    public float centerY() { return y + height / 2f; }

    public boolean intersects(Aabb o) {
        return left() < o.right() && right() > o.left()
            && top() < o.bottom() && bottom() > o.top();
    }
}
