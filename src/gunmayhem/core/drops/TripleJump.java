package gunmayhem.core.drops;

import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;

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
        player.maxJumps = GameConfig.MAX_JUMPS + 1;
        player.jumpsLeft = player.maxJumps;
    }
}
