package gunmayhem;

import gunmayhem.net.GameServer;
import gunmayhem.net.NetworkClient;
import gunmayhem.ui.ConnectDialog;
import gunmayhem.ui.NetworkGamePanel;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point (pure JDK — no external frameworks). All play is client/server:
 * the server owns the simulation and every player runs a client.
 * <pre>
 *   java -cp out gunmayhem.Main server [port]                 authoritative server (default port 5010)
 *   java -cp out gunmayhem.Main client &lt;host&gt; [port] [nick]   join a server
 *   java -cp out gunmayhem.Main                               join a server (asks host/port/nickname)
 * </pre>
 * When no nickname is given on the command line the join screen asks for one.
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        String mode = args.length > 0 ? args[0].toLowerCase() : "client";
        switch (mode) {
            case "server" -> {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : GameServer.DEFAULT_PORT;
                new GameServer(port).run();
            }
            case "client" -> startClient(
                args.length > 1 ? args[1] : "localhost",
                args.length > 2 ? Integer.parseInt(args[2]) : GameServer.DEFAULT_PORT,
                args.length > 3 ? args[3] : null);
            default -> {
                System.err.println("Usage:");
                System.err.println("  java -cp out gunmayhem.Main server [port]");
                System.err.println("  java -cp out gunmayhem.Main client <host> [port] [nickname]");
                System.err.println("  java -cp out gunmayhem.Main            (asks for host/nickname)");
            }
        }
    }

    /**
     * Ask for a nickname (unless one was passed on the command line), connect,
     * and open the game window. A failed connection re-opens the join screen
     * with the error rather than dumping a stack trace.
     */
    private static void startClient(String host, int port, String nickname) {
        ConnectDialog.Settings settings = new ConnectDialog.Settings(host, port, nickname);
        String error = null;
        if (nickname == null || nickname.isBlank()) {
            settings = ConnectDialog.prompt(settings, null);
        }
        while (settings != null) {
            try {
                NetworkClient client = new NetworkClient(
                    settings.host(), settings.port(), settings.nickname());
                openWindow(client, settings);
                return;
            } catch (IOException e) {
                error = "Could not reach " + settings.host() + ":" + settings.port()
                    + " (" + e.getMessage() + ")";
            }
            settings = ConnectDialog.prompt(settings, error);
        }
    }

    private static void openWindow(NetworkClient client, ConnectDialog.Settings settings) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gun Mayhem — " + settings.nickname()
                + " @ " + settings.host() + ":" + settings.port());
            NetworkGamePanel panel = new NetworkGamePanel(client);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
