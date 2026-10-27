package gunmayhem.core.drops;

import gunmayhem.core.Player;
import gunmayhem.core.drops.powerups.SpeedPowerUp;

/** Power-up family, common tier: run faster until you lose a life. */
public final class SpeedBoost implements CommonDrop {
    @Override
    public String name() {
        return "Speed";
    }

    @Override
    public String kind() {
        return "power";
    }

    @Override
    public void applyTo(Player player) {
        player.addPowerUp(new SpeedPowerUp(player.powerUps));
    }
}
