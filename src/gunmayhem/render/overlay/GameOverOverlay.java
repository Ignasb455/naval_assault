package gunmayhem.render.overlay;

import gunmayhem.core.GameConfig;
import gunmayhem.core.PlayerStyle;
import gunmayhem.net.StateSnapshot;
import java.awt.Color;
import java.awt.Font;

/** Refined abstraction: dim the match and show its result and rematch hint. */
public final class GameOverOverlay extends Overlay<StateSnapshot> {
    private static final Font RESULT = new Font("SansSerif", Font.BOLD, 44);
    private static final Font HINT = new Font("Monospaced", Font.BOLD, 13);

    public GameOverOverlay(DrawingBackend backend) {
        super(backend);
    }

    @Override
    public void draw(StateSnapshot state) {
        backend.fillRectangle(0, 0, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT,
            new Color(0, 0, 0, 160));
        String winner = state.nameOf(state.winnerId);
        String text = state.winnerId >= 0 && !winner.isEmpty() ? winner + " WINS!" : "DRAW";
        centerText(text, GameConfig.WORLD_HEIGHT / 2 - 8, RESULT,
            state.winnerId >= 0 ? PlayerStyle.color(state.winnerId) : Color.WHITE);
        centerText("Press R for a rematch", GameConfig.WORLD_HEIGHT / 2 + 30,
            HINT, Color.WHITE);
    }
}
