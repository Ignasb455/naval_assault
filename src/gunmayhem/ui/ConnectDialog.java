package gunmayhem.ui;

import gunmayhem.core.GameConfig;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * The join screen: asks which server to connect to and what nickname the player
 * wants to use. Shown before the socket is opened, so the nickname can travel
 * with the very first message the client sends.
 */
public final class ConnectDialog {
    private ConnectDialog() { }

    /** What the player typed on the join screen. */
    public record Settings(String host, int port, String nickname) { }

    /** Show the dialog (from any thread) and return the settings, or null if cancelled. */
    public static Settings prompt(Settings initial, String errorMessage) {
        if (SwingUtilities.isEventDispatchThread()) {
            return show(initial, errorMessage);
        }
        final Settings[] result = new Settings[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = show(initial, errorMessage));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
        return result[0];
    }

    private static Settings show(Settings initial, String errorMessage) {
        JTextField host = new JTextField(initial.host(), 16);
        JTextField port = new JTextField(Integer.toString(initial.port()), 6);
        JTextField nick = new JTextField(
            initial.nickname() == null ? "" : initial.nickname(), 16);

        // Put the caret in the nickname box as soon as the modal dialog appears.
        nick.addAncestorListener(new AncestorListener() {
            @Override public void ancestorAdded(AncestorEvent e) {
                nick.requestFocusInWindow();
                nick.selectAll();
            }
            @Override public void ancestorRemoved(AncestorEvent e) { }
            @Override public void ancestorMoved(AncestorEvent e) { }
        });

        JLabel message = new JLabel();
        message.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel fields = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.add(new JLabel("Nickname:"));
        fields.add(nick);
        fields.add(new JLabel("Server host:"));
        fields.add(host);
        fields.add(new JLabel("Port:"));
        fields.add(port);

        JLabel hint = new JLabel("<html><br>The match starts "
            + (int) GameConfig.LOBBY_COUNTDOWN_SECONDS + " seconds after the "
            + GameConfig.MIN_PLAYERS + "nd player connects.<br>Up to "
            + GameConfig.MAX_PLAYERS + " players; nobody may join once it begins.</html>");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(new Color(90, 95, 110));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(message, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(hint, BorderLayout.SOUTH);

        String error = errorMessage;
        while (true) {
            message.setText(error == null ? " "
                : "<html><font color='#b00020'>" + escape(error) + "</font></html>");

            int choice = JOptionPane.showConfirmDialog(null, panel,
                "Join a Gun Mayhem server", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }

            String chosenName = nick.getText().trim();
            if (chosenName.isEmpty()) {
                error = "Please choose a nickname.";
                continue;
            }
            if (chosenName.length() > GameConfig.MAX_NAME_LENGTH) {
                chosenName = chosenName.substring(0, GameConfig.MAX_NAME_LENGTH).trim();
                nick.setText(chosenName); // show the player exactly what will be used
            }

            String chosenHost = host.getText().trim();
            if (chosenHost.isEmpty()) {
                error = "Please enter the server host.";
                continue;
            }

            int chosenPort;
            try {
                chosenPort = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException e) {
                error = "The port must be a number.";
                continue;
            }
            if (chosenPort < 1 || chosenPort > 65535) {
                error = "The port must be between 1 and 65535.";
                continue;
            }
            return new Settings(chosenHost, chosenPort, chosenName);
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
