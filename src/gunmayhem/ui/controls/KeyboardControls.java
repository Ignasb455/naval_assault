package gunmayhem.ui.controls;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * The native control scheme: move {@code A/D} or arrows, jump {@code W} or
 * {@code Up}, drop {@code S} or {@code Down}, shoot {@code F} or {@code L}.
 * Each action fires once per key press; held keys do not repeat it.
 */
public final class KeyboardControls extends KeyAdapter implements Controls {
    private final Set<Integer> held = new HashSet<>();
    private boolean jumpPressed;
    private boolean dropPressed;
    private boolean shootPressed;

    private static boolean isLeft(int c)  { return c == KeyEvent.VK_A || c == KeyEvent.VK_LEFT; }
    private static boolean isRight(int c) { return c == KeyEvent.VK_D || c == KeyEvent.VK_RIGHT; }
    private static boolean isJump(int c)  { return c == KeyEvent.VK_W || c == KeyEvent.VK_UP; }
    private static boolean isDrop(int c)  { return c == KeyEvent.VK_S || c == KeyEvent.VK_DOWN; }
    private static boolean isShoot(int c) { return c == KeyEvent.VK_F || c == KeyEvent.VK_L; }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        boolean isRepeat = !held.add(code);
        if (isRepeat) {
            return;
        }
        if (isJump(code)) {
            jumpPressed = true;
        } else if (isDrop(code)) {
            dropPressed = true;
        } else if (isShoot(code)) {
            shootPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        held.remove(e.getKeyCode());
    }

    /**
     * Forget every held key. Keys held when the window loses focus never
     * produce a release event, and the player would run off on their own.
     */
    public void releaseAll() {
        held.clear();
    }

    @Override
    public boolean left() {
        return held.stream().anyMatch(KeyboardControls::isLeft);
    }

    @Override
    public boolean right() {
        return held.stream().anyMatch(KeyboardControls::isRight);
    }

    @Override
    public boolean takeJump() {
        boolean pressed = jumpPressed;
        jumpPressed = false;
        return pressed;
    }

    @Override
    public boolean takeShoot() {
        boolean pressed = shootPressed;
        shootPressed = false;
        return pressed;
    }

    @Override
    public boolean takeDrop() {
        boolean pressed = dropPressed;
        dropPressed = false;
        return pressed;
    }
}
