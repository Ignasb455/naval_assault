package gunmayhem.core.stages;

/**
 * A platform's saved state, as seen by whoever is holding on to it.
 *
 * <p>Deliberately empty: this is the narrow half of the memento pattern. A
 * caretaker can store one and hand it back later, but cannot read or alter
 * what is inside, because the only implementation is a private record nested
 * in the platform that produced it. The wide half — the actual values — is
 * visible to that platform alone.
 */
public interface PlatformMemento {
}
