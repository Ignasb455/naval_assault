package gunmayhem.core.drops;

import gunmayhem.core.Aabb;
import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/**
 * What a gun does once a player is holding it. Guns are stateless — the shot
 * cooldown is counted in {@link Player} — so one instance can be shared by
 * everybody carrying that weapon.
 */
public interface Weapon {
    String name();

    /** Seconds between shots. */
    float cooldown();

    /** Bullets produced by one trigger pull. */
    List<Bullet> fire(Player owner);

    /** Where a bullet leaves the barrel: just in front of the player. */
    default float muzzleX(Player owner) {
        Aabb b = owner.bounds();
        return owner.facing > 0
            ? b.right() + 2f
            : b.left() - GameConfig.BULLET_WIDTH - 2f;
    }

    default float muzzleY(Player owner) {
        return owner.bounds().centerY() - GameConfig.BULLET_HEIGHT / 2f;
    }
}
