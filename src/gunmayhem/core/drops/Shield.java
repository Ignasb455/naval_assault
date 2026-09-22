package gunmayhem.core.drops;

import gunmayhem.core.Player;

/** Power-up family, rare tier: swallows the next hit completely. */
public final class Shield implements RareDrop {
    @Override
    public String name() {
        return "Shield";
    }

    @Override
    public String kind() {
        return "power";
    }

    @Override
    public void applyTo(Player player) {
        player.shielded = true;
    }
}
