package gunmayhem.core.stages;

import java.util.Collections;
import java.util.List;

/**
 * Where every platform of a stage was at one moment. Held by a caretaker —
 * the admin console — which knows only that it has something to hand back.
 *
 * <p>One entry per platform, in layout order. Materials that never move save
 * nothing and leave a null in their slot; the caretaker cannot tell the
 * difference, and does not need to.
 */
public final class StageMemento {
    private final List<PlatformMemento> platforms;

    StageMemento(List<PlatformMemento> platforms) {
        this.platforms = Collections.unmodifiableList(platforms);
    }

    /** How many platforms this covers, for logs. */
    public int size() {
        return platforms.size();
    }

    /** How many of them actually had travel state worth saving. */
    public int moving() {
        return (int) platforms.stream().filter(m -> m != null).count();
    }

    /** Package-private: only {@link Stage} hands these back to the platforms. */
    List<PlatformMemento> platforms() {
        return platforms;
    }
}
