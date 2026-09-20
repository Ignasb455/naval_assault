package gunmayhem.core.drops;

/** How rare a loot-crate drop is. Also picks the crate colour on the client. */
public enum Rarity {
    COMMON, UNCOMMON, RARE;

    /** Lower-case name used on the wire. */
    public String wireName() {
        return name().toLowerCase();
    }
}
