package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/**
 * Concrete creator: a solid centre with ledges that slide back and forth and a
 * top perch that rides up and down. The centre stays put on purpose — with
 * every surface moving, a respawning player has nowhere predictable to land.
 *
 * <p>This is the stage that uses the factory method's {@code index}: the same
 * creator returns three differently-configured products depending on which slot
 * of the layout is being built.
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

    @Override
    protected Platform createPlatform(int index, float x, float y, float w, float h) {
        return switch (index) {
            case 0 -> new SolidPlatform(x, y, w, h);
            // Both ledges drift sideways in step, so the gap between them and
            // the centre opens and closes as they travel.
            case 1, 2 -> new MovingPlatform(x, y, w, h, 1f, 0f, 120f, 70f);
            default -> new MovingPlatform(x, y, w, h, 0f, 1f, 60f, 55f);
        };
    }

    @Override
    protected float[][] layout() {
        int w = GameConfig.WORLD_WIDTH;
        // The ledges sit further in than on Classic so that a full 120px of
        // travel either way still keeps them on screen.
        return new float[][] {
            { w / 2f - 240, 430, 480, 26 }, // center, solid
            { 170, 300, 180, 22 },          // left ledge, drifts 50..290
            { w - 350, 300, 180, 22 },      // right ledge, drifts 490..730
            { w / 2f - 90, 190, 180, 22 },  // top perch, rides 130..250
        };
    }
}
