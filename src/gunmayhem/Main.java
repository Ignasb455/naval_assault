package gunmayhem;

import gunmayhem.net.GameServer;
import gunmayhem.net.NetworkClient;
import gunmayhem.ui.GamePanel;
import gunmayhem.ui.NetworkGamePanel;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Entry point with three modes (pure JDK — no external frameworks):
 * <pre>
 *   java -cp out gunmayhem.Main                     local hot-seat (2 players, 1 keyboard)
 *   java -cp out gunmayhem.Main server [port]       authoritative server (default port 5010)
 *   java -cp out gunmayhem.Main client host [port]  connect to a server on the LAN
 * </pre>
 */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        String mode = args.length > 0 ? args[0].toLowerCase() : "local";
        switch (mode) {
            case "server" -> {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : GameServer.DEFAULT_PORT;
                new GameServer(port).run();
            }
            case "client" -> {
                if (args.length < 2) {
                    System.err.println("Usage: java -cp out gunmayhem.Main client <host> [port]");
                    return;
                }
                String host = args[1];
                int port = args.length > 2 ? Integer.parseInt(args[2]) : GameServer.DEFAULT_PORT;
                startClient(host, port);
            }
            default -> startLocal();
        }
    }

    private static void startLocal() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gun Mayhem — local (Java2D)");
            GamePanel panel = new GamePanel();
            showFrame(frame, panel);
            panel.requestFocusInWindow();
        });
    }

    private static void startClient(String host, int port) throws IOException {
        NetworkClient client = new NetworkClient(host, port);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gun Mayhem — client @ " + host + ":" + port);
            NetworkGamePanel panel = new NetworkGamePanel(client);
            showFrame(frame, panel);
            panel.requestFocusInWindow();
        });
    }

    private static void showFrame(JFrame frame, javax.swing.JPanel panel) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
