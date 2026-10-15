package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/** Concrete creator: the original four-ledge layout, built entirely of solid footing. */
public final class ClassicStage extends Stage {

    @Override
    public String id() {
        return "classic";
    }

    @Override
    public String displayName() {
        return "Classic";
    }

    @Override
    protected Platform createPlatform(float x, float y, float w, float h) {
        return new SolidPlatform(x, y, w, h);
    }

    @Override
    protected float[][] layout() {
        int w = GameConfig.WORLD_WIDTH;
        return new float[][] {
            { w / 2f - 240, 430, 480, 26 }, // center
            { 90, 300, 200, 22 },           // left ledge
            { w - 290, 300, 200, 22 },      // right ledge
            { w / 2f - 90, 190, 180, 22 },  // top ledge
        };
    }
}
