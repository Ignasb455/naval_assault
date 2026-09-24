package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Creator in the factory-method pattern. A stage owns the real work — the
 * layout geometry and the spawn points, shared by every stage — and defers the
 * one decision it cannot make itself to its subclasses: which material each
 * platform is built from.
 *
 * <p>Adding a stage therefore means adding one {@code Stage} subclass and, if
 * it needs a new surface, one {@link Platform} implementation. Nothing in
 * {@code GameWorld}, the server or the renderer has to change.
 */
public abstract class Stage {

    /**
     * Core business logic: lay the stage out, whatever it is built from. The
     * concrete platform classes are never named here.
     */
    public final List<Platform> createPlatforms() {
        List<Platform> out = new ArrayList<>();
        float[][] rows = layout();
        for (int i = 0; i < rows.length; i++) {
            float[] r = rows[i];
            out.add(createPlatform(i, r[0], r[1], r[2], r[3]));
        }
        return out;
    }

    /**
     * The factory method. {@code index} is the platform's position in
     * {@link #layout()}, so a stage can mix materials — a solid centre with
     * drifting ledges, say.
     */
    protected abstract Platform createPlatform(int index, float x, float y,
                                               float width, float height);

    /** The rectangles making up this stage, each as {x, y, width, height}. */
    protected abstract float[][] layout();

    /** Stable identifier, used in logs and when picking a stage by name. */
    public abstract String id();

    /** Human-readable name for the server log. */
    public abstract String displayName();

    /**
     * Starting position for the {@code slot}-th player of a match (0-based, so a
     * two-player match uses the two center spots and a four-player match spreads
     * across the ledges). Also used as the respawn point after losing a life.
     */
    public float[] spawnPoint(int slot) {
        int w = GameConfig.WORLD_WIDTH;
        return switch (Math.floorMod(slot, GameConfig.MAX_PLAYERS)) {
            case 0 -> new float[] { w / 2f - 200, 360 };  // above the center platform
            case 1 -> new float[] { w / 2f + 170, 360 };
            case 2 -> new float[] { 150, 230 };           // above the left ledge
            default -> new float[] { w - 200, 230 };      // above the right ledge
        };
    }
}
