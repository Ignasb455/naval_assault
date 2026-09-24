package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;

/**
 * Concrete product: slippery footing. The low grip means a player accelerates
 * and stops gradually instead of instantly, so they slide past the edges.
 */
public final class IcePlatform extends Platform {
    public IcePlatform(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public String kind() {
        return "ice";
    }

    @Override
    public float grip() {
        return GameConfig.ICE_GRIP;
    }
}
