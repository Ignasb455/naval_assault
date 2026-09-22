package gunmayhem.core.drops;

/** Concrete factory 2: the crate holds a stat boost instead of a gun. */
public final class PowerUpDropFactory implements DropFactory {
    @Override
    public CommonDrop createCommon() {
        return new SpeedBoost();
    }

    @Override
    public UncommonDrop createUncommon() {
        return new TripleJump();
    }

    @Override
    public RareDrop createRare() {
        return new Shield();
    }

    @Override
    public String kind() {
        return "power";
    }
}
