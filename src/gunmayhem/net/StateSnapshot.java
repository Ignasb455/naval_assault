package gunmayhem.net;

import gunmayhem.core.Bullet;
import gunmayhem.core.GameConfig;
import gunmayhem.core.GameWorld;
import gunmayhem.core.LootCrate;
import gunmayhem.core.Player;
import gunmayhem.core.audio.Sound;
import gunmayhem.core.stages.Platform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable view of the world at one instant — the authoritative state the
 * server broadcasts and clients render. Deliberately carries only what a client
 * needs to draw (no velocities), keeping the wire format small.
 *
 * <p>Platforms travel between stages and some of them move during a match, so
 * the layout is sent too rather than rebuilt client-side.
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

    /** One platform as the client needs to draw it. */
    public static final class PlatformView {
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        public final String kind;   // "solid" / "ice" / "moving"

        public PlatformView(float x, float y, float width, float height, String kind) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.kind = kind;
        }
    }

    public static final class BulletView {
        public final float x;
        public final float y;
        public final int color; // packed RGB, straight from the bullet's spec

        public BulletView(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    public final List<PlayerView> players;
    public final List<BulletView> bullets;
    public final List<CrateView> crates;
    public final List<PlatformView> platforms;
    public final boolean over;
    public final int winnerId; // -1 when there is no winner yet / a draw
    public final List<Sound> sounds;

    public StateSnapshot(List<PlayerView> players, List<BulletView> bullets,
                         List<CrateView> crates, List<PlatformView> platforms,
                         boolean over, int winnerId, List<Sound> sounds) {
        this.players = players;
        this.bullets = bullets;
        this.crates = crates;
        this.platforms = platforms;
        this.over = over;
        this.winnerId = winnerId;
        this.sounds = sounds;
    }

    /** Capture the current authoritative world state. Server side only. */
    public static StateSnapshot fromWorld(GameWorld world) {
        List<PlayerView> ps = new ArrayList<>();
        for (Player p : world.players()) {
            ps.add(new PlayerViewBuilder()
                .id(p.id)
                .name(p.name)
                .position(p.x, p.y)
                .facing(p.facing)
                .damage(p.damage)
                .lives(p.lives)
                .kills(p.kills)
                .alive(p.isAlive())
                .weapon(p.weapon.name())
                .boosted(p.speedMultiplier > 1f)
                .tripleJump(p.maxJumps > GameConfig.MAX_JUMPS)
                .shielded(p.shielded)
                .moving(Math.abs(p.vx) > 1f)
                .build());
        }
        List<BulletView> bs = new ArrayList<>();
        for (Bullet b : world.bullets()) {
            bs.add(new BulletView(b.x, b.y, b.spec.colorRgb));
        }
        List<CrateView> cs = new ArrayList<>();
        for (LootCrate c : world.crates()) {
            cs.add(new CrateView(c.x, c.y, c.drop.kind(), c.drop.rarity().wireName()));
        }
        List<PlatformView> pls = new ArrayList<>();
        for (Platform plat : world.platforms()) {
            pls.add(new PlatformView(plat.x(), plat.y(), plat.width(), plat.height(),
                plat.kind()));
        }
        int winner = world.winner() != null ? world.winner().id : -1;
        // Drains the world's sound queue, so the sounds are send once.
        return new StateSnapshot(ps, bs, cs, pls, world.isOver(), winner, world.drainSounds());
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

    /**
     * Run the building steps for one player on any builder. The steps are the
     * same whichever builder is passed; only the product differs.
     */
    public static <T> T describe(PlayerView p, PlayerBuilder<T> builder) {
        return builder
            .id(p.id)
            .name(p.name)
            .position(p.x, p.y)
            .facing(p.facing)
            .damage(p.damage)
            .lives(p.lives)
            .kills(p.kills)
            .alive(p.alive)
            .weapon(p.weapon)
            .boosted(p.boosted)
            .tripleJump(p.tripleJump)
            .shielded(p.shielded)
            .moving(p.moving)
            .build();
    }

    // ---- Wire format --------------------------------------------------------

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "state");
        root.put("over", over);
        root.put("winnerId", winnerId);

        List<Object> ps = new ArrayList<>();
        for (PlayerView p : players) {
            ps.add(describe(p, new PlayerJsonBuilder()));
        }
        root.put("players", ps);

        List<Object> bs = new ArrayList<>();
        for (BulletView b : bullets) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", b.x);
            m.put("y", b.y);
            m.put("color", b.color);
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

        List<Object> pls = new ArrayList<>();
        for (PlatformView p : platforms) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("x", p.x);
            m.put("y", p.y);
            m.put("w", p.width);
            m.put("h", p.height);
            m.put("kind", p.kind);
            pls.add(m);
        }
        root.put("platforms", pls);

        if (!sounds.isEmpty()) {
            List<Object> snd = new ArrayList<>();
            for (Sound s : sounds) {
                snd.add(s.name());
            }
            root.put("sounds", snd);
        }

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
            players.add(new PlayerViewBuilder()
                .id(asInt(m.get("id"), 0))
                .name(asString(m.get("name")))
                .position(asFloat(m.get("x")), asFloat(m.get("y")))
                .facing(asInt(m.get("facing"), 1))
                .damage(asFloat(m.get("damage")))
                .lives(asInt(m.get("lives"), 0))
                .kills(asInt(m.get("kills"), 0))
                .alive(asBool(m.get("alive")))
                .weapon(asString(m.get("weapon")))
                .boosted(asBool(m.get("boosted")))
                .tripleJump(asBool(m.get("triple")))
                .shielded(asBool(m.get("shield")))
                .moving(asBool(m.get("moving")))
                .build());
        }

        List<BulletView> bullets = new ArrayList<>();
        for (Object o : (List<Object>) root.get("bullets")) {
            Map<String, Object> m = (Map<String, Object>) o;
            bullets.add(new BulletView(asFloat(m.get("x")), asFloat(m.get("y")),
                asInt(m.get("color"), 0xFFD400)));
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

        List<PlatformView> platforms = new ArrayList<>();
        Object rawPlatforms = root.get("platforms");
        if (rawPlatforms instanceof List<?> list) {
            for (Object o : list) {
                Map<String, Object> m = (Map<String, Object>) o;
                platforms.add(new PlatformView(asFloat(m.get("x")), asFloat(m.get("y")),
                    asFloat(m.get("w")), asFloat(m.get("h")), asString(m.get("kind"))));
            }
        }

        List<Sound> sounds = new ArrayList<>();
        if (root.get("sounds") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof String s) {
                    try {
                        sounds.add(Sound.valueOf(s));
                    } catch (IllegalArgumentException unknownToThisClient) {
                    }
                }
            }
        }

        return new StateSnapshot(players, bullets, crates, platforms, over, winnerId, sounds);
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
