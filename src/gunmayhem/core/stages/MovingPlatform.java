package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/**
 * Concrete product: a platform that travels back and forth along one axis and
 * carries whoever is standing on it.
 *
 * <p>The position handed to the constructor is the midpoint of the run: the
 * platform slides {@code range} pixels to one side, reverses, slides the same
 * distance to the other, and repeats. {@link #deltaX()}/{@link #deltaY()} report
 * the distance covered in the current frame, which is what {@code GameWorld}
 * adds to a rider's position.
 */
public final class MovingPlatform extends Platform {
    private final float axisX;   // unit direction, e.g. (1,0) horizontal
    private final float axisY;   // or (0,1) vertical
    private final float range;   // pixels either side of the midpoint
    private final float speed;   // pixels per second

    private float offset;        // current displacement along the axis
    private float lastOffset;
    private int direction = 1;   // +1 or -1, flipped at each end of the run

    public MovingPlatform(float x, float y, float width, float height,
                          float axisX, float axisY, float range, float speed) {
        super(x, y, width, height);
        this.axisX = axisX;
        this.axisY = axisY;
        this.range = range;
        this.speed = speed;
    }

    @Override
    public String kind() {
        return "moving";
    }

    @Override
    public float grip() {
        return GameConfig.SOLID_GRIP;
    }

    @Override public float x() { return super.x() + axisX * offset; }
    @Override public float y() { return super.y() + axisY * offset; }

    @Override public float deltaX() { return axisX * (offset - lastOffset); }
    @Override public float deltaY() { return axisY * (offset - lastOffset); }

    @Override
    public void update(float dt) {
        lastOffset = offset;
        offset += direction * speed * dt;
        if (offset > range) {
            offset = range;
            direction = -1;
        } else if (offset < -range) {
            offset = -range;
            direction = 1;
        }
    }
}
