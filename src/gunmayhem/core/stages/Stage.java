package gunmayhem.core.stages;

import gunmayhem.core.GameConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creator in the factory-method pattern, and the owner of what it creates. A
 * stage builds its platforms, keeps them for the length of a match, moves them
 * each tick and decides where a player may appear on them. The one thing it
 * leaves to its subclasses is which material a platform is made of.
 *
 * <p>Adding a stage therefore means adding one {@code Stage} subclass and, if
 * it needs a new surface, one {@link Platform} implementation. Nothing in
 * {@code GameWorld}, the server or the renderer has to change.
 *
 * <p>A stage holds live platform state (a moving platform's position), so one
 * instance belongs to exactly one match. Never reuse a stage across matches.
 */
public abstract class Stage {
    /** A platform at least this wide starts two players side by side. */
    private static final float TWO_PLAYER_WIDTH = 300f;

    /** How far above a platform a player appears, so they drop onto it. */
    private static final float SPAWN_DROP = 24f;

    private List<Platform> platforms;

    /**
     * This stage's platforms, built on first use. Not in a constructor: that
     * would call the subclass's factory method before the subclass is set up.
     */
    public final List<Platform> platforms() {
        if (platforms == null) {
            List<Platform> built = new ArrayList<>();
            for (float[] r : layout()) {
                built.add(createPlatform(r[0], r[1], r[2], r[3]));
            }
            platforms = Collections.unmodifiableList(built);
        }
        return platforms;
    }

    /**
     * The factory method: one platform of this stage's material at the given
     * rectangle. The arguments say where it goes, never which class to build.
     */
    protected abstract Platform createPlatform(float x, float y, float width, float height);

    /** The rectangles making up this stage, each as {x, y, width, height}. */
    protected abstract float[][] layout();

    /** Stable identifier, used in logs and when picking a stage by name. */
    public abstract String id();

    /** Human-readable name for the server log. */
    public abstract String displayName();

    /** Advance every platform by one step. Call once per tick, before anything reads them. */
    public final void update(float dt) {
        for (Platform plat : platforms()) {
            plat.update(dt);
        }
    }

    /**
     * Where the {@code slot}-th player of a match (0-based) appears, both at
     * the start and after losing a life. Worked out from where the platforms
     * are at this moment, so there is always one underneath - including on a
     * stage whose platforms move.
     *
     * <p>Platforms offer places in layout order; a wide one offers two. A
     * two-player match therefore starts on the first (main) platform and a
     * four-player match spreads onto the next ones.
     */
    public final float[] spawnPoint(int slot) {
        List<float[]> places = new ArrayList<>();
        for (Platform plat : platforms()) {
            int count = plat.width() >= TWO_PLAYER_WIDTH ? 2 : 1;
            for (int i = 1; i <= count; i++) {
                float centre = plat.x() + plat.width() * i / (count + 1);
                places.add(new float[] {
                    centre - GameConfig.PLAYER_WIDTH / 2f,
                    plat.y() - GameConfig.PLAYER_HEIGHT - SPAWN_DROP,
                });
            }
        }
        return places.get(Math.floorMod(slot, places.size()));
    }
}
