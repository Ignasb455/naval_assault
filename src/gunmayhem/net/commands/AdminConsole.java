package gunmayhem.net.commands;

import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.drops.Pistol;
import gunmayhem.core.drops.SemiAutoPistol;
import gunmayhem.core.drops.Shotgun;
import gunmayhem.core.drops.Smg;
import gunmayhem.core.drops.Weapon;
import gunmayhem.core.stages.StageMemento;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Admin commands typed into the server console:
 * <pre>
 *   give &lt;slot&gt; pistol|semi|smg|shotgun   hand a player a weapon
 *   lives &lt;slot&gt; &lt;n&gt;                      set a player's remaining lives
 *   crate                                 drop a random crate now
 *   stage save                            remember where the platforms are
 *   stage rewind                          put them back there
 *   group &lt;label&gt;                         start recording a group
 *   group end                             close it; the group runs as one command
 *   undo                                  take back the last command
 *   history                               list commands that can be undone
 * </pre>
 * Everything typed between {@code group} and {@code group end} goes into a
 * {@link CommandGroup} instead of running immediately, and groups nest, so a
 * scenario is assembled here rather than hardcoded.
 * Lines are read on their own thread but only queued there; {@link
 * #runPending} executes them on the game-loop thread, so a command never
 * changes the world halfway through a simulation step.
 */
public final class AdminConsole {
    private final Queue<String> pending = new ConcurrentLinkedQueue<>();
    private final CommandHistory history = new CommandHistory();
    private final PrintStream out;

    // One builder per open group. Pushing a second one while the first is
    // still recording is what nests them.
    private final Deque<CommandGroupBuilder> recording = new ArrayDeque<>();

    // Caretaker: the last `stage save`. Opaque here - only the platforms that
    // produced the pieces inside can read or apply them.
    private StageMemento savedStage;

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
            // Half-recorded groups hold players from the match that just ended.
            history.clear();
            recording.clear();
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
            // undo and history act on the history itself, so they are never
            // folded into a group being recorded.
            case "undo" -> {
                if (!recording.isEmpty()) {
                    out.println("[admin] finish the group first ('group end')");
                    return;
                }
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
            case "group" -> group(arg);
            case "stage" -> stage(arg, world);
            default -> {
                AdminCommand command = parse(arg, world);
                if (command == null) {
                    out.println("[admin] commands: give <slot> pistol|semi|smg|shotgun,"
                        + " lives <slot> <n>, crate, stage save|rewind,"
                        + " group <label>, group end, undo, history");
                } else {
                    accept(command);
                }
            }
        }
    }

    /** Turn a typed line into a command, or null if the verb is not one. */
    private AdminCommand parse(String[] arg, GameWorld world) {
        return switch (arg[0]) {
            case "give" -> new GiveWeaponCommand(player(world, arg, 1), weapon(arg, 2));
            case "lives" -> new SetLivesCommand(player(world, arg, 1), number(arg, 2));
            case "crate" -> new SpawnCrateCommand(world);
            default -> null;
        };
    }

    /** {@code group <label>} opens a level; {@code group end} closes one. */
    private void group(String[] arg) {
        if (arg.length < 2) {
            out.println("[admin] group <label>   or   group end");
            return;
        }
        if (arg[1].equals("end")) {
            if (recording.isEmpty()) {
                out.println("[admin] no group is being recorded");
                return;
            }
            // Closing the outermost level runs it; closing an inner one makes
            // it a child of the level still open underneath.
            accept(recording.pop().build());
        } else {
            recording.push(new CommandGroupBuilder(arg[1]));
            out.println("[admin] recording '" + arg[1] + "' — close it with 'group end'");
        }
    }

    /**
     * {@code stage save} takes a snapshot of where the moving platforms are;
     * {@code stage rewind} puts them back there. This console is the
     * caretaker: it holds the memento and never looks inside it.
     */
    private void stage(String[] arg, GameWorld world) {
        if (arg.length < 2) {
            out.println("[admin] stage save   or   stage rewind");
            return;
        }
        switch (arg[1]) {
            case "save" -> {
                savedStage = world.stage().save();
                out.println("[admin] stage saved (" + savedStage.moving() + " of "
                    + savedStage.size() + " platforms move)");
            }
            case "rewind" -> {
                if (savedStage == null) {
                    out.println("[admin] nothing saved yet - run 'stage save' first");
                    return;
                }
                accept(new StageRewindCommand(world.stage(), savedStage));
            }
            default -> out.println("[admin] stage save   or   stage rewind");
        }
    }

    /** Run the command, or fold it into the group currently being recorded. */
    private void accept(AdminCommand command) {
        if (recording.isEmpty()) {
            run(command);
        } else {
            recording.peek().add(command);
            out.println("[admin] recorded into '" + recording.peek().label() + "': "
                + command.describe());
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
