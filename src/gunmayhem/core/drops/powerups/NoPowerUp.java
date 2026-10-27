package gunmayhem.core.drops.powerups;

import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;

/**
 * Bottom of every stack: puts the stats back to normal. Because it resets
 * them, the whole stack can be re-run safely without a bonus counting twice.
 */
public final class NoPowerUp implements PowerUp {
    @Override
    public void givePower(Player player) {
        player.speedMultiplier = 1f;
        player.maxJumps = GameConfig.MAX_JUMPS;
        player.shieldHits = 0;
    }
}
