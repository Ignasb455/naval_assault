package gunmayhem.core.drops;

import gunmayhem.core.Player;

/**
 * Something a player can pick up out of a loot crate. Every product of every
 * {@link DropFactory} is a {@code Drop}, so the world can carry them around
 * without knowing whether it holds a gun or a power-up.
 */
public interface Drop {
    /** Label shown in the HUD, e.g. "Shotgun" or "Speed". */
    String name();

    /** Which family this came from: {@code "gun"} or {@code "power"}. */
    String kind();

    Rarity rarity();

    /** Apply the effect to whoever touched the crate. */
    void applyTo(Player player);
}
