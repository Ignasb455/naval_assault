package gunmayhem.core.drops.powerups;

import gunmayhem.core.PlayerStats;

/**
 * A power-up wrapped around the player's current stats. Every call is passed
 * on to the wrapped object; subclasses override only the value they change.
 */
public abstract class PowerUpDecorator implements PlayerStats {
    protected final PlayerStats inner;

    protected PowerUpDecorator(PlayerStats inner) {
        this.inner = inner;
    }

    @Override
    public float speedMultiplier() {
        return inner.speedMultiplier();
    }

    @Override
    public int maxJumps() {
        return inner.maxJumps();
    }

    @Override
    public boolean absorbHit() {
        return inner.absorbHit();
    }

    @Override
    public boolean hasShield() {
        return inner.hasShield();
    }
}
