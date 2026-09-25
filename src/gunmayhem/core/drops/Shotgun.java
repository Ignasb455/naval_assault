package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.BulletSpec;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * Gun family, rare tier: a wall of pellets spread vertically, so a hit is much
 * easier to land — paid for with a long cooldown. Each pellet is a clone of the
 * same template, which is where the prototype pattern earns its keep.
 */
public final class Shotgun implements RareDrop, Weapon {
    private final Bullet prototype = new Bullet(0, 0, 0, -1,
        new BulletSpec(GameConfig.PELLET_DAMAGE, GameConfig.KNOCKBACK_BASE * 0.7f,
            GameConfig.BULLET_COLOR_PELLET, "pellet"));

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
        for (int i = 0; i < GameConfig.SHOTGUN_PELLETS; i++) {
            float offset = (i - (GameConfig.SHOTGUN_PELLETS - 1) / 2f)
                * GameConfig.SHOTGUN_SPREAD;
            pellets.add(aim(prototype.copy(), owner, GameConfig.BULLET_SPEED * 0.9f, offset));
        }
        return pellets;
    }

    /** The template itself, for the prototype demo. */
    public Bullet prototype() {
        return prototype;
    }
}
