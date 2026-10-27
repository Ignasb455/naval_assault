package gunmayhem.core.events;

import gunmayhem.core.Player;
import gunmayhem.core.drops.Drop;
import java.io.PrintStream;

/**
 * Narrates the match on the server console: knockouts, crate pickups and the
 * result. Shots are left out, there are far too many of them to be useful.
 */
public final class MatchLog implements GameObserver {
    private final PrintStream out;

    public MatchLog(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onKnockout(Player victim, Player killer) {
        String what = killer != null
            ? killer.name + " knocked out " + victim.name
            : victim.name + " fell off";
        String left = victim.isAlive()
            ? victim.lives + (victim.lives == 1 ? " life" : " lives") + " left"
            : "out of the match";
        out.println(what + " (" + victim.name + ": " + left + ")");
    }

    @Override
    public void onCratePicked(Player player, Drop drop) {
        out.println(player.name + " picked up " + drop.name()
            + " (" + drop.rarity().wireName() + ")");
    }

    @Override
    public void onMatchOver(Player winner) {
        out.println(winner != null ? winner.name + " wins!" : "Match ended in a draw.");
    }
}
