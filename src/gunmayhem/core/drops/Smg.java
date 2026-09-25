package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.BulletSpec;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/** Gun family, uncommon tier: one fast bullet, very short cooldown. */
public final class Smg implements UncommonDrop, Weapon {
    private final Bullet prototype = new Bullet(0, 0, 0, -1,
        new BulletSpec(GameConfig.SMG_DAMAGE, GameConfig.KNOCKBACK_BASE * 0.8f,
            GameConfig.BULLET_COLOR_SMG, "smg"));

    @Override
    public String name() {
        return "SMG";
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
        return 0.07f;
    }

    @Override
    public List<Bullet> fire(Player owner) {
        return List.of(aim(prototype.copy(), owner, GameConfig.BULLET_SPEED * 1.25f, 0f));
    }
}
