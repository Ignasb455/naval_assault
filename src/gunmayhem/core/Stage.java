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
}
