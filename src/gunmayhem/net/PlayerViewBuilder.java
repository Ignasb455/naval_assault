package gunmayhem.net;

/** Builds an immutable {@link StateSnapshot.PlayerView} one field at a time. */
public final class PlayerViewBuilder {
    private int id;
    private String name = "";
    private float x;
    private float y;
    private int facing = 1;
    private float damage;
    private int lives;
    private int kills;
    private boolean alive;
    private String weapon = "";
    private boolean boosted;
    private boolean tripleJump;
    private boolean shielded;
    private boolean moving;

    public PlayerViewBuilder id(int id) {
        this.id = id;
        return this;
    }

    public PlayerViewBuilder name(String name) {
        this.name = name;
        return this;
    }

    public PlayerViewBuilder position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public PlayerViewBuilder facing(int facing) {
        this.facing = facing;
        return this;
    }

    public PlayerViewBuilder damage(float damage) {
        this.damage = damage;
        return this;
    }

    public PlayerViewBuilder lives(int lives) {
        this.lives = lives;
        return this;
    }

    public PlayerViewBuilder kills(int kills) {
        this.kills = kills;
        return this;
    }

    public PlayerViewBuilder alive(boolean alive) {
        this.alive = alive;
        return this;
    }

    public PlayerViewBuilder weapon(String weapon) {
        this.weapon = weapon;
        return this;
    }

    public PlayerViewBuilder boosted(boolean boosted) {
        this.boosted = boosted;
        return this;
    }

    public PlayerViewBuilder tripleJump(boolean tripleJump) {
        this.tripleJump = tripleJump;
        return this;
    }

    public PlayerViewBuilder shielded(boolean shielded) {
        this.shielded = shielded;
        return this;
    }

    public PlayerViewBuilder moving(boolean moving) {
        this.moving = moving;
        return this;
    }

    public StateSnapshot.PlayerView build() {
        return new StateSnapshot.PlayerView(id, name, x, y, facing, damage, lives, kills,
            alive, weapon, boosted, tripleJump, shielded, moving);
    }
}
