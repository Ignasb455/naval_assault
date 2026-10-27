package gunmayhem.net.commands;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Runs commands and remembers them, newest first, so they can be undone. */
public final class CommandHistory {
    private final Deque<AdminCommand> done = new ArrayDeque<>();

    /** What {@link #undo()} took off the history and whether it could revert it. */
    public record Undone(AdminCommand command, boolean reverted) { }

    public void run(AdminCommand command) {
        command.execute();
        done.push(command);
    }

    /** Take back the newest command, or return null if the history is empty. */
    public Undone undo() {
        AdminCommand last = done.poll();
        return last == null ? null : new Undone(last, last.undo());
    }

    /** Commands still undoable, newest first. */
    public List<AdminCommand> entries() {
        return List.copyOf(done);
    }

    public void clear() {
        done.clear();
    }
}
