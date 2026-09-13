package gunmayhem.core;

import java.awt.Color;

/**
 * Maps a player id to its display colour and fallback name, so the server and
 * every client agree without sending colours over the wire. Also owns the rules
 * for cleaning up a player-supplied nickname.
 */
public final class PlayerStyle {
    private PlayerStyle() { }

    private static final Color[] COLORS = {
        new Color(0, 191, 255),   // P1 DeepSkyBlue
        new Color(255, 99, 71),   // P2 Tomato
        new Color(80, 220, 120),  // P3 green
        new Color(240, 200, 60),  // P4 amber
    };

    /** Default label for a slot, used until (or instead of) a chosen nickname. */
    public static String name(int id) {
        return "P" + (id + 1);
    }

    public static Color color(int id) {
        return COLORS[Math.floorMod(id, COLORS.length)];
    }

    /**
     * Turn a nickname typed by a player into something safe to display and to
     * put on the wire: control characters stripped, trimmed, length-capped, and
     * falling back to the slot's default name when nothing usable is left.
     */
    public static String sanitizeName(String raw, int id) {
        if (raw == null) {
            return name(id);
        }
        StringBuilder sb = new StringBuilder();
        raw.codePoints()
            .filter(cp -> !Character.isISOControl(cp))
            .forEach(sb::appendCodePoint);
        String cleaned = sb.toString().trim();
        if (cleaned.length() > GameConfig.MAX_NAME_LENGTH) {
            cleaned = cleaned.substring(0, GameConfig.MAX_NAME_LENGTH).trim();
        }
        return cleaned.isEmpty() ? name(id) : cleaned;
    }
}
