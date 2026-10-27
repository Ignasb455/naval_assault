package gunmayhem.net.commands;

/**
 * One change an admin makes to a running match from the server console.
 * Every command remembers what it replaced, so it can be taken back.
 */
public interface AdminCommand {

    /** Apply the change. Called once, on the game-loop thread. */
    void execute();

    /**
     * Put back what {@link #execute()} changed.
     *
     * @return false if the change can no longer be reverted (for example the
     *         crate was already picked up), true otherwise
     */
    boolean undo();

    /** Short human-readable summary, e.g. "give Ignas Shotgun". */
    String describe();
}
