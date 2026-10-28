package gunmayhem.ui.controls;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/**
 * A plain mouse, in its own terms: where the cursor is and which buttons are
 * or were pressed. It knows nothing about players, jumping or shooting, which
 * is why the game cannot use it without {@link MouseControlsAdapter}.
 */
public final class Mouse extends MouseAdapter {
    private float cursorX = Float.NaN;   // NaN while the cursor is outside the window
    private boolean leftClicked;
    private boolean rightClicked;
    private boolean middleClicked;

    /** Cursor x in window pixels, or NaN when the cursor is outside the window. */
    public float cursorX() {
        return cursorX;
    }

    /** True if the left button was pressed since the last call. */
    public boolean takeLeftClick() {
        boolean clicked = leftClicked;
        leftClicked = false;
        return clicked;
    }

    /** True if the right button was pressed since the last call. */
    public boolean takeRightClick() {
        boolean clicked = rightClicked;
        rightClicked = false;
        return clicked;
    }

    /** True if the middle button was pressed since the last call. */
    public boolean takeMiddleClick() {
        boolean clicked = middleClicked;
        middleClicked = false;
        return clicked;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        cursorX = e.getX();
        if (SwingUtilities.isLeftMouseButton(e)) {
            leftClicked = true;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            rightClicked = true;
        } else if (SwingUtilities.isMiddleMouseButton(e)) {
            middleClicked = true;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        cursorX = e.getX();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        cursorX = e.getX();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        cursorX = e.getX();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        cursorX = Float.NaN;
    }
}
