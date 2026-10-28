package gunmayhem.ui.controls;

import gunmayhem.core.GameConfig;
import java.util.function.DoubleSupplier;

/**
 * Lets a {@link Mouse} act as {@link Controls}. Left click shoots, right click
 * jumps, middle click drops through the platform, and the player runs towards
 * the side of the screen the cursor is on. Like the keyboard, every action is
 * one per click; holding a button does not repeat it.
 */
public final class MouseControlsAdapter implements Controls {
    // Cursor this close to the player's centre means "stand still"; without
    // it the player could never stop and would jitter around the cursor.
    private static final float DEAD_ZONE = GameConfig.PLAYER_WIDTH / 2f;

    private final Mouse mouse;
    private final DoubleSupplier playerCenterX;   // NaN when there is no player to steer

    public MouseControlsAdapter(Mouse mouse, DoubleSupplier playerCenterX) {
        this.mouse = mouse;
        this.playerCenterX = playerCenterX;
    }

    // An unknown cursor or player position is NaN, and every comparison with
    // NaN is false: the player simply stands still.

    @Override
    public boolean left() {
        return mouse.cursorX() < playerCenterX.getAsDouble() - DEAD_ZONE;
    }

    @Override
    public boolean right() {
        return mouse.cursorX() > playerCenterX.getAsDouble() + DEAD_ZONE;
    }

    @Override
    public boolean takeJump() {
        return mouse.takeRightClick();
    }

    @Override
    public boolean takeShoot() {
        return mouse.takeLeftClick();
    }

    @Override
    public boolean takeDrop() {
        return mouse.takeMiddleClick();
    }
}
