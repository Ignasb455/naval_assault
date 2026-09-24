package gunmayhem.core.gamemodes;

import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/** Ends after a fixed duration; kills, then remaining lives, break ties. */
public final class TimeLimit implements GameMode {
    private final float durationSeconds;

    public TimeLimit() {
        this(GameConfig.TIME_LIMIT_SECONDS);
    }

    public TimeLimit(float durationSeconds) {
        if (durationSeconds <= 0f) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        this.durationSeconds = durationSeconds;
    }

    public float durationSeconds() {
        return durationSeconds;
    }

    @Override
    public GameOverResult checkGameOver(List<Player> players, float elapsedSeconds) {
        if (elapsedSeconds < durationSeconds) {
            return GameOverResult.stillRunning();
        }
        Player winner = null;
        boolean tie = false;
        for (Player player : players) {
            if (winner == null || player.kills > winner.kills
                || (player.kills == winner.kills && player.lives > winner.lives)) {
                winner = player;
                tie = false;
            } else if (player.kills == winner.kills && player.lives == winner.lives) {
                tie = true;
            }
        }
        return new GameOverResult(true, tie ? null : winner);
    }
}
