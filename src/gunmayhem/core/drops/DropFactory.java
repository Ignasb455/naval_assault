package gunmayhem.core.drops;

/**
 * Abstract factory for loot-crate contents. A caller picks one family
 * ({@link GunDropFactory} or {@link PowerUpDropFactory}) and can then ask it for
 * any of the three rarities without knowing which concrete classes come back.
 */
public interface DropFactory {
    CommonDrop createCommon();

    UncommonDrop createUncommon();

    RareDrop createRare();

    /** Family label, used when telling the client what a crate holds. */
    String kind();
}
