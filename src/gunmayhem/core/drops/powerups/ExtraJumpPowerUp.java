package gunmayhem.core.drops.powerups;

import gunmayhem.core.PlayerStats;

/** One more mid-air jump per layer. */
public final class ExtraJumpPowerUp extends PowerUpDecorator {
    public ExtraJumpPowerUp(PlayerStats inner) {
        super(inner);
    }

    @Override
    public int maxJumps() {
        return inner.maxJumps() + 1;
    }
}
