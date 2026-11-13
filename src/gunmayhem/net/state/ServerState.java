package gunmayhem.net.state;

import gunmayhem.core.GameWorld;

/**
 * What the server does right now. A session moves between a lobby, a running
 * match and a decided one, and almost everything the server does differs
 * between them: whether a new client is let in, what is simulated each tick,
 * what is broadcast, and which messages from a client still mean anything.
 *
 * <p>Each state holds the data only it needs — the lobby its countdown, a
 * match its world — so the server no longer carries fields that are dead for
 * most of a session, nor asks which phase it is in before every decision.
 *
 * <p>Every method here is called on the game-loop thread while it holds the
 * server's client lock, except {@link #admitsConnections()}, which the accept
 * thread calls under that same lock.
 */
public interface ServerState {

    /** Short name for logs and the state demo, e.g. {@code LOBBY}. */
    String name();

    /** Whether a client connecting right now is let in or turned away. */
    boolean admitsConnections();

    /** Whether jump, drop and shoot still do anything. */
    boolean acceptsPlayerActions();

    /** The match being played, or null in the lobby. */
    GameWorld world();

    /** Advance whatever this state is responsible for. */
    void tick(float dt);

    /** One line of JSON sent to every client this tick. */
    String broadcastJson();

    /** Somebody joined or left. */
    void onClientsChanged();

    /** A client pressed the rematch key. */
    void onRestartRequested();
}
