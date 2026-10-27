package gunmayhem.net;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JSON object for one player, as {@link StateSnapshot#toJson()}
 * sends it. The key names are the wire format that
 * {@link StateSnapshot#fromJson(String)} reads back.
 */
public final class PlayerJsonBuilder implements PlayerBuilder<Map<String, Object>> {
    private final Map<String, Object> json = new LinkedHashMap<>();

    @Override
    public PlayerJsonBuilder id(int id) {
        json.put("id", id);
        return this;
    }

    @Override
    public PlayerJsonBuilder name(String name) {
        json.put("name", name);
        return this;
    }

    @Override
    public PlayerJsonBuilder position(float x, float y) {
        json.put("x", x);
        json.put("y", y);
        return this;
    }

    @Override
    public PlayerJsonBuilder facing(int facing) {
        json.put("facing", facing);
        return this;
    }

    @Override
    public PlayerJsonBuilder damage(float damage) {
        json.put("damage", damage);
        return this;
    }

    @Override
    public PlayerJsonBuilder lives(int lives) {
        json.put("lives", lives);
        return this;
    }

    @Override
    public PlayerJsonBuilder kills(int kills) {
        json.put("kills", kills);
        return this;
    }

    @Override
    public PlayerJsonBuilder alive(boolean alive) {
        json.put("alive", alive);
        return this;
    }

    @Override
    public PlayerJsonBuilder weapon(String weapon) {
        json.put("weapon", weapon);
        return this;
    }

    @Override
    public PlayerJsonBuilder boosted(boolean boosted) {
        json.put("boosted", boosted);
        return this;
    }

    @Override
    public PlayerJsonBuilder tripleJump(boolean tripleJump) {
        json.put("triple", tripleJump);
        return this;
    }

    @Override
    public PlayerJsonBuilder shielded(boolean shielded) {
        json.put("shield", shielded);
        return this;
    }

    @Override
    public PlayerJsonBuilder moving(boolean moving) {
        json.put("moving", moving);
        return this;
    }

    @Override
    public Map<String, Object> build() {
        return new LinkedHashMap<>(json);
    }
}
