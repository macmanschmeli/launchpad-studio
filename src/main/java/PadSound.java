
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

public class PadSound {

    private Clip   clip;
    private long   startFrame = 0; // frame to seek to before playing

    // load from a bundled resource
    public static PadSound fromResource(String filename) throws Exception {
        PadSound ps = new PadSound();
        InputStream is = PadSound.class.getResourceAsStream("/sounds/" + filename);
        AudioInputStream audio = AudioSystem.getAudioInputStream(
                new BufferedInputStream(is));
        ps.clip = AudioSystem.getClip();
        ps.clip.open(audio);
        return ps;
    }

    // load from a user-picked file
    public static PadSound fromFile(File file, double startSeconds) throws Exception {
        PadSound ps = new PadSound();

        AudioInputStream rawStream  = AudioSystem.getAudioInputStream(file);
        AudioFormat      rawFormat  = rawStream.getFormat();

        // if it's MP3 (or any non-PCM format), transcode to PCM
        AudioInputStream pcmStream;
        if (rawFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rawFormat.getSampleRate(),
                    16,
                    rawFormat.getChannels(),
                    rawFormat.getChannels() * 2,
                    rawFormat.getSampleRate(),
                    false
            );
            pcmStream = AudioSystem.getAudioInputStream(pcmFormat, rawStream);
        } else {
            pcmStream = rawStream;
        }

        ps.clip = AudioSystem.getClip();
        ps.clip.open(pcmStream);

        if (startSeconds > 0) {
            float fps     = ps.clip.getFormat().getFrameRate();
            ps.startFrame = Math.min(
                    (long)(startSeconds * fps),
                    ps.clip.getFrameLength() - 1
            );
        }

        return ps;
    }

    public void play() {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition((int) startFrame);
        clip.start();
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition((int) startFrame);
        }
    }

    public Clip getClip() { return clip; }
}