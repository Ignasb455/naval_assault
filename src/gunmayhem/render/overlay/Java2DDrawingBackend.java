package gunmayhem.render.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Objects;

/** Concrete implementor: draw on the caller-owned Java2D graphics context. */
public final class Java2DDrawingBackend implements DrawingBackend {
    private final Graphics2D graphics;

    public Java2DDrawingBackend(Graphics2D graphics) {
        this.graphics = Objects.requireNonNull(graphics);
    }

    @Override
    public void fillRectangle(int x, int y, int width, int height, Color color) {
        graphics.setColor(color);
        graphics.fillRect(x, y, width, height);
    }

    @Override
    public void drawCenteredText(String text, int centerX, int baselineY,
                                 Font font, Color color) {
        graphics.setFont(font);
        graphics.setColor(color);
        int width = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, centerX - (width + 1) / 2, baselineY);
    }
}
