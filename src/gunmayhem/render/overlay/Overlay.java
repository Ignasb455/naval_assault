package gunmayhem.render.overlay;

import gunmayhem.core.GameConfig;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;

/** Bridge abstraction: overlay layout is independent of its drawing backend. */
public abstract class Overlay<T> {
    protected final DrawingBackend backend;

    protected Overlay(DrawingBackend backend) {
        this.backend = Objects.requireNonNull(backend);
    }

    public abstract void draw(T view);

    protected final void centerText(String text, int baselineY, Font font, Color color) {
        backend.drawCenteredText(text, GameConfig.WORLD_WIDTH / 2, baselineY, font, color);
    }
}
