package gunmayhem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The static stage layout, shared by the authoritative {@link GameWorld} and by
 * clients (which need the same platforms to render, but never simulate them).
 */
public final class Stage {
    private Stage() { }

    public static List<Platform> createPlatforms() {
        int w = GameConfig.WORLD_WIDTH;
        List<Platform> platforms = new ArrayList<>();
        platforms.add(new Platform(w / 2f - 240, 430, 480, 26)); // center
        platforms.add(new Platform(90, 300, 200, 22));          // left ledge
        platforms.add(new Platform(w - 290, 300, 200, 22));     // right ledge
        platforms.add(new Platform(w / 2f - 90, 190, 180, 22)); // top ledge
        return platforms;
    }

    /**
     * Starting position for the {@code slot}-th player of a match (0-based, so a
     * two-player match uses the two center spots and a four-player match spreads
     * across the ledges). Also used as the respawn point after losing a life.
     */
    public static float[] spawnPoint(int slot) {
        int w = GameConfig.WORLD_WIDTH;
        return switch (Math.floorMod(slot, GameConfig.MAX_PLAYERS)) {
            case 0 -> new float[] { w / 2f - 200, 360 };  // above the center platform
            case 1 -> new float[] { w / 2f + 170, 360 };
            case 2 -> new float[] { 150, 230 };           // above the left ledge
            default -> new float[] { w - 200, 230 };      // above the right ledge
        };
    }
}
