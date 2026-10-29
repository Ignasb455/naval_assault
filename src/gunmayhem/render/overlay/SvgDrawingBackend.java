package gunmayhem.render.overlay;

import java.awt.Color;
import java.awt.Font;
import java.util.Locale;

/** Concrete implementor: collect the same primitives in an exportable SVG. */
public final class SvgDrawingBackend implements DrawingBackend {
    private final int width;
    private final int height;
    private final StringBuilder elements = new StringBuilder();

    public SvgDrawingBackend(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("SVG dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    @Override
    public void fillRectangle(int x, int y, int width, int height, Color color) {
        elements.append("<rect x=\"").append(x).append("\" y=\"").append(y)
            .append("\" width=\"").append(width).append("\" height=\"").append(height)
            .append("\"").append(paint(color)).append("/>\n");
    }

    @Override
    public void drawCenteredText(String text, int centerX, int baselineY,
                                 Font font, Color color) {
        String family = switch (font.getFamily(Locale.ROOT)) {
            case "Monospaced" -> "monospace";
            case "Serif" -> "serif";
            default -> "sans-serif";
        };
        elements.append("<text x=\"").append(centerX).append("\" y=\"").append(baselineY)
            .append("\" text-anchor=\"middle\" font-family=\"").append(family)
            .append("\" font-size=\"").append(font.getSize())
            .append("\" font-weight=\"").append(font.isBold() ? "bold" : "normal")
            .append("\" font-style=\"").append(font.isItalic() ? "italic" : "normal")
            .append("\"").append(paint(color)).append(">").append(escape(text))
            .append("</text>\n");
    }

    /** Return a complete document without changing the accumulated drawing. */
    public String toSvg() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width
            + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height
            + "\">\n" + elements + "</svg>\n";
    }

    private static String paint(Color color) {
        return " fill=\"" + String.format(Locale.ROOT, "#%06x", color.getRGB() & 0xffffff)
            + "\" fill-opacity=\"" + (color.getAlpha() / 255.0) + "\"";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
