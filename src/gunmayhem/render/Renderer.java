package gunmayhem.render;

import gunmayhem.net.LobbyView;
import gunmayhem.net.StateSnapshot;
import java.awt.Graphics2D;

/**
 * Abstraction over how the two server-driven views are drawn: the pre-match
 * lobby with its shared countdown, and the match itself. The game core never
 * touches AWT/Swing directly; only the concrete renderer does.
 */
public interface Renderer {
    /** Draw one authoritative frame of the match. */
    void draw(Graphics2D g, StateSnapshot state);

    /** Draw the lobby: who has joined and how long until the match starts. */
    void drawLobby(Graphics2D g, LobbyView lobby, int myPlayerId);
}
