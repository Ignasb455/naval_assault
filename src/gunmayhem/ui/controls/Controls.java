package gunmayhem.ui.controls;

/**
 * What the game view needs from an input device, asked once per frame.
 * {@link KeyboardControls} answers it directly; any other device has to be
 * adapted to it, as {@link MouseControlsAdapter} does for the mouse.
 */
public interface Controls {
    boolean left();

    boolean right();

    /** True if a jump was asked for since the last call. */
    boolean takeJump();

    /** True if a shot was asked for since the last call. */
    boolean takeShoot();

    /** True if dropping through the platform was asked for since the last call. */
    boolean takeDrop();
}
