package gunmayhem.net.commands;

import gunmayhem.core.stages.Stage;
import gunmayhem.core.stages.StageMemento;

/**
 * Put the stage's moving platforms back where they were when the admin last
 * ran {@code stage save}.
 *
 * <p>Undo here is the memento pattern rather than a hand-kept field: the
 * command takes a second snapshot before it restores the first, and hands
 * that one back if it is undone. It could not do this any other way — a
 * platform's travel state is private to the platform.
 */
public final class StageRewindCommand implements AdminCommand {
    private final Stage stage;
    private final StageMemento checkpoint;

    /** Where the platforms were just before this command ran. */
    private StageMemento before;

    public StageRewindCommand(Stage stage, StageMemento checkpoint) {
        this.stage = stage;
        this.checkpoint = checkpoint;
    }

    @Override
    public void execute() {
        before = stage.save();
        stage.restore(checkpoint);
    }

    @Override
    public boolean undo() {
        if (before == null) {
            return false;
        }
        stage.restore(before);
        return true;
    }

    @Override
    public String describe() {
        return "stage rewind (" + checkpoint.moving() + " moving platforms)";
    }
}
