package gunmayhem.core;

/**
 * A player's stats with no power-ups: normal speed, the normal number of
 * jumps, no shield. Every power-up chain starts from one of these.
 */
public final class BaseStats implements PlayerStats {
    @Override
    public float speedMultiplier() {
        return 1f;
    }

    @Override
    public int maxJumps() {
        return GameConfig.MAX_JUMPS;
    }

    @Override
    public boolean absorbHit() {
        return false;
    }

    @Override
    public boolean hasShield() {
        return false;
    }
}
