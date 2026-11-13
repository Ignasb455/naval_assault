package gunmayhem.render.overlay;

import gunmayhem.core.GameConfig;
import gunmayhem.net.StateSnapshot;
import java.awt.Color;
import java.awt.Font;

/**
 * Refined abstraction: a band across the top saying the match has reached its
 * last stand, so the state the server is in is something a player can see
 * rather than something only the log knows.
 */
public final class LastStandOverlay extends Overlay<StateSnapshot> {
    private static final Font TITLE = new Font("SansSerif", Font.BOLD, 26);
    private static final Font HINT = new Font("Monospaced", Font.BOLD, 12);

    private static final Color BAND = new Color(150, 24, 32, 170);
    private static final Color TEXT = new Color(255, 214, 214);

    public LastStandOverlay(DrawingBackend backend) {
        super(backend);
    }

    @Override
    public void draw(StateSnapshot state) {
        backend.fillRectangle(0, 56, GameConfig.WORLD_WIDTH, 56, BAND);
        centerText("LAST STAND", 88, TITLE, TEXT);
        centerText("one life each — crates every "
            + (int) GameConfig.LAST_STAND_CRATE_INTERVAL + "s", 104, HINT, TEXT);
    }
}
