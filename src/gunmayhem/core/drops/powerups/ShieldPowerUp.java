package gunmayhem.core.drops.powerups;

import gunmayhem.core.PlayerStats;

/**
 * Swallows one hit. Once spent it passes hits on to whatever it wraps, so
 * stacked shields are used up one at a time, outermost first.
 */
public final class ShieldPowerUp extends PowerUpDecorator {
    private boolean spent;

    public ShieldPowerUp(PlayerStats inner) {
        super(inner);
    }

    @Override
    public boolean absorbHit() {
        if (!spent) {
            spent = true;
            return true;
        }
        return inner.absorbHit();
    }

    @Override
    public boolean hasShield() {
        return !spent || inner.hasShield();
    }
}
