import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Utility class for loading bundled audio resources from the JAR.
 * All sounds placed under src/main/resources/sounds/ are accessible
 * via the classpath and work identically on all platforms.
 */
public class SoundLoader {
    public static Clip loadBundled(String filename) throws Exception {
        InputStream is = SoundLoader.class
                .getResourceAsStream("/sounds/" + filename);
        AudioInputStream audio = AudioSystem.getAudioInputStream(
                new BufferedInputStream(is));
        Clip clip = AudioSystem.getClip();
        clip.open(audio);
        return clip;
    }
}
