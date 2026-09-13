package gunmayhem.net;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameWorld;
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
        public final boolean alive;

        public PlayerView(int id, String name, float x, float y, int facing,
                          float damage, int lives, boolean alive) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.damage = damage;
            this.lives = lives;
            this.alive = alive;
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
    public final boolean over;
    public final int winnerId; // -1 when there is no winner yet / a draw

    public StateSnapshot(List<PlayerView> players, List<BulletView> bullets, boolean over, int winnerId) {
        this.players = players;
        this.bullets = bullets;
        this.over = over;
        this.winnerId = winnerId;
    }

    /** Capture the current authoritative world state. Server side only. */
    public static StateSnapshot fromWorld(GameWorld world) {
        List<PlayerView> ps = new ArrayList<>();
        for (Player p : world.players()) {
            ps.add(new PlayerView(p.id, p.name, p.x, p.y, p.facing, p.damage, p.lives, p.isAlive()));
        }
        List<BulletView> bs = new ArrayList<>();
        for (Bullet b : world.bullets()) {
            bs.add(new BulletView(b.x, b.y));
        }
        int winner = world.winner() != null ? world.winner().id : -1;
        return new StateSnapshot(ps, bs, world.isOver(), winner);
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
            m.put("alive", p.alive);
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
                asBool(m.get("alive"))));
        }

        List<BulletView> bullets = new ArrayList<>();
        for (Object o : (List<Object>) root.get("bullets")) {
            Map<String, Object> m = (Map<String, Object>) o;
            bullets.add(new BulletView(asFloat(m.get("x")), asFloat(m.get("y"))));
        }

        return new StateSnapshot(players, bullets, over, winnerId);
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
