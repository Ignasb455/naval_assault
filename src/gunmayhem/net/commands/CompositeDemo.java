package gunmayhem.net.commands;

import gunmayhem.core.GameWorld;
import gunmayhem.core.Player;
import gunmayhem.core.PlayerSpec;
import gunmayhem.core.drops.Shotgun;
import gunmayhem.core.gamemodes.LastManStanding;
import gunmayhem.core.stages.ClassicStage;
import java.util.List;

/**
 * Builds a nested group of admin commands, runs it, and takes the whole thing
 * back with a single undo. Run with
 * {@code java -cp out gunmayhem.Main composite}.
 *
 * <p>The point to watch: {@link CommandHistory} is handed the outermost group
 * and treats it exactly like a one-line command, because a group is an
 * {@link AdminCommand} too.
 */
public final class CompositeDemo {
    private CompositeDemo() { }

    public static void run() {
        GameWorld world = new GameWorld(
            List.of(new PlayerSpec(0, "Ignas"), new PlayerSpec(1, "Nedas")),
            new ClassicStage(), new LastManStanding());
        Player ignas = world.playerById(0);
        Player nedas = world.playerById(1);

        // A group inside a group: this is what typing `group` while another
        // group is still recording produces.
        CommandGroup duel = new CommandGroup("duel", List.of(
            new GiveWeaponCommand(ignas, new Shotgun()),
            new GiveWeaponCommand(nedas, new Shotgun())));

        CommandGroup tournament = new CommandGroup("tournament", List.of(
            duel,
            new SetLivesCommand(ignas, 5),
            new SetLivesCommand(nedas, 5),
            new SpawnCrateCommand(world)));

        System.out.println("The tree the console would have recorded:");
        System.out.print(tournament.tree());

        System.out.println();
        System.out.println("before:  " + describe(ignas) + " | " + describe(nedas)
            + " | crates=" + world.crates().size());

        CommandHistory history = new CommandHistory();
        history.run(tournament);

        System.out.println("after:   " + describe(ignas) + " | " + describe(nedas)
            + " | crates=" + world.crates().size());

        System.out.println();
        System.out.println("History holds " + history.entries().size() + " entry, not 4:");
        for (AdminCommand entry : history.entries()) {
            System.out.println("  " + entry.describe());
        }

        System.out.println();
        CommandHistory.Undone undone = history.undo();
        world.update(1f / 60f); // the world drops crates marked dead on its next step
        System.out.println("one undo -> " + undone.command().describe()
            + ", fully reverted = " + undone.reverted());
        System.out.println("after:   " + describe(ignas) + " | " + describe(nedas)
            + " | crates=" + world.crates().size());

        System.out.println();
        System.out.println("Children are undone newest first, so a later step never"
            + " overwrites what an earlier one restored.");
    }

    private static String describe(Player player) {
        return player.name + " " + player.weapon.name() + "/" + player.lives + " lives";
    }
}
