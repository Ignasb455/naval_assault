package gunmayhem.core.stages;

import java.util.List;
import java.util.Random;

/**
 * Convenience lookup over the available stages. This is a plain static helper,
 * not part of the factory-method pattern: {@link Stage} and its subclasses are
 * the creators, and this only picks which creator a match uses.
 */
public final class Stages {
    private Stages() { }

    public static List<Stage> all() {
        return List.of(new ClassicStage(), new GlacierStage(), new DriftStage());
    }

    /** The stage with that id, or {@link ClassicStage} if the id is unknown. */
    public static Stage byId(String id) {
        for (Stage s : all()) {
            if (s.id().equalsIgnoreCase(id)) {
                return s;
            }
        }
        return new ClassicStage();
    }

    public static Stage random(Random rng) {
        List<Stage> all = all();
        return all.get(rng.nextInt(all.size()));
    }
}
