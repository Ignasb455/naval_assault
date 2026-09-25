package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.BulletSpec;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/**
 * The weapon everybody starts (and respawns) with. It is deliberately not a
 * {@link Drop}: crates hand out something better than the default.
 */
public final class Pistol implements Weapon {
    /** Prototype: one configured bullet, cloned per shot. */
    private final Bullet prototype = new Bullet(0, 0, 0, -1,
        new BulletSpec(GameConfig.PISTOL_DAMAGE, GameConfig.KNOCKBACK_BASE,
            GameConfig.BULLET_COLOR_PISTOL, "pistol"));

    @Override
    public String name() {
        return "Pistol";
    }

    @Override
    public float cooldown() {
        return 0.18f;
    }

    @Override
    public List<Bullet> fire(Player owner) {
        return List.of(aim(prototype.copy(), owner, GameConfig.BULLET_SPEED, 0f));
    }
}
