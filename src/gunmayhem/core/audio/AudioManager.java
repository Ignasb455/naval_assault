package gunmayhem.core.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class AudioManager {

    private static AudioManager instance;

    /** Raw file bytes, read once per sound and replayed from memory after that. */
    private final Map<Sound, byte[]> cache = new EnumMap<>(Sound.class);

    /**
     * Effects play on this thread rather than the caller's, to avoid blocking while opening clip.
     */
    private final ExecutorService effects = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio");
        t.setDaemon(true); // must never hold the JVM open after the window closes
        return t;
    });

    private Clip musicClip;

    /** For singleton constructor should be private so nobody uses "new" and does not create new objects */
    private AudioManager() {
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null)
            instance = new AudioManager();

        return instance;
    }

    /** Play a one-shot effect. Returns immediately; the sound starts on the audio thread. */
    public void playSound(Sound sound) {
        effects.execute(() -> start(sound));
    }

    private void start(Sound sound) {
        byte[] data = bytesOf(sound);

        if (data.length == 0) {
            return;
        }

        try (AudioInputStream file = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
             AudioInputStream pcm = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, file)) {

            Clip clip = AudioSystem.getClip();

            // A finished clip keeps its mixer line and thread until it is closed,
            // and lines are a limited resource. We need to make sure we close clip after sound is played
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    event.getLine().close();
                }
            });

            clip.open(pcm);
            clip.start();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not play " + sound + ": " + e.getMessage());
        }
    }

    /** File bytes for a sound, loaded on first use. An empty array means "unavailable". */
    private byte[] bytesOf(Sound sound) {
        synchronized (cache) {
            byte[] cached = cache.get(sound);
            if (cached != null) {
                return cached;
            }
        }

        byte[] data = read(sound.getFilename());
        synchronized (cache) {
            // Failures are cached too, so a missing file is reported once rather
            // than on every trigger pull.
            cache.put(sound, data);
        }

        return data;
    }

    private byte[] read(String filename) {
        URL url = AudioManager.class.getResource(filename);

        if (url == null) {
            System.err.println("Sound not found: " + filename);
            return new byte[0];
        }

        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            System.err.println("Could not read " + filename + ": " + e.getMessage());
            return new byte[0];
        }
    }

    /** Start a looping background track, replacing whatever was playing. */
    public synchronized void playMusic(Music music) {
        stopMusic();
        String filename = music.getFilename();
        URL url = AudioManager.class.getResource(filename);

        if (url == null) {
            System.err.println("Music not found: " + filename);
            return;
        }

        try (AudioInputStream file = AudioSystem.getAudioInputStream(url);
             AudioInputStream pcm = AudioSystem.getAudioInputStream(
                 AudioFormat.Encoding.PCM_SIGNED, file)) {

            musicClip = AudioSystem.getClip();
            musicClip.open(pcm);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not play " + music + ": " + e.getMessage());
            musicClip = null;
        }
    }

    public synchronized void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }
}
