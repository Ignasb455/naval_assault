package gunmayhem.core.audio;

public enum Music {

    BACKGROUND("/gunmayhem/core/audio/tracks/music/background_music.wav");

    private final String filename;

    Music(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}