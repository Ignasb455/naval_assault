package gunmayhem.render.overlay;

import java.awt.Color;
import java.awt.Font;

/** Bridge implementor: drawing primitives shared by all overlay types. */
public interface DrawingBackend {
    void fillRectangle(int x, int y, int width, int height, Color color);

    /** Draw text with its horizontal centre at centerX and baseline at baselineY. */
    void drawCenteredText(String text, int centerX, int baselineY, Font font, Color color);
}
