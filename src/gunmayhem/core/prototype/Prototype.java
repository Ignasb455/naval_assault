package gunmayhem.core.prototype;

import gunmayhem.core.GameConfig;

/**
 * An object that can produce copies of itself, so a fully configured instance
 * can act as a template instead of being rebuilt from scratch every time.
 *
 * <p>Both copy kinds are exposed on purpose: the difference only shows up when
 * the object owns a mutable sub-object, and comparing the two is the whole
 * point of the exercise.
 */
public interface Prototype<T> {
    /** New object; inner objects are shared with this one. */
    T shallowCopy();

    /** New object; inner objects are copied too. */
    T deepCopy();

    /** Copy according to the mode currently selected in {@link GameConfig}. */
    default T copy() {
        return GameConfig.BULLET_COPY_MODE == CopyMode.DEEP ? deepCopy() : shallowCopy();
    }
}
