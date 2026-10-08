package gunmayhem.client;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

/**
 * Second client of {@link GameClientFacade}: opens several player windows in
 * one JVM and tiles them across the screen, so a match can be tried out without
 * juggling a terminal per player.
 *
 * <p>It builds a session the same way {@code Main} does — one call — and only
 * adds what is its own concern: how many windows and where they go.
 */
public final class TestLauncher {
    private TestLauncher() { }

    public static void launch(int count, String host, int port) {
        List<GameClientFacade> sessions = new ArrayList<>();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // Two per row, scaled down if the screen cannot fit them side by side.
        int columns = Math.min(2, Math.max(1, screen.width / GameClientFacade.windowWidth()));
        int stepX = GameClientFacade.windowWidth() + 12;
        int stepY = 46; // cascade rows, since two full windows rarely fit vertically

        for (int i = 0; i < count; i++) {
            GameClientFacade session = new GameClientFacade(host, port, "P" + (i + 1));
            session.placeAt((i % columns) * stepX, (i / columns) * stepY);

            if (!session.start()) {
                System.out.println("Window " + (i + 1) + " cancelled; stopping here.");
                break;
            }
            sessions.add(session);
        }

        // Closing a window now tears its own session down, and the last one to
        // go ends the process, so there is nothing left to clean up here.
        System.out.println("Opened " + sessions.size() + " client window(s) against "
            + host + ":" + port + ". Close them to quit.");
    }
}
