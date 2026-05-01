import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

public class WaveformVisualizer extends JPanel {

    private static final int BAR_COUNT  = 48;
    private static final int BAR_WIDTH  = 5;
    private static final int BAR_GAP    = 3;
    private static final int MAX_HEIGHT = 52;

    private float[] barHeights = new float[BAR_COUNT];

    private Timer   animTimer;
    private Timer   clockTimer;

    private String  padName    = "";
    private String  spaceName  = "";
    private Color   accentColor = new Color(29, 158, 117);

    private long    lastPlayedAt = -1;
    private boolean isPlaying    = false;
    private float   currentRms   = 0f;  // latest amplitude from audio thread
    private float   displayRms   = 0f;  // smoothed value used for rendering

    private JButton stopButton;
    private Runnable onStop; // callback to stop the active PadSound

    /**
     * A fixed-height panel docked at the bottom of the main window
     * that visualizes audio playback in real time. Displays a scrolling
     * bar graph driven by RMS amplitude values fed from PadSound,
     * the name and space of the last triggered pad, a "last played"
     * elapsed timer, and a stop button that halts the active sound.
     * All rendering happens on the EDT via a Swing Timer at ~25fps.
     */
    public WaveformVisualizer() {
        setPreferredSize(new Dimension(0, 70));
        setBackground(new Color(18, 18, 18));
        setLayout(new BorderLayout());

        stopButton = buildStopButton();
        add(stopButton, BorderLayout.EAST);

        // animation tick — shift bars and push new value
        animTimer = new Timer(40, e -> {  // ~25fps is plenty
            if (isPlaying) {
                // smooth the RMS so bars don't flicker
                displayRms += (currentRms - displayRms) * 0.4f;
                pushBar(displayRms * 4);
            } else {
                // decay all bars toward zero
                boolean anyLeft = false;
                for (int i = 0; i < BAR_COUNT; i++) {
                    barHeights[i] *= 0.82f;
                    if (barHeights[i] > 0.5f) anyLeft = true;
                }
                if (!anyLeft) java.util.Arrays.fill(barHeights, 0f);
            }
            repaint();
        });
        animTimer.start();

        clockTimer = new Timer(1000, e -> repaint());
        clockTimer.start();

        updateStopButton();
    }

    // called from the audio thread — just store, don't draw here
    public void feedAmplitude(float rms) {
        this.currentRms = rms;
    }

    // push a new bar onto the right, shift everything left
    private void pushBar(float rms) {
        System.arraycopy(barHeights, 1, barHeights, 0, BAR_COUNT - 1);
        barHeights[BAR_COUNT - 1] = Math.min(MAX_HEIGHT, rms * MAX_HEIGHT);
    }

    public void notifyPlay(String padName, String spaceName,
                           Color accentColor, Runnable onStop) {
        this.padName     = padName;
        this.spaceName   = spaceName;
        this.accentColor = accentColor;
        this.onStop      = onStop;
        this.isPlaying   = true;
        this.lastPlayedAt = System.currentTimeMillis();
        updateStopButton();
    }

    public void notifyStop() {
        isPlaying  = false;
        currentRms = 0f;
        updateStopButton();
    }

    public void stopAll() {
        SpaceManager spaceManager = SpaceManager.getInstance();
        java.util.List<Space> spaces = spaceManager.getSpaces();
        for (Space space : spaces){
            java.util.List<PadButton> buttons = space.getPads();
            for (PadButton button : buttons){
                PadSound sound = button.getSound();
                if (sound != null)
                    sound.stop();
            }
        }
        if (onStop != null) onStop.run();
        notifyStop();
    }

    private void updateStopButton() {
        stopButton.setEnabled(isPlaying);
        stopButton.repaint();
    }

    private JButton buildStopButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                boolean en = isEnabled();
                g2.setColor(en ? new Color(200, 60, 60) : new Color(55, 55, 55));
                g2.fillOval(4, 4, getWidth() - 8, getHeight() - 8);
                int sq = 14;
                int cx = getWidth()  / 2 - sq / 2;
                int cy = getHeight() / 2 - sq / 2;
                g2.setColor(en ? Color.WHITE : new Color(90, 90, 90));
                g2.fillRect(cx, cy, sq, sq);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(70, 70));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setEnabled(false);
        btn.addActionListener(e -> stopAll());
        return btn;
    }

    /**
     * Main paint entry point. Delegates each visual section to a
     * dedicated method and disposes the graphics context when done.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int panelH = getHeight();
        int panelW = getWidth() - 70;
        int midY   = panelH / 2;

        drawAccentStripe(g2, panelH);
        drawPadLabels(g2, midY);
        drawWaveformBars(g2, midY);
        drawLastPlayed(g2, panelW, midY);

        g2.dispose();
    }

    /**
     * Draws the 3px colored stripe on the left edge using the current accent color.
     *
     * @param g2     graphics context
     * @param panelH full panel height for stripe extent
     */
    private void drawAccentStripe(Graphics2D g2, int panelH) {
        g2.setColor(accentColor);
        g2.fillRect(0, 0, 3, panelH);
    }

    /**
     * Draws the pad name and space name labels on the left side.
     * Shows "—" when no pad has been triggered yet.
     *
     * @param g2   graphics context
     * @param midY vertical center of the panel
     */
    private void drawPadLabels(Graphics2D g2, int midY) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(padName.isEmpty() ? "—" : padName, 14, midY - 4);

        g2.setColor(new Color(110, 110, 110));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(spaceName, 14, midY + 12);
    }

    /**
     * Draws the scrolling waveform bar graph.
     * Bar alpha scales with height so quiet bars fade out naturally.
     * Bars shorter than 0.5px are skipped entirely.
     *
     * @param g2   graphics context
     * @param midY vertical center used to mirror bars above and below
     */
    private void drawWaveformBars(Graphics2D g2, int midY) {
        int barsStartX = 110;
        for (int i = 0; i < BAR_COUNT; i++) {
            float h = barHeights[i];
            if (h < 0.5f) continue;

            float alpha = 0.25f + (h / MAX_HEIGHT) * 0.75f;
            g2.setColor(new Color(
                    accentColor.getRed()   / 255f,
                    accentColor.getGreen() / 255f,
                    accentColor.getBlue()  / 255f,
                    Math.min(1f, alpha)
            ));

            int x    = barsStartX + i * (BAR_WIDTH + BAR_GAP);
            int barH = Math.max(2, (int) h);
            g2.fill(new RoundRectangle2D.Float(
                    x, midY - barH / 2f, BAR_WIDTH, barH, 2, 2));
        }
    }

    /**
     * Draws the "last played" label and elapsed time on the right side.
     * Does nothing if no sound has been played yet in this session.
     * Time is shown as seconds for under a minute, minutes otherwise.
     *
     * @param g2     graphics context
     * @param panelW usable panel width (total width minus stop button width)
     * @param midY   vertical center of the panel
     */
    private void drawLastPlayed(Graphics2D g2, int panelW, int midY) {
        if (lastPlayedAt <= 0) return;

        long   elapsed = (System.currentTimeMillis() - lastPlayedAt) / 1000;
        String timeStr = elapsed < 60
                ? elapsed + "s ago"
                : (elapsed / 60) + "m ago";

        g2.setColor(new Color(90, 90, 90));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int lw = g2.getFontMetrics().stringWidth("last played");
        g2.drawString("last played", panelW - lw - 16, midY - 4);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        int tw = g2.getFontMetrics().stringWidth(timeStr);
        g2.drawString(timeStr, panelW - tw - 16, midY + 14);
    }
}