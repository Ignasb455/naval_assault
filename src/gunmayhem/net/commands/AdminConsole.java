package gunmayhem.net.commands;

import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.drops.Pistol;
import gunmayhem.core.drops.SemiAutoPistol;
import gunmayhem.core.drops.Shotgun;
import gunmayhem.core.drops.Smg;
import gunmayhem.core.drops.Weapon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Admin commands typed into the server console:
 * <pre>
 *   give &lt;slot&gt; pistol|semi|smg|shotgun   hand a player a weapon
 *   lives &lt;slot&gt; &lt;n&gt;                      set a player's remaining lives
 *   crate                                 drop a random crate now
 *   undo                                  take back the last command
 *   history                               list commands that can be undone
 * </pre>
 * Lines are read on their own thread but only queued there; {@link
 * #runPending} executes them on the game-loop thread, so a command never
 * changes the world halfway through a simulation step.
 */
public final class AdminConsole {
    private final Queue<String> pending = new ConcurrentLinkedQueue<>();
    private final CommandHistory history = new CommandHistory();
    private final PrintStream out;

    // Commands hold references into one match; a new match starts a new history.
    private GameWorld historyWorld;

    public AdminConsole(PrintStream out) {
        this.out = out;
    }

    /** Start reading admin commands from {@code in} on a daemon thread. */
    public void listen(InputStream in) {
        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) {
                        pending.add(line.trim());
                    }
                }
            } catch (IOException e) {
                // Console closed (e.g. the server runs in the background): no admin input.
            }
        }, "admin-console");
        reader.setDaemon(true);
        reader.start();
    }

    /** Queue a line as if it had been typed. */
    public void submit(String line) {
        pending.add(line.trim());
    }

    /** Run every queued line against the current match. Game-loop thread only. */
    public void runPending(GameWorld world) {
        if (world != historyWorld) {
            history.clear();
            historyWorld = world;
        }
        String line;
        while ((line = pending.poll()) != null) {
            if (world == null) {
                out.println("[admin] no match running");
                continue;
            }
            try {
                handle(line, world);
            } catch (IllegalArgumentException e) {
                out.println("[admin] " + e.getMessage());
            }
        }
    }

    private void handle(String line, GameWorld world) {
        String[] arg = line.toLowerCase().split("\\s+");
        switch (arg[0]) {
            case "undo" -> {
                CommandHistory.Undone undone = history.undo();
                if (undone == null) {
                    out.println("[admin] nothing to undo");
                } else if (undone.reverted()) {
                    out.println("[admin] undone: " + undone.command().describe());
                } else {
                    out.println("[admin] cannot undo " + undone.command().describe()
                        + " any more");
                }
            }
            case "history" -> {
                if (history.entries().isEmpty()) {
                    out.println("[admin] history is empty");
                }
                for (AdminCommand c : history.entries()) {
                    out.println("[admin]   " + c.describe());
                }
            }
            case "give" -> run(new GiveWeaponCommand(player(world, arg, 1), weapon(arg, 2)));
            case "lives" -> run(new SetLivesCommand(player(world, arg, 1), number(arg, 2)));
            case "crate" -> run(new SpawnCrateCommand(world));
            default -> out.println("[admin] commands: give <slot> pistol|semi|smg|shotgun,"
                + " lives <slot> <n>, crate, undo, history");
        }
    }

    private void run(AdminCommand command) {
        history.run(command);
        out.println("[admin] done: " + command.describe());
    }

    private static Player player(GameWorld world, String[] arg, int i) {
        Player p = world.playerById(number(arg, i));
        if (p == null) {
            throw new IllegalArgumentException("no player in slot " + arg[i]);
        }
        return p;
    }

    private static Weapon weapon(String[] arg, int i) {
        String name = i < arg.length ? arg[i] : "";
        return switch (name) {
            case "pistol" -> new Pistol();
            case "semi" -> new SemiAutoPistol();
            case "smg" -> new Smg();
            case "shotgun" -> new Shotgun();
            default -> throw new IllegalArgumentException(
                "unknown weapon '" + name + "' (pistol, semi, smg, shotgun)");
        };
    }

    private static int number(String[] arg, int i) {
        try {
            return Integer.parseInt(arg[i]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("expected a number at position " + (i + 1));
        }
    }
}
