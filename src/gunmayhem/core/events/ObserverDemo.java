package gunmayhem.core.events;

import gunmayhem.core.GameWorld;
import gunmayhem.core.LootCrate;
import gunmayhem.core.Player;
import gunmayhem.core.PlayerSpec;
import gunmayhem.core.drops.Shotgun;
import gunmayhem.core.gamemodes.LastManStanding;
import gunmayhem.core.stages.ClassicStage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Plays a short scripted match without any network and shows each observer
 * reacting to the events the world publishes. Run with
 * {@code java -cp out gunmayhem.Main observer}.
 */
public final class ObserverDemo {
    private static final float STEP = 1f / 60f;

    private ObserverDemo() { }

    public static void run() {
        GameWorld world = new GameWorld(
            List.of(new PlayerSpec(0, "Ignas"), new PlayerSpec(1, "Nedas")),
            new ClassicStage(), new LastManStanding());
        world.addObserver(new MatchLog(System.out));
        Player ignas = world.playerById(0);
        Player nedas = world.playerById(1);

        System.out.println("GameWorld notifies, in order: " + world.observers().stream()
            .map(o -> o.getClass().getSimpleName())
            .collect(Collectors.joining(" -> ")));

        System.out.println();
        System.out.println("1) Ignas fires -> onShot");
        world.fire(ignas);
        System.out.println("   SoundEffects queued: " + world.drainSounds());

        System.out.println();
        System.out.println("2) Nedas, last hit by Ignas, falls off -> onKnockout");
        knockOut(world, nedas, ignas);
        System.out.println("   KillTracker: Ignas kills = " + ignas.kills);

        System.out.println();
        System.out.println("3) Nedas walks into a crate -> onCratePicked");
        world.crates().add(new LootCrate(new Shotgun(), nedas.x, nedas.y));
        world.update(STEP);
        System.out.println("   Nedas now holds: " + nedas.weapon.name());

        System.out.println();
        System.out.println("4) Nedas is knocked out twice more -> onKnockout, onMatchOver");
        knockOut(world, nedas, ignas);
        knockOut(world, nedas, ignas);
        System.out.println("   KillTracker: Ignas kills = " + ignas.kills
            + ", match over = " + world.isOver());
    }

    /** Throw the victim far below the arena and let the world notice. */
    private static void knockOut(GameWorld world, Player victim, Player hitter) {
        victim.lastHitBy = hitter.id;
        victim.y = 10_000f;
        world.update(STEP);
    }
}
