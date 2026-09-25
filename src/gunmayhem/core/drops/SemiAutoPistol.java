package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.BulletSpec;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/**
 * Gun family, common tier: a faster-firing sidearm. Slower than the SMG, but a
 * clear step up from the pistol everybody starts with.
 */
public final class SemiAutoPistol implements CommonDrop, Weapon {
    private final Bullet prototype = new Bullet(0, 0, 0, -1,
        new BulletSpec(GameConfig.SEMI_DAMAGE, GameConfig.KNOCKBACK_BASE * 0.95f,
            GameConfig.BULLET_COLOR_SEMI, "semi-auto"));

    @Override
    public String name() {
        return "Semi-auto";
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
        return 0.11f;
    }

    @Override
    public List<Bullet> fire(Player owner) {
        return List.of(aim(prototype.copy(), owner, GameConfig.BULLET_SPEED * 1.1f, 0f));
    }
}
