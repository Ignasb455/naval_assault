package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/**
 * Concrete creator: the same shape as {@link ClassicStage} but every surface is
 * ice, so knockback is far harder to recover from. The ledges are narrower to
 * make the sliding count.
 */
public final class GlacierStage extends Stage {

    @Override
    public String id() {
        return "glacier";
    }

    @Override
    public String displayName() {
        return "Glacier";
    }

    @Override
    protected Platform createPlatform(int index, float x, float y, float w, float h) {
        return new IcePlatform(x, y, w, h);
    }

    @Override
    protected float[][] layout() {
        int w = GameConfig.WORLD_WIDTH;
        return new float[][] {
            { w / 2f - 280, 430, 560, 26 }, // wide center: room to slide
            { 100, 290, 160, 22 },          // left ledge
            { w - 260, 290, 160, 22 },      // right ledge
            { w / 2f - 80, 180, 160, 22 },  // top ledge
        };
    }
}
