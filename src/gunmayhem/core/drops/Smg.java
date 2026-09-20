package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/** Gun family, uncommon tier: one fast bullet, very short cooldown. */
public final class Smg implements UncommonDrop, Weapon {
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
        return List.of(new Bullet(muzzleX(owner), muzzleY(owner),
            GameConfig.BULLET_SPEED * 1.25f * owner.facing, owner.id));
    }
}
