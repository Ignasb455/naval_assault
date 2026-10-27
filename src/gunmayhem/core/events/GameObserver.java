package gunmayhem.core.events;

import gunmayhem.core.Bullet;
import gunmayhem.core.Player;
import gunmayhem.core.drops.Drop;
import java.util.List;

/**
 * Something that wants to hear what happens in a match. {@link
 * gunmayhem.core.GameWorld} is the subject: it calls these methods on every
 * registered observer, in registration order, at the moment the event happens.
 *
 * <p>Every method has an empty default, so an observer implements only the
 * events it cares about.
 */
public interface GameObserver {

    /** A player fired; {@code bullets} is what the weapon produced. */
    default void onShot(Player shooter, List<Bullet> bullets) { }

    /**
     * A player left the arena and lost a life. {@code killer} is whoever hit
     * them last, or null if they fell on their own.
     */
    default void onKnockout(Player victim, Player killer) { }

    /** A player walked into a crate and got its contents. */
    default void onCratePicked(Player player, Drop drop) { }

    /** The game mode declared the match over; {@code winner} is null on a draw. */
    default void onMatchOver(Player winner) { }
}
