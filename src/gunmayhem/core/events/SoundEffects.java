package gunmayhem.core.events;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.audio.Sound;
import java.util.List;

/**
 * Turns match events into sounds. They are queued on the world, which sends
 * each one to the clients in exactly one snapshot.
 */
public final class SoundEffects implements GameObserver {
    private final GameWorld world;

    public SoundEffects(GameWorld world) {
        this.world = world;
    }

    @Override
    public void onShot(Player shooter, List<Bullet> bullets) {
        world.addSound(Sound.GUNSHOT_GLOCK_18);
    }
}
