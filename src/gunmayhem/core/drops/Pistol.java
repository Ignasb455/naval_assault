package gunmayhem.core.drops;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.Player;
import java.util.List;

/**
 * The weapon everybody starts (and respawns) with. It is deliberately not a
 * {@link Drop}: crates hand out something better than the default.
 */
public final class Pistol implements Weapon {
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
        return List.of(new Bullet(muzzleX(owner), muzzleY(owner),
            GameConfig.BULLET_SPEED * owner.facing, owner.id));
    }
}
