package gunmayhem.net.state;

import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.net.GameServer;

/**
 * Waiting for players. This is the only state that lets anyone in, and the
 * countdown it runs belongs to it alone — the server used to carry those two
 * fields through a whole match that never read them.
 */
public final class LobbyState implements ServerState {
    private final GameServer server;

    private boolean counting;
    private float countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;

    public LobbyState(GameServer server) {
        this.server = server;
    }

    @Override
    public String name() {
        return "LOBBY";
    }

    @Override
    public boolean admitsConnections() {
        return true;
    }

    @Override
    public boolean acceptsPlayerActions() {
        return false; // nobody has a character yet
    }

    @Override
    public GameWorld world() {
        return null;
    }

    @Override
    public void tick(float dt) {
        // Checked every tick rather than only when somebody joins, so a player
        // leaving mid-countdown cancels it on the same frame they disappear.
        int joined = server.clientCount();
        if (!counting && joined >= GameConfig.MIN_PLAYERS) {
            counting = true;
            countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
            System.out.println(joined + " players connected — starting in "
                + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + "s.");
        } else if (counting && joined < GameConfig.MIN_PLAYERS) {
            counting = false;
            countdown = GameConfig.LOBBY_COUNTDOWN_SECONDS;
            System.out.println("Countdown cancelled — waiting for at least "
                + GameConfig.MIN_PLAYERS + " players.");
        }
        if (counting) {
            countdown -= dt;
            if (countdown <= 0f) {
                server.setState(new RunningState(server,
                    server.createWorld("Match started")));
                System.out.println("Further connections are now rejected"
                    + " until the match ends.");
            }
        }
    }

    @Override
    public String broadcastJson() {
        return server.lobbyView(counting, Math.max(0f, countdown)).toJson();
    }

    @Override
    public void onClientsChanged() {
        // tick() re-reads the roster anyway, so there is nothing to do here.
    }

    @Override
    public void onRestartRequested() {
        // No match to restart.
    }
}
