package gunmayhem.core.gamemodes;

import gunmayhem.core.Player;
import java.util.List;

/** Ends immediately when any player is eliminated. */
public final class SuddenDeath implements GameMode {
    @Override
    public void configurePlayers(List<Player> players) {
        for (Player player : players) {
            player.lives = 1;
        }
    }

    @Override
    public GameOverResult checkGameOver(List<Player> players, float elapsedSeconds) {
        boolean eliminated = false;
        Player survivor = null;
        int alive = 0;
        for (Player player : players) {
            if (player.isAlive()) {
                survivor = player;
                alive++;
            } else {
                eliminated = true;
            }
        }
        return eliminated
            ? new GameOverResult(true, alive == 1 ? survivor : null)
            : GameOverResult.stillRunning();
    }
}
