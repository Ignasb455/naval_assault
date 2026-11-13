package gunmayhem.net.state;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.events.GameObserver;
import gunmayhem.net.GameServer;
import gunmayhem.net.StateSnapshot;

/**
 * The same match, played once nobody has a life left to spare.
 *
 * <p>It is a state of its own rather than a flag on {@link RunningState}
 * because it plays differently: crates fall three times as often, so a player
 * who is losing has a reason to break cover instead of hiding out the clock,
 * and the clients are told so they can say as much on screen.
 *
 * <p>There is no way back. Lives only ever go down, so once this state is
 * entered the match either ends here or everybody leaves.
 */
public final class LastStandState implements ServerState, GameObserver {
    private final GameServer server;
    private final GameWorld world;

    /** Seconds until the next crate is forced down. */
    private float cratePressure = GameConfig.LAST_STAND_CRATE_INTERVAL;

    public LastStandState(GameServer server, GameWorld world) {
        this.server = server;
        this.world = world;
        world.addObserver(this);
        System.out.println("Last stand: nobody has a life to spare — crates every "
            + (int) GameConfig.LAST_STAND_CRATE_INTERVAL + "s.");
    }

    @Override
    public String name() {
        return "LAST_STAND";
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

        cratePressure -= dt;
        if (cratePressure <= 0f) {
            world.dropCrate();
            cratePressure = GameConfig.LAST_STAND_CRATE_INTERVAL;
        }
    }

    @Override
    public String broadcastJson() {
        return StateSnapshot.fromWorld(world, true).toJson();
    }

    @Override
    public void onClientsChanged() {
        if (server.clientCount() == 0) {
            server.returnToLobby("everyone disconnected");
        }
    }

    @Override
    public void onRestartRequested() {
        // The match is still being played; R only counts once it is decided.
    }

    // ---- GameObserver -------------------------------------------------------

    /** Called from inside the world's notification loop — see RunningState. */
    @Override
    public void onMatchOver(Player winner) {
        server.setState(new MatchOverState(server, world));
    }
}
