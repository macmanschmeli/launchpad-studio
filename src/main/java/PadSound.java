import javax.sound.sampled.*;
import java.io.*;

/**
 * Wraps a single audio file for low-latency playback via SourceDataLine.
 * Streams PCM audio on a dedicated daemon thread and emits normalized
 * RMS amplitude values to an AmplitudeListener on each buffer chunk.
 * Transparently transcodes MP3 and other non-PCM formats to signed PCM.
 * Calling play() while already playing stops and restarts from startSeconds.
 */
public class PadSound {

    private AudioInputStream pcmStream;
    private AudioFormat      format;
    private File             file;
    private double           startSeconds;
    private SourceDataLine   line;
    private Thread           playThread;
    private boolean          playing = false;

    private AmplitudeListener amplitudeListener;

    public interface AmplitudeListener {
        void onAmplitude(float rms); // 0.0 to 1.0
    }

    public static PadSound fromFile(File file, double startSeconds) throws Exception {
        PadSound ps     = new PadSound();
        ps.file         = file;
        ps.startSeconds = startSeconds;
        ps.reloadStream();
        return ps;
    }

    private void reloadStream() throws Exception {
        AudioInputStream raw    = AudioSystem.getAudioInputStream(file);
        AudioFormat      rawFmt = raw.getFormat();

        // transcode to PCM signed if needed (e.g. MP3)
        if (rawFmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat pcmFmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    rawFmt.getSampleRate(),
                    16,
                    rawFmt.getChannels(),
                    rawFmt.getChannels() * 2,
                    rawFmt.getSampleRate(),
                    false
            );
            pcmStream = AudioSystem.getAudioInputStream(pcmFmt, raw);
        } else {
            pcmStream = raw;
        }

        format = pcmStream.getFormat();
        if (startSeconds > 0) {
            long bytesToSkip = (long)(startSeconds
                    * format.getSampleRate()
                    * format.getFrameSize());

            byte[] dummyBuffer = new byte[4096];
            long totalRead = 0;

            while (totalRead < bytesToSkip) {
                // Berechne, wie viel noch gelesen werden muss (maximal Puffergröße)
                int toRead = (int) Math.min(dummyBuffer.length, bytesToSkip - totalRead);
                int read = pcmStream.read(dummyBuffer, 0, toRead);

                if (read == -1) break; // Datei zu Ende
                totalRead += read;
            }
        }
    }

    public void setAmplitudeListener(AmplitudeListener listener) {
        this.amplitudeListener = listener;
    }

    public void play() {
        stop(); // stop any current playback first

        try {
            reloadStream(); // rewind by reloading

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            playing = true;

            playThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int    bytesRead;
                try {
                    while (playing && (bytesRead = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                        line.write(buffer, 0, bytesRead);

                        // compute RMS amplitude from this buffer chunk
                        if (amplitudeListener != null) {
                            float rms = computeRms(buffer, bytesRead, format);
                            amplitudeListener.onAmplitude(rms);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Playback error: " + e.getMessage());
                } finally {
                    if (playing) {
                        line.drain();
                        line.stop();
                    }
                    line.close();
                    playing = false;
                    if (amplitudeListener != null) {
                        amplitudeListener.onAmplitude(0f);
                    }
                }
            }, "audio-playback");

            playThread.setDaemon(true);
            playThread.start();

        } catch (Exception e) {
            System.err.println("Could not start playback: " + e.getMessage());
        }
    }

    public void stop() {
        playing = false;
        if (line != null && line.isOpen()) {
            line.stop();
            line.flush();
            line.close();
        }
        if (playThread != null) {
            playThread.interrupt();
        }
    }

    public boolean isPlaying() { return playing; }

    // RMS = root mean square of all samples in the buffer
    private float computeRms(byte[] buffer, int length, AudioFormat fmt) {
        int    sampleSize = fmt.getSampleSizeInBits() / 8;
        int    channels   = fmt.getChannels();
        boolean bigEndian = fmt.isBigEndian();
        int    numSamples = length / sampleSize;

        double sumSq = 0;
        for (int i = 0; i + sampleSize <= length; i += sampleSize) {
            int sample;
            if (sampleSize == 2) {
                // 16-bit sample
                sample = bigEndian
                        ? (buffer[i] << 8)     | (buffer[i + 1] & 0xFF)
                        : (buffer[i + 1] << 8) | (buffer[i]     & 0xFF);
            } else {
                // 8-bit sample
                sample = buffer[i] - 128;
            }
            sumSq += (double) sample * sample;
        }

        double rms    = Math.sqrt(sumSq / Math.max(1, numSamples));
        double maxVal = sampleSize == 2 ? 32768.0 : 128.0;
        return (float) Math.min(1.0, rms / maxVal);
    }

    // no longer needed but kept for ConfigManager compatibility
    public String getFilePath()      { return file != null ? file.getAbsolutePath() : null; }
    public double getStartSeconds()  { return startSeconds; }
}