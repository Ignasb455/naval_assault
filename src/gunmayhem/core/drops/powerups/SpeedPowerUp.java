package gunmayhem.core.drops.powerups;

import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;

/** Run faster. Stacked boosts multiply. */
public final class SpeedPowerUp extends PowerUpDecorator {
    public SpeedPowerUp(PowerUp inner) {
        super(inner);
    }

    @Override
    protected void addPower(Player player) {
        player.speedMultiplier *= GameConfig.SPEED_BOOST_MULTIPLIER;
    }
}
