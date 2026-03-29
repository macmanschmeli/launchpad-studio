
import javax.swing.*;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class WaveformVisualizer extends JPanel {

    private static final int BAR_COUNT  = 40;
    private static final int BAR_WIDTH  = 4;
    private static final int BAR_GAP    = 3;
    private static final int MAX_HEIGHT = 50;

    private float[] barHeights    = new float[BAR_COUNT];
    private float[] barVelocities = new float[BAR_COUNT];

    private Timer   animTimer;
    private Timer   clockTimer;
    private boolean isPlaying  = false;

    private String padName    = "";
    private String spaceName  = "";
    private Color  accentColor = new Color(29, 158, 117);

    private long lastPlayedAt = -1;
    private Clip activeClip   = null;  // track the live clip

    private JButton stopButton;

    public WaveformVisualizer() {
        setPreferredSize(new Dimension(0, 70));
        setBackground(new Color(18, 18, 18));
        setLayout(new BorderLayout());

        stopButton = buildStopButton();
        add(stopButton, BorderLayout.EAST);

        animTimer = new Timer(16, e -> { updateBars(); repaint(); });
        animTimer.start();

        clockTimer = new Timer(1000, e -> repaint());
        clockTimer.start();

        updateStopButtonState();
    }

    private JButton buildStopButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // background circle
                boolean enabled = isEnabled();
                g2.setColor(enabled ? new Color(200, 60, 60) : new Color(60, 60, 60));
                g2.fillOval(4, 4, getWidth() - 8, getHeight() - 8);

                // stop square icon
                int sq = 14;
                int cx = getWidth()  / 2 - sq / 2;
                int cy = getHeight() / 2 - sq / 2;
                g2.setColor(enabled ? Color.WHITE : new Color(100, 100, 100));
                g2.fillRect(cx, cy, sq, sq);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(70, 70));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Stop");

        btn.addActionListener(e -> stopAll());

        return btn;
    }

    // called by PadButton when sound starts
    public void notifyPlay(String padName, String spaceName, Color accentColor, Clip clip) {
        this.padName      = padName;
        this.spaceName    = spaceName;
        this.accentColor  = accentColor;
        this.activeClip   = clip;
        this.isPlaying    = true;
        this.lastPlayedAt = System.currentTimeMillis();

        for (int i = 0; i < BAR_COUNT; i++) {
            float center = BAR_COUNT / 2f;
            float dist   = Math.abs(i - center) / center;
            barHeights[i] = MAX_HEIGHT * (1f - dist * 0.6f)
                    * (0.6f + (float) Math.random() * 0.4f);
        }

        updateStopButtonState();
    }

    public void notifyStop() {
        isPlaying  = false;
        activeClip = null;
        updateStopButtonState();
    }

    // hard stop — called by the button
    public void stopAll() {
        if (activeClip != null && activeClip.isRunning()) {
            activeClip.stop();
            activeClip.setFramePosition(0);
        }
        notifyStop();
    }

    private void updateStopButtonState() {
        stopButton.setEnabled(isPlaying);
        stopButton.repaint();
    }

    private void updateBars() {
        for (int i = 0; i < BAR_COUNT; i++) {
            if (isPlaying) {
                float target = MAX_HEIGHT * (0.3f + (float) Math.random() * 0.7f);
                barHeights[i] += (target - barHeights[i]) * 0.15f;
                barHeights[i]  = Math.max(3, Math.min(MAX_HEIGHT, barHeights[i]));
            } else {
                barHeights[i] *= 0.88f;
                if (barHeights[i] < 1) barHeights[i] = 0;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelH = getHeight();
        int panelW = getWidth() - 70; // reserve space for stop button
        int midY   = panelH / 2;

        // accent stripe
        g2.setColor(accentColor);
        g2.fillRect(0, 0, 3, panelH);

        // pad name
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(padName.isEmpty() ? "—" : padName, 14, midY - 4);

        // space name
        g2.setColor(new Color(120, 120, 120));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(spaceName, 14, midY + 12);

        // waveform bars
        int barsStartX = 110;
        for (int i = 0; i < BAR_COUNT; i++) {
            float h     = barHeights[i];
            float alpha = 0.3f + (h / MAX_HEIGHT) * 0.7f;
            g2.setColor(new Color(
                    accentColor.getRed()   / 255f,
                    accentColor.getGreen() / 255f,
                    accentColor.getBlue()  / 255f,
                    alpha
            ));
            int x    = barsStartX + i * (BAR_WIDTH + BAR_GAP);
            int barH = Math.max(2, (int) h);
            g2.fill(new RoundRectangle2D.Float(
                    x, midY - barH / 2f, BAR_WIDTH, barH, 2, 2));
        }

        // last played timer
        if (lastPlayedAt > 0) {
            long elapsed  = (System.currentTimeMillis() - lastPlayedAt) / 1000;
            String timeStr = elapsed < 60
                    ? elapsed + "s ago"
                    : (elapsed / 60) + "m ago";

            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String label = "last played";
            int labelW   = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, panelW - labelW - 16, midY - 4);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            int timeW = g2.getFontMetrics().stringWidth(timeStr);
            g2.drawString(timeStr, panelW - timeW - 16, midY + 14);
        }

        g2.dispose();
    }
}