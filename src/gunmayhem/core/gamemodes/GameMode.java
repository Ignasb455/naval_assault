package gunmayhem.core.gamemodes;

import gunmayhem.core.Player;
import java.util.List;

/** Strategy that decides when a match is over and who won. */
public interface GameMode {
    GameOverResult checkGameOver(List<Player> players, float elapsedSeconds);

    default void configurePlayers(List<Player> players) {
        // for game modes that need to modify player state
    }

    record GameOverResult(boolean over, Player winner) {
        public static GameOverResult stillRunning() {
            return new GameOverResult(false, null);
        }
    }
}
