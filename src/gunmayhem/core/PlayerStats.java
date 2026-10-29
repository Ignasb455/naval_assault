package gunmayhem.core;

/**
 * The values crate power-ups change. {@link BaseStats} answers with the
 * normal values; each power-up wraps whatever the player had before it and
 * adjusts the answer on the way out, so the effects stack. {@link Player}
 * holds the outermost object and asks it.
 */
public interface PlayerStats {
    float speedMultiplier();

    int maxJumps();

    /** Called on every hit: true if a shield swallowed it. */
    boolean absorbHit();

    /** True while a shield is still waiting for a hit. */
    boolean hasShield();
}
