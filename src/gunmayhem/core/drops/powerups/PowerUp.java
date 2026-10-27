package gunmayhem.core.drops.powerups;

import gunmayhem.core.Player;

/**
 * The lasting effects a player has collected, kept as a stack of decorators.
 * Running the stack works the player's stats out from scratch: the bottom
 * layer ({@link NoPowerUp}) resets them, and every layer above adds its bonus.
 * It is run on pickup and on respawn, never per tick.
 */
public interface PowerUp {
    void givePower(Player player);
}
