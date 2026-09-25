package gunmayhem.core.prototype;

/**
 * Which kind of copy {@link Prototype#copy()} makes. Kept switchable at runtime
 * so the two strategies can be compared side by side.
 */
public enum CopyMode {
    /** New object, but the inner objects are shared with the original. */
    SHALLOW,

    /** New object and new copies of every inner object. */
    DEEP
}
