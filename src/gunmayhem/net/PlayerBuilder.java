package gunmayhem.net;

/**
 * The building steps for one player's per-tick description. Every concrete
 * builder accepts the same steps but assembles a different product:
 * {@link PlayerViewBuilder} an in-memory {@link StateSnapshot.PlayerView},
 * {@link PlayerJsonBuilder} the JSON object that goes on the wire.
 *
 * @param <T> the product {@link #build()} returns
 */
public interface PlayerBuilder<T> {
    PlayerBuilder<T> id(int id);

    PlayerBuilder<T> name(String name);

    PlayerBuilder<T> position(float x, float y);

    PlayerBuilder<T> facing(int facing);

    PlayerBuilder<T> damage(float damage);

    PlayerBuilder<T> lives(int lives);

    PlayerBuilder<T> kills(int kills);

    PlayerBuilder<T> alive(boolean alive);

    PlayerBuilder<T> weapon(String weapon);

    PlayerBuilder<T> boosted(boolean boosted);

    PlayerBuilder<T> tripleJump(boolean tripleJump);

    PlayerBuilder<T> shielded(boolean shielded);

    PlayerBuilder<T> moving(boolean moving);

    T build();
}
