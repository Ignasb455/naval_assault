package gunmayhem.core.stages;

import java.util.Locale;

/**
 * Drifts a stage, saves where its platforms are, drifts further, then puts
 * them back. Run with {@code java -cp out gunmayhem.Main memento}.
 *
 * <p>The point to watch: nothing here ever sees the saved state. {@code save()}
 * returns nothing and {@code restore()} takes nothing, so this class cannot
 * read the snapshot, keep a copy of it, or hand back one of its own making —
 * each platform holds what it remembered and is the only thing that can apply
 * it.
 */
public final class MementoDemo {
    private static final float STEP = 1f / 60f;

    private MementoDemo() { }

    public static void run() {
        System.out.println("Drift: every platform travels, so there is state worth saving.");
        Stage drift = new DriftStage();
        System.out.println("  " + drift.movingPlatformCount() + " of "
            + drift.platforms().size() + " platforms move");

        tick(drift, 2f);
        drift.save();
        String atSave = positions(drift);
        System.out.println("  saved at   " + atSave);

        tick(drift, 2.5f);
        String drifted = positions(drift);
        System.out.println("  2.5s later " + drifted);

        drift.restore();
        String restored = positions(drift);
        System.out.println("  restored   " + restored);
        System.out.println("  back to the saved moment exactly: " + restored.equals(atSave));

        // Restoring swaps, so a second call returns to where the first one
        // found things. That is all an undo needs to be.
        drift.restore();
        System.out.println("  restored again: " + positions(drift));
        System.out.println("  back to where the restore found things: "
            + positions(drift).equals(drifted));

        System.out.println();
        System.out.println("A platform that cannot move has nothing to remember:");
        Stage classic = new ClassicStage();
        System.out.println("  Classic    " + classic.movingPlatformCount() + " of "
            + classic.platforms().size() + " platforms move");
        String before = positions(classic);
        classic.save();
        classic.restore();
        System.out.println("  save and restore are a no-op: "
            + positions(classic).equals(before));

        System.out.println();
        System.out.println("Nothing above could pass a snapshot in: save() takes no"
            + " arguments and returns none,");
        System.out.println("and the record holding the values is private to"
            + " MovingPlatform.");
    }

    private static void tick(Stage stage, float seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) {
            stage.update(STEP);
        }
    }

    /** Where every platform sits right now, rounded for readable output. */
    private static String positions(Stage stage) {
        StringBuilder sb = new StringBuilder("x = ");
        for (Platform plat : stage.platforms()) {
            sb.append(String.format(Locale.ROOT, "%7.2f", plat.x())).append(' ');
        }
        return sb.toString().trim();
    }
}
