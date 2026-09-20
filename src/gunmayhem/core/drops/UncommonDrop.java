package gunmayhem.core.drops;

/** Abstract product B: the mid-tier drop. */
public interface UncommonDrop extends Drop {
    @Override
    default Rarity rarity() {
        return Rarity.UNCOMMON;
    }
}
