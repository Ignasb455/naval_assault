package gunmayhem.core.events;

import gunmayhem.core.Player;

/**
 * Credits a knockout to the player who hit the victim last. The kill-based
 * game modes (first to three kills, time limit) read the resulting counts.
 */
public final class KillTracker implements GameObserver {

    @Override
    public void onKnockout(Player victim, Player killer) {
        if (killer != null && killer != victim) {
            killer.kills++;
        }
    }
}
