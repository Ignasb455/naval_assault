package gunmayhem.core.drops;

/** Concrete factory 1: the crate holds a weapon. */
public final class GunDropFactory implements DropFactory {
    @Override
    public CommonDrop createCommon() {
        return new SemiAutoPistol();
    }

    @Override
    public UncommonDrop createUncommon() {
        return new Smg();
    }

    @Override
    public RareDrop createRare() {
        return new Shotgun();
    }

    @Override
    public String kind() {
        return "gun";
    }
}
