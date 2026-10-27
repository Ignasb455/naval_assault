package gunmayhem.core.drops.powerups;

import gunmayhem.core.Player;

/**
 * A power-up layered on top of the ones collected before it. The layers
 * beneath always run first; subclasses only supply their own bonus.
 */
public abstract class PowerUpDecorator implements PowerUp {
    private final PowerUp inner;

    protected PowerUpDecorator(PowerUp inner) {
        this.inner = inner;
    }

    @Override
    public final void givePower(Player player) {
        inner.givePower(player);
        addPower(player);
    }

    /** This layer's bonus, applied after everything beneath it. */
    protected abstract void addPower(Player player);
}
