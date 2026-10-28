package gunmayhem.client;

import gunmayhem.core.GameConfig;
import gunmayhem.core.audio.AudioManager;
import gunmayhem.core.audio.Music;
import gunmayhem.net.NetworkClient;
import gunmayhem.ui.ConnectDialog;
import gunmayhem.ui.NetworkGamePanel;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Facade over everything a player client is made of. Starting one means asking
 * for a server and a nickname, opening a socket, building a Swing window around
 * it and starting the music — in that order, undone in reverse. Callers say
 * {@code start()} and need to know none of it.
 *
 * <p>The subsystems it drives — {@link ConnectDialog}, {@link NetworkClient},
 * {@link NetworkGamePanel}, {@link AudioManager} and {@link JFrame} — know
 * nothing about this class and can still be used directly.
 */
public final class GameClientFacade {

    /**
     * Windows still open across the whole JVM. The music is one shared clip, so
     * it may only be stopped once the last session is gone — otherwise closing
     * one window of several would silence the rest.
     */
    private static final AtomicInteger LIVE_SESSIONS = new AtomicInteger();

    private ConnectDialog.Settings settings;

    private NetworkClient client;
    private NetworkGamePanel panel;
    private JFrame frame;
    private Point windowAt;

    /** shutdown() can arrive twice: from the window, and from a shutdown hook. */
    private boolean closed;

    public GameClientFacade(String host, int port, String nickname) {
        // Keyboard unless the join screen is shown and the player picks the mouse.
        this.settings = new ConnectDialog.Settings(host, port, nickname, false);
    }

    /** Place the window here instead of centring it. Used when tiling several. */
    public void placeAt(int x, int y) {
        this.windowAt = new Point(x, y);
    }

    /**
     * The whole sequence: ask for a nickname unless one was given, connect, and
     * open the window. A failed connection re-opens the join screen with the
     * error rather than dumping a stack trace. Returns false if the player
     * cancelled.
     */
    public boolean start() {
        if (settings.nickname() == null || settings.nickname().isBlank()) {
            settings = ConnectDialog.prompt(settings, null);
        }
        String error = null;
        while (settings != null) {
            error = connect();
            if (error == null) {
                openWindow();
                playMusic();
                return true;
            }
            settings = ConnectDialog.prompt(settings, error);
        }
        return false;
    }

    /** Subsystem step: open the socket. Returns an error message, or null. */
    public String connect() {
        try {
            client = new NetworkClient(settings.host(), settings.port(), settings.nickname());
            return null;
        } catch (IOException e) {
            return "Could not reach " + settings.host() + ":" + settings.port()
                + " (" + e.getMessage() + ")";
        }
    }

    private void onSwingThread(Runnable work) {
        if (SwingUtilities.isEventDispatchThread()) {
            work.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(work);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            // nothing useful to do in a demo app
        }
    }

    /** Subsystem step: build the window around the connected client. */
    public void openWindow() {
        onSwingThread(() -> {
            panel = new NetworkGamePanel(client, settings.mouseControls());
            frame = new JFrame("Gun Mayhem — " + settings.nickname()
                + " @ " + settings.host() + ":" + settings.port());
            // Never EXIT_ON_CLOSE: with several windows open that would take the
            // others down too. shutdown() ends the process after the last one.
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            if (windowAt != null) {
                frame.setLocation(windowAt);
            } else {
                frame.setLocationRelativeTo(null);
            }
            // Disposing the window is not enough: without closing the socket the
            // server keeps the player in the match, standing there for good.
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    shutdown();
                }
            });

            frame.setVisible(true);
            panel.requestFocusInWindow();
            LIVE_SESSIONS.incrementAndGet();
        });
    }

    /** Subsystem step: background music, for as long as this session lives. */
    public void playMusic() {
        AudioManager.getInstance().playMusic(Music.BACKGROUND);
    }

    /**
     * Tear the session down: stop the view, drop the window, close the socket —
     * and once the last session is gone, end the process. Safe to call twice.
     */
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;

        boolean hadWindow = frame != null;
        if (hadWindow) {
            // Stop the repaint timer first: left running, it keeps posting events
            // for a window that no longer exists, and the process never exits.
            onSwingThread(() -> {
                panel.stop();
                frame.dispose();
            });
            panel = null;
            frame = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (hadWindow && LIVE_SESSIONS.decrementAndGet() <= 0) {
            // The last window is gone, so the whole point of this process is too.
            AudioManager.getInstance().stopMusic();
            System.exit(0);
        }
    }

    /** Window width, so a caller tiling several windows can do the arithmetic. */
    public static int windowWidth() {
        return GameConfig.WORLD_WIDTH;
    }
}
