package gunmayhem.core.drops;

import gunmayhem.core.Player;
import gunmayhem.core.drops.powerups.ExtraJumpPowerUp;

/** Power-up family, uncommon tier: one extra mid-air jump. */
public final class TripleJump implements UncommonDrop {
    @Override
    public String name() {
        return "Triple jump";
    }

    @Override
    public String kind() {
        return "power";
    }

    @Override
    public void applyTo(Player player) {
        player.addPowerUp(new ExtraJumpPowerUp(player.powerUps));
        player.jumpsLeft = player.maxJumps;
    }
}
