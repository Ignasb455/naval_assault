package gunmayhem.render.overlay;

import gunmayhem.core.GameConfig;
import gunmayhem.net.LobbyView;
import java.awt.Color;
import java.awt.Font;

/** Refined abstraction: the lobby's status, countdown and joining hint. */
public final class LobbyStatusOverlay extends Overlay<LobbyView> {
    private static final Font CAPTION = new Font("SansSerif", Font.BOLD, 13);
    private static final Font COUNTDOWN = new Font("SansSerif", Font.BOLD, 92);
    private static final Color MUTED = new Color(150, 156, 180);

    public LobbyStatusOverlay(DrawingBackend backend) {
        super(backend);
    }

    @Override
    public void draw(LobbyView lobby) {
        centerText(lobby.counting ? "MATCH STARTS IN" : "WAITING FOR PLAYERS",
            158, CAPTION, MUTED);
        // Use the authoritative server clock and keep the original baselines.
        int seconds = lobby.counting ? lobby.displaySeconds()
            : (int) GameConfig.LOBBY_COUNTDOWN_SECONDS;
        Color color = lobby.counting
            ? (seconds <= 5 ? new Color(255, 99, 71) : Color.WHITE)
            : new Color(255, 255, 255, 55);
        centerText(Integer.toString(seconds), 252, COUNTDOWN, color);
        centerText(hint(lobby), 290, CAPTION, MUTED);
    }

    private String hint(LobbyView lobby) {
        if (lobby.counting) {
            return lobby.seats.size() >= lobby.maxPlayers
                ? "Lobby full (" + lobby.seats.size() + "/" + lobby.maxPlayers
                    + ") \u2014 no more players can join"
                : "Others can still join until it hits zero  ("
                    + lobby.seats.size() + "/" + lobby.maxPlayers + " players)";
        }
        int missing = lobby.minPlayers - lobby.seats.size();
        return missing > 0
            ? "Need " + missing + " more player" + (missing == 1 ? "" : "s")
                + " to start the " + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + "s countdown"
            : "Starting shortly...";
    }
}
