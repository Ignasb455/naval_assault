package gunmayhem.net.commands;

import gunmayhem.core.Player;

/** Set how many lives a player has left; undo restores the old count. */
public final class SetLivesCommand implements AdminCommand {
    private final Player player;
    private final int lives;
    private int previous;

    public SetLivesCommand(Player player, int lives) {
        if (lives < 1) {
            throw new IllegalArgumentException("lives must be at least 1");
        }
        this.player = player;
        this.lives = lives;
    }

    @Override
    public void execute() {
        previous = player.lives;
        player.lives = lives;
    }

    @Override
    public boolean undo() {
        player.lives = previous;
        return true;
    }

    @Override
    public String describe() {
        return "lives " + player.name + " " + lives;
    }
}
