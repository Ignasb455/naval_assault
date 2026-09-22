package gunmayhem.core.drops;

/** Abstract product A: the drop every factory hands out most often. */
public interface CommonDrop extends Drop {
    @Override
    default Rarity rarity() {
        return Rarity.COMMON;
    }
}
