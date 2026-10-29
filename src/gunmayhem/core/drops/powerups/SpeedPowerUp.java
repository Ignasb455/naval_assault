package gunmayhem.core.drops.powerups;

import gunmayhem.core.GameConfig;
import gunmayhem.core.PlayerStats;

/** Run faster. Stacked boosts multiply. */
public final class SpeedPowerUp extends PowerUpDecorator {
    public SpeedPowerUp(PlayerStats inner) {
        super(inner);
    }

    @Override
    public float speedMultiplier() {
        return inner.speedMultiplier() * GameConfig.SPEED_BOOST_MULTIPLIER;
    }
}
