package gunmayhem.net.state;

import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.events.GameObserver;
import gunmayhem.net.GameServer;
import gunmayhem.net.StateSnapshot;

/**
 * A match in progress. Owns the world it is simulating, turns new clients
 * away, and hands over as soon as the game mode declares a winner.
 *
 * <p>That handover is not polled: the state registers itself as a {@link
 * GameObserver} and moves on when the world announces the result, on the tick
 * it happens.
 */
public final class RunningState implements ServerState, GameObserver {
    private final GameServer server;
    private final GameWorld world;

    public RunningState(GameServer server, GameWorld world) {
        this.server = server;
        this.world = world;
        world.addObserver(this);
    }

    @Override
    public String name() {
        return "RUNNING";
    }

    @Override
    public boolean admitsConnections() {
        return false;
    }

    @Override
    public boolean acceptsPlayerActions() {
        return true;
    }

    @Override
    public GameWorld world() {
        return world;
    }

    @Override
    public void tick(float dt) {
        world.update(dt);

        // Checked after the step, and only while the match is still open: if
        // that update decided it, onMatchOver has already moved us on.
        if (!world.isOver() && nobodyHasALifeToSpare()) {
            server.setState(new LastStandState(server, world));
        }
    }

    /**
     * True once every player still in the match is on their last life. Takes
     * two or more of them — with one left the match is about to end anyway,
     * and there would be nobody to make it tense for.
     */
    private boolean nobodyHasALifeToSpare() {
        int standing = 0;
        for (Player player : world.players()) {
            if (!player.isAlive()) {
                continue;
            }
            if (player.lives > 1) {
                return false;
            }
            standing++;
        }
        return standing >= 2;
    }

    @Override
    public String broadcastJson() {
        return StateSnapshot.fromWorld(world).toJson();
    }

    @Override
    public void onClientsChanged() {
        if (server.clientCount() == 0) {
            server.returnToLobby("everyone disconnected");
        }
    }

    @Override
    public void onRestartRequested() {
        // The match is still being played; R only means something once it is
        // decided, and MatchOverState is what answers then.
    }

    // ---- GameObserver -------------------------------------------------------

    /**
     * Called from inside the world's own notification loop, so this must not
     * touch the observer list — the world fires this exactly once per match
     * and is replaced wholesale on a rematch, which makes unregistering both
     * unnecessary and unsafe.
     */
    @Override
    public void onMatchOver(Player winner) {
        server.setState(new MatchOverState(server, world));
    }
}
