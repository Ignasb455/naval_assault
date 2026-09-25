package gunmayhem.core.prototype;

import gunmayhem.core.Bullet;
import gunmayhem.core.BulletSpec;
import gunmayhem.core.GameConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints the difference between a shallow and a deep copy of a {@link Bullet}:
 * the identity hash of every object involved, and what happens to the template
 * when a clone's spec is modified.
 *
 * <p>Run with {@code java -cp out gunmayhem.Main prototype}.
 */
public final class PrototypeDemo {
    private PrototypeDemo() { }

    public static void run() {
        CopyMode original = GameConfig.BULLET_COPY_MODE;
        try {
            report(CopyMode.SHALLOW);
            System.out.println();
            report(CopyMode.DEEP);
            System.out.println();
            System.out.println("Bullet copy mode in the running game: " + original);
        } finally {
            GameConfig.BULLET_COPY_MODE = original;
        }
    }

    private static void report(CopyMode mode) {
        GameConfig.BULLET_COPY_MODE = mode;

        // A shotgun's template: one configured bullet, cloned once per pellet.
        Bullet template = new Bullet(0, 0, 0, -1,
            new BulletSpec(GameConfig.PELLET_DAMAGE, GameConfig.KNOCKBACK_BASE * 0.7f,
                GameConfig.BULLET_COLOR_PELLET, "pellet"));

        List<Bullet> pellets = new ArrayList<>();
        for (int i = 0; i < GameConfig.SHOTGUN_PELLETS; i++) {
            pellets.add(template.copy());
        }

        System.out.println("=== " + mode + " ===");
        System.out.printf("%-12s %-18s %-18s %s%n", "object", "bullet address", "spec address", "damage");
        line("template", template);
        for (int i = 0; i < pellets.size(); i++) {
            line("pellet " + i, pellets.get(i));
        }

        // Mutating one clone's spec: shared under SHALLOW, private under DEEP.
        Bullet mutated = pellets.get(0);
        mutated.spec.damage = 99f;

        System.out.println("after pellet 0 sets spec.damage = 99:");
        System.out.println("  template damage : " + template.spec.damage
            + (template.spec.damage == 99f ? "   <-- the template changed too!" : ""));
        System.out.println("  pellet 1 damage : " + pellets.get(1).spec.damage);
        System.out.println("  specs shared    : " + (template.spec == mutated.spec));
    }

    private static void line(String label, Bullet b) {
        System.out.printf("%-12s @%-17s @%-17s %s%n",
            label,
            Integer.toHexString(System.identityHashCode(b)),
            Integer.toHexString(System.identityHashCode(b.spec)),
            b.spec.damage);
    }
}
