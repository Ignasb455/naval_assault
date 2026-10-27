package gunmayhem.net.commands;

import gunmayhem.core.GameWorld;
import gunmayhem.core.LootCrate;

/**
 * Drop a random crate right away. Undo takes it out of the arena again, but
 * only while nobody has picked it up: a drop already applied stays applied.
 */
public final class SpawnCrateCommand implements AdminCommand {
    private final GameWorld world;
    private LootCrate crate;

    public SpawnCrateCommand(GameWorld world) {
        this.world = world;
    }

    @Override
    public void execute() {
        crate = world.dropCrate();
    }

    @Override
    public boolean undo() {
        if (crate == null || !crate.alive) {
            return false; // already picked up (or fell off the map)
        }
        crate.alive = false; // the world drops dead crates on its next step
        return true;
    }

    @Override
    public String describe() {
        return "crate " + (crate != null ? crate.drop.name() : "");
    }
}
