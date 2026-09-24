package gunmayhem.core.gamemodes;

import gunmayhem.core.Player;
import java.util.List;

/** The first player to reach three credited eliminations wins. */
public final class FirstToThreeKills implements GameMode {
    public static final int KILLS_TO_WIN = 3;

    @Override
    public GameOverResult checkGameOver(List<Player> players, float elapsedSeconds) {
        for (Player player : players) {
            if (player.kills >= KILLS_TO_WIN) {
                return new GameOverResult(true, player);
            }
        }
        return GameOverResult.stillRunning();
    }
}
