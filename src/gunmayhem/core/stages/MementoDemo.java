package gunmayhem.core.stages;

import java.util.Locale;

/**
 * Drifts a stage, saves where its platforms are, drifts further, then puts
 * them back. Run with {@code java -cp out gunmayhem.Main memento}.
 *
 * <p>The point to watch: this class holds a {@link StageMemento} and hands it
 * back, but there is nothing on it to read. A moving platform keeps its
 * offset and direction private, so nobody else can capture or rebuild that
 * state — which is exactly when a memento is the right answer.
 */
public final class MementoDemo {
    private static final float STEP = 1f / 60f;

    private MementoDemo() { }

    public static void run() {
        System.out.println("Drift: every platform travels, so the memento has work to do.");
        Stage drift = new DriftStage();
        drift.platforms(); // build them before the first tick

        tick(drift, 2f);
        System.out.println("  after 2s   " + positions(drift));

        StageMemento saved = drift.save();
        System.out.println("  saved      " + saved.moving() + " of " + saved.size()
            + " platforms had travel state");
        String atSave = positions(drift);

        tick(drift, 2.5f);
        System.out.println("  after 4.5s " + positions(drift));

        drift.restore(saved);
        String afterRestore = positions(drift);
        System.out.println("  restored   " + afterRestore);
        System.out.println("  identical to the saved moment: " + afterRestore.equals(atSave));

        System.out.println();
        System.out.println("A platform that cannot move saves nothing, and the caretaker"
            + " cannot tell:");
        Stage classic = new ClassicStage();
        classic.platforms();
        StageMemento none = classic.save();
        System.out.println("  Classic    " + none.moving() + " of " + none.size()
            + " platforms had travel state");
        String before = positions(classic);
        classic.restore(none);
        System.out.println("  restoring it is a no-op: " + positions(classic).equals(before));

        System.out.println();
        System.out.println("The memento is opaque on purpose: StageMemento exposes only"
            + " size() and moving(),");
        System.out.println("and each platform's own state is a private record that no"
            + " other class can read.");
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
