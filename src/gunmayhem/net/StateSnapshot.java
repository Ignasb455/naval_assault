package gunmayhem.net;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.LootCrate;
import gunmayhem.core.Player;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable view of the world at one instant — the authoritative state the
 * server broadcasts and clients render. Deliberately carries only what a client
 * needs to draw (no velocities), keeping the wire format small.
 */
public final class StateSnapshot {

    public static final class PlayerView {
        public final int id;
        public final String name;
        public final float x;
        public final float y;
        public final int facing;
        public final float damage;
        public final int lives;
        public final int kills;
        public final boolean alive;
        public final String weapon;

        // Active crate effects, so the client can draw them on the body.
        public final boolean boosted;
        public final boolean tripleJump;
        public final boolean shielded;
        public final boolean moving;

        public PlayerView(int id, String name, float x, float y, int facing,
                          float damage, int lives, boolean alive, String weapon,
                          boolean boosted, boolean tripleJump, boolean shielded,
                          boolean moving) {
            this(id, name, x, y, facing, damage, lives, 0, alive, weapon,
                boosted, tripleJump, shielded, moving);
        }

        public PlayerView(int id, String name, float x, float y, int facing,
                          float damage, int lives, int kills, boolean alive, String weapon,
                          boolean boosted, boolean tripleJump, boolean shielded,
                          boolean moving) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.damage = damage;
            this.lives = lives;
            this.kills = kills;
            this.alive = alive;
            this.weapon = weapon;
            this.boosted = boosted;
            this.tripleJump = tripleJump;
            this.shielded = shielded;
            this.moving = moving;
        }
    }

    /** A loot crate as the client needs to draw it. */
    public static final class CrateView {
        public final float x;
        public final float y;
        public final String kind;   // "gun" or "power"
        public final String rarity; // "common" / "uncommon" / "rare"

        public CrateView(float x, float y, String kind, String rarity) {
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.rarity = rarity;
        }
    }

    public static final class BulletView {
        public final float x;
        public final float y;

        public BulletView(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public final List<PlayerView> players;
    public final List<BulletView> bullets;
    public final List<CrateView> crates;
    public final boolean over;
    public final int winnerId; // -1 when there is no winner yet / a draw
    public StateSnapshot(List<PlayerView> players, List<BulletView> bullets,
                         List<CrateView> crates, boolean over, int winnerId) {
        this.players = players;
        this.bullets = bullets;
        this.crates = crates;
        this.over = over;
        this.winnerId = winnerId;
    }

    /** Capture the current authoritative world state. Server side only. */
    public static StateSnapshot fromWorld(GameWorld world) {
        List<PlayerView> ps = new ArrayList<>();
        for (Player p : world.players()) {
            ps.add(new PlayerView(p.id, p.name, p.x, p.y, p.facing, p.damage, p.lives, p.kills,
                p.isAlive(), p.weapon.name(),
                p.speedMultiplier > 1f,
                p.maxJumps > GameConfig.MAX_JUMPS,
                p.shielded,
                Math.abs(p.vx) > 1f));
        }
        List<BulletView> bs = new ArrayList<>();
        for (Bullet b : world.bullets()) {
            bs.add(new BulletView(b.x, b.y));
        }
        List<CrateView> cs = new ArrayList<>();
        for (LootCrate c : world.crates()) {
            cs.add(new CrateView(c.x, c.y, c.drop.kind(), c.drop.rarity().wireName()));
        }
        int winner = world.winner() != null ? world.winner().id : -1;
        return new StateSnapshot(ps, bs, cs, world.isOver(), winner);
    }

    /** Nickname of the player holding that id, or a blank string if unknown. */
    public String nameOf(int id) {
        for (PlayerView p : players) {
            if (p.id == id) {
                return p.name;
            }
        }
        return "";
    }

    // ---- Wire format --------------------------------------------------------

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "state");
        root.put("over", over);
        root.put("winnerId", winnerId);

        List<Object> ps = new ArrayList<>();
        for (PlayerView p : players) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("name", p.name);
            m.put("x", p.x);
            m.put("y", p.y);
            m.put("facing", p.facing);
            m.put("damage", p.damage);
            m.put("lives", p.lives);
            m.put("kills", p.kills);
            m.put("alive", p.alive);
            m.put("weapon", p.weapon);
            m.put("boosted", p.boosted);
            m.put("triple", p.tripleJump);
            m.put("shield", p.shielded);
            m.put("moving", p.moving);
            ps.add(m);
        }
        root.put("players", ps);

        List<Object> bs = new ArrayList<>();
        for (BulletView b : bullets) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", b.x);
            m.put("y", b.y);
            bs.add(m);
        }
        root.put("bullets", bs);

        List<Object> cs = new ArrayList<>();
        for (CrateView c : crates) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", c.x);
            m.put("y", c.y);
            m.put("kind", c.kind);
            m.put("rarity", c.rarity);
            cs.add(m);
        }
        root.put("crates", cs);

        return Json.write(root);
    }

    @SuppressWarnings("unchecked")
    public static StateSnapshot fromJson(String json) {
        Map<String, Object> root = Json.parseObject(json);
        boolean over = asBool(root.get("over"));
        int winnerId = asInt(root.get("winnerId"), -1);

        List<PlayerView> players = new ArrayList<>();
        for (Object o : (List<Object>) root.get("players")) {
            Map<String, Object> m = (Map<String, Object>) o;
            players.add(new PlayerView(
                asInt(m.get("id"), 0),
                asString(m.get("name")),
                asFloat(m.get("x")),
                asFloat(m.get("y")),
                asInt(m.get("facing"), 1),
                asFloat(m.get("damage")),
                asInt(m.get("lives"), 0),
                asInt(m.get("kills"), 0),
                asBool(m.get("alive")),
                asString(m.get("weapon")),
                asBool(m.get("boosted")),
                asBool(m.get("triple")),
                asBool(m.get("shield")),
                asBool(m.get("moving"))));
        }

        List<BulletView> bullets = new ArrayList<>();
        for (Object o : (List<Object>) root.get("bullets")) {
            Map<String, Object> m = (Map<String, Object>) o;
            bullets.add(new BulletView(asFloat(m.get("x")), asFloat(m.get("y"))));
        }

        List<CrateView> crates = new ArrayList<>();
        Object rawCrates = root.get("crates");
        if (rawCrates instanceof List<?> list) {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                crates.add(new CrateView(asFloat(m.get("x")), asFloat(m.get("y")),
                    asString(m.get("kind")), asString(m.get("rarity"))));
            }
        }

        return new StateSnapshot(players, bullets, crates, over, winnerId);
    }

    private static String asString(Object o) {
        return o instanceof String s ? s : "";
    }

    private static float asFloat(Object o) {
        return o instanceof Number n ? n.floatValue() : 0f;
    }

    private static int asInt(Object o, int fallback) {
        return o instanceof Number n ? n.intValue() : fallback;
    }

    private static boolean asBool(Object o) {
        return o instanceof Boolean b && b;
    }
}
