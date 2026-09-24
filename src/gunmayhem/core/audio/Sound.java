package gunmayhem.core.audio;

public enum Sound {
    GUNSHOT_GLOCK_18("/gunmayhem/core/audio/tracks/sounds/glock_18.wav");

    private final String filename;

    Sound(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return this.filename;
    }
}
