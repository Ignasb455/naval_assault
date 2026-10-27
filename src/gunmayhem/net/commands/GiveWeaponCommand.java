package gunmayhem.net.commands;

import gunmayhem.core.Player;
import gunmayhem.core.drops.Weapon;

/** Hand a player a weapon; undo gives back the one they held before. */
public final class GiveWeaponCommand implements AdminCommand {
    private final Player player;
    private final Weapon weapon;
    private Weapon previous;

    public GiveWeaponCommand(Player player, Weapon weapon) {
        this.player = player;
        this.weapon = weapon;
    }

    @Override
    public void execute() {
        previous = player.weapon;
        player.weapon = weapon;
    }

    @Override
    public boolean undo() {
        player.weapon = previous;
        return true;
    }

    @Override
    public String describe() {
        return "give " + player.name + " " + weapon.name();
    }
}
