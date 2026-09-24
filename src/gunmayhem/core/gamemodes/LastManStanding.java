package gunmayhem.core.gamemodes;

import gunmayhem.core.Player;
import java.util.List;

/** The default mode: the last player with at least one life wins. */
public final class LastManStanding implements GameMode {
    @Override
    public GameOverResult checkGameOver(List<Player> players, float elapsedSeconds) {
        Player survivor = null;
        int alive = 0;
        for (Player player : players) {
            if (player.isAlive()) {
                survivor = player;
                alive++;
            }
        }
        return alive <= 1
            ? new GameOverResult(true, alive == 1 ? survivor : null)
            : GameOverResult.stillRunning();
    }
}
