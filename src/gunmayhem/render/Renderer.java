package gunmayhem.render;

import gunmayhem.net.StateSnapshot;
import java.awt.Graphics2D;

/**
 * Abstraction over how a {@link StateSnapshot} is drawn. The game core never
 * touches AWT/Swing directly; only the concrete renderer does. Drawing from a
 * snapshot means the same renderer serves both local play and networked clients.
 */
public interface Renderer {
    void draw(Graphics2D g, StateSnapshot state);
}
