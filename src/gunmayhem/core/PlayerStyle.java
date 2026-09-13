package gunmayhem.core;

import java.awt.Color;

/**
 * Maps a player id to its display name and colour, so the server, the local
 * renderer and networked clients all agree without sending colours over the wire.
 */
public final class PlayerStyle {
    private PlayerStyle() { }

    private static final Color[] COLORS = {
        new Color(0, 191, 255),   // P1 DeepSkyBlue
        new Color(255, 99, 71),   // P2 Tomato
        new Color(80, 220, 120),  // P3 green (room to grow)
        new Color(240, 200, 60),  // P4 amber
    };

    public static String name(int id) {
        return "P" + (id + 1);
    }

    public static Color color(int id) {
        return COLORS[id % COLORS.length];
    }
}
