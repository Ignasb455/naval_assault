package gunmayhem.net.state;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.net.GameServer;
import gunmayhem.net.StateSnapshot;

/**
 * The match is decided and the result is on screen. The world is still
 * broadcast so clients keep drawing it behind the game-over overlay, but it
 * is no longer stepped and player actions no longer reach it — which is why
 * this is its own state rather than a flag inside {@link RunningState}.
 *
 * <p>This is the only state where the rematch key means anything.
 */
public final class MatchOverState implements ServerState {
    private final GameServer server;
    private final GameWorld world;

    private boolean restartRequested;

    public MatchOverState(GameServer server, GameWorld world) {
        this.server = server;
        this.world = world;
    }

    @Override
    public String name() {
        return "MATCH_OVER";
    }

    @Override
    public boolean admitsConnections() {
        return false;
    }

    @Override
    public boolean acceptsPlayerActions() {
        return false; // nothing moves behind the game-over overlay
    }

    @Override
    public GameWorld world() {
        return world;
    }

    @Override
    public void tick(float dt) {
        if (!restartRequested) {
            return; // the result stays up until somebody asks for a rematch
        }
        restartRequested = false;
        if (server.clientCount() >= GameConfig.MIN_PLAYERS) {
            server.setState(new RunningState(server, server.createWorld("Rematch started")));
        } else {
            server.returnToLobby("not enough players for a rematch");
        }
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
        restartRequested = true;
    }
}
