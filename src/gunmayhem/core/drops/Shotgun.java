package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Gun family, rare tier: a wall of pellets spread vertically, so a hit is much
 * easier to land — paid for with a long cooldown.
 */
public final class Shotgun implements RareDrop, Weapon {
    @Override
    public String name() {
        return "Shotgun";
    }

    @Override
    public String kind() {
        return "gun";
    }

    @Override
    public void applyTo(Player player) {
        player.weapon = this;
    }

    @Override
    public float cooldown() {
        return 0.55f;
    }

    @Override
    public List<Bullet> fire(Player owner) {
        List<Bullet> pellets = new ArrayList<>();
        float x = muzzleX(owner);
        float y = muzzleY(owner);
        for (int i = 0; i < GameConfig.SHOTGUN_PELLETS; i++) {
            float offset = (i - (GameConfig.SHOTGUN_PELLETS - 1) / 2f)
                * GameConfig.SHOTGUN_SPREAD;
            pellets.add(new Bullet(x, y + offset,
                GameConfig.BULLET_SPEED * 0.9f * owner.facing, owner.id));
        }
        return pellets;
    }
}
