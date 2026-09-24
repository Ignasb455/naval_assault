package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/** Concrete product: ordinary footing — full grip, never moves. */
public final class SolidPlatform extends Platform {
    public SolidPlatform(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public String kind() {
        return "solid";
    }

    @Override
    public float grip() {
        return GameConfig.SOLID_GRIP;
    }
}
