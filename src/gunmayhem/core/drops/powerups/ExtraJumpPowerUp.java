package gunmayhem.core.drops.powerups;

import gunmayhem.core.Player;

/** One more mid-air jump per layer. */
public final class ExtraJumpPowerUp extends PowerUpDecorator {
    public ExtraJumpPowerUp(PowerUp inner) {
        super(inner);
    }

    @Override
    protected void addPower(Player player) {
        player.maxJumps += 1;
    }
}
