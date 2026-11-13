package gunmayhem.net.commands;

import gunmayhem.core.stages.Stage;

/**
 * Put the stage's moving platforms back where they were when the admin last
 * ran {@code stage save}.
 *
 * <p>The command never touches the snapshot. It cannot: a platform keeps its
 * own and exposes no way to read it or pass one in. Restoring swaps what is
 * saved with what is current, so undoing is the very same call a second time
 * — the memento pattern doing the work that a hand-kept {@code previous}
 * field does in the other commands.
 */
public final class StageRewindCommand implements AdminCommand {
    private final Stage stage;
    private final int movingPlatforms;

    public StageRewindCommand(Stage stage) {
        this.stage = stage;
        this.movingPlatforms = stage.movingPlatformCount();
    }

    @Override
    public void execute() {
        stage.restore();
    }

    @Override
    public boolean undo() {
        stage.restore(); // swaps back to where the rewind found things
        return true;
    }

    @Override
    public String describe() {
        return "stage rewind (" + movingPlatforms + " moving platforms)";
    }
}
