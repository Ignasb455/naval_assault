package gunmayhem.core.drops;

/** Abstract product C: the best drop a factory can produce. */
public interface RareDrop extends Drop {
    @Override
    default Rarity rarity() {
        return Rarity.RARE;
    }
}
