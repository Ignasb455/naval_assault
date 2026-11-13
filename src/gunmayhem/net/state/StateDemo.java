package gunmayhem.net.state;

import gunmayhem.core.gamemodes.TimeLimit;
import gunmayhem.net.GameServer;
import gunmayhem.net.NetworkClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a real server with headless clients and prints the state it is in as
 * it moves between them. Run with {@code java -cp out gunmayhem.Main state}.
 *
 * <p>Nothing here drives the transitions by hand: clients connect and leave,
 * the clock runs out, and the states hand over to each other.
 */
public final class StateDemo {
    private static final int PORT = 5099;

    /** Short match so the demo reaches the decided state without a fight. */
    private static final float MATCH_SECONDS = 3f;

    private StateDemo() { }

    public static void run() throws InterruptedException {
        System.out.println("Starting a server on port " + PORT
            + " with a " + (int) MATCH_SECONDS + "s time limit.");
        System.out.println("This takes about half a minute: the lobby countdown"
            + " alone is 15s.");
        System.out.println();

        GameServer server = new GameServer(PORT, new TimeLimit(MATCH_SECONDS), "classic");
        Thread serverThread = new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                System.out.println("server stopped: " + e.getMessage());
            }
        }, "state-demo-server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        report(server, "no clients yet");

        List<NetworkClient> clients = new ArrayList<>();
        clients.add(connect("P0"));
        Thread.sleep(500);
        report(server, "one client: too few to start");

        clients.add(connect("P1"));
        Thread.sleep(500);
        report(server, "two clients: the lobby starts counting");

        System.out.println();
        System.out.println("...waiting out the lobby countdown...");
        waitFor(server, "RUNNING", 20);
        report(server, "countdown reached zero");

        NetworkClient latecomer = connect("P2");
        Thread.sleep(500);
        System.out.println("  a third client tried to join: "
            + (latecomer.rejectionMessage() == null
                ? "let in (unexpected)" : "turned away — " + latecomer.rejectionMessage()));
        latecomer.close();

        System.out.println();
        System.out.println("An admin puts everybody on their last life:");
        server.admin().submit("group laststand");
        server.admin().submit("lives 0 1");
        server.admin().submit("lives 1 1");
        server.admin().submit("group end");
        waitFor(server, "LAST_STAND", 10);
        report(server, "nobody has a life to spare");
        System.out.println("  clients are told: lastStand = "
            + (clients.get(0).latest() != null && clients.get(0).latest().lastStand));

        System.out.println();
        System.out.println("...waiting out the match...");
        waitFor(server, "MATCH_OVER", 15);
        report(server, "the time limit decided it");

        System.out.println();
        System.out.println("A client asks for a rematch:");
        clients.get(0).sendRestart();
        waitFor(server, "RUNNING", 10);
        report(server, "rematch");

        System.out.println();
        System.out.println("Both clients leave:");
        for (NetworkClient client : clients) {
            client.close();
        }
        waitFor(server, "LOBBY", 10);
        report(server, "nobody left to play");

        System.out.println();
        System.out.println("LOBBY -> RUNNING -> LAST_STAND -> MATCH_OVER -> RUNNING"
            + " -> LOBBY, and each step was the state's own decision.");
    }

    private static NetworkClient connect(String name) {
        try {
            return new NetworkClient("localhost", PORT, name);
        } catch (Exception e) {
            throw new IllegalStateException("could not connect " + name, e);
        }
    }

    /** Poll the server until it reports the wanted state, or give up. */
    private static void waitFor(GameServer server, String wanted, int seconds)
            throws InterruptedException {
        for (int i = 0; i < seconds * 10; i++) {
            if (server.stateName().equals(wanted)) {
                return;
            }
            Thread.sleep(100);
        }
        System.out.println("  (gave up waiting for " + wanted + ")");
    }

    private static void report(GameServer server, String why) {
        System.out.printf("  %-12s %s%n", server.stateName(), "<- " + why);
    }
}
