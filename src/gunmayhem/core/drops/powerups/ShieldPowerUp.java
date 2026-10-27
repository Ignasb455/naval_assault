package gunmayhem.core.drops.powerups;

import gunmayhem.core.Player;

/**
 * One more hit the player can swallow. The layer only counts shields; the
 * player keeps track of how many it has already used up.
 */
public final class ShieldPowerUp extends PowerUpDecorator {
    public ShieldPowerUp(PowerUp inner) {
        super(inner);
    }

    @Override
    protected void addPower(Player player) {
        player.shieldHits += 1;
    }
}
