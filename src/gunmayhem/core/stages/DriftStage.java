package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/**
 * Concrete creator: every platform slides back and forth. Platforms on the
 * left half start out moving right and those on the right half start out
 * moving left, so the gaps between them open and close.
 *
 * <p>Nothing here stands still, which is safe because {@link Stage} works out
 * spawn points from where the platforms currently are.
 */
public final class DriftStage extends Stage {

    @Override
    public String id() {
        return "drift";
    }

    @Override
    public String displayName() {
        return "Drift";
    }

    /** How far each platform travels either side of its starting position. */
    private static final float RANGE = 90f;
    private static final float SPEED = 60f;

    @Override
    protected Platform createPlatform(float x, float y, float w, float h) {
        boolean onLeftHalf = x + w / 2f <= GameConfig.WORLD_WIDTH / 2f;
        return new MovingPlatform(x, y, w, h, onLeftHalf ? 1f : -1f, 0f, RANGE, SPEED);
    }

    @Override
    protected float[][] layout() {
        int w = GameConfig.WORLD_WIDTH;
        // The ledges sit further in than on Classic so that RANGE of travel
        // either way still keeps them on screen.
        return new float[][] {
            { w / 2f - 240, 430, 480, 26 }, // center
            { 170, 300, 180, 22 },          // left ledge
            { w - 350, 300, 180, 22 },      // right ledge
            { w / 2f - 90, 190, 180, 22 },  // top perch
        };
    }
}
