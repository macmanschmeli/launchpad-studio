import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static SpaceManager spaceManager;
    private static SpaceListPanel spaceListPanel;
    private static JPanel padArea;       // right side — swapped when space changes
    private static JPanel mainWrapper;   // holds padArea
    private static WaveformVisualizer visualizer = new WaveformVisualizer();


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            spaceManager = SpaceManager.getInstance();
            visualizer   = new WaveformVisualizer();
            padArea      = new JPanel();
            mainWrapper  = new JPanel(new BorderLayout());
            mainWrapper.setBackground(new Color(20, 20, 20));
            mainWrapper.add(padArea, BorderLayout.CENTER);

            spaceListPanel = new SpaceListPanel(spaceManager, Main::showSpace);

            // restore saved config if it exists
            restoreConfig();

            JPanel content = new JPanel(new BorderLayout());
            content.add(spaceListPanel, BorderLayout.WEST);
            content.add(mainWrapper,    BorderLayout.CENTER);

            JFrame frame = new JFrame("Launchpad Studio");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            // save on close
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) {
                    ConfigManager.save(spaceManager);
                    System.exit(0);
                }
            });

            frame.setLayout(new BorderLayout());
            frame.add(spaceListPanel, BorderLayout.WEST);
            frame.add(mainWrapper,    BorderLayout.CENTER);
            frame.add(visualizer,     BorderLayout.SOUTH);
            frame.setSize(820, 650);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // select first space after UI is built
            if (!spaceManager.getSpaces().isEmpty()) {
                spaceListPanel.selectSpace(spaceManager.getSpaces().get(0));
            }
        });
    }

    private static void restoreConfig() {
        ConfigManager.AppConfig config = ConfigManager.load();
        if (config == null || config.spaces.isEmpty()) return;

        // clear the default space created by SpaceManager constructor
        for (ConfigManager.SpaceData sd : config.spaces) {
            Color color = new Color(sd.colorRgb, true);
            Space space = new Space(sd.name, color);

            for (ConfigManager.PadData pd : sd.pads) {
                PadButton btn = new PadButton(pd.name, 0, null);
                btn.setBaseColor(color.darker());
                btn.setVisualizer(visualizer, space);

                if (pd.soundFilePath != null) {
                    File file = new File(pd.soundFilePath);
                    if (file.exists()) {
                        btn.setSoundFromFile(file, pd.startTimestamp);
                    } else {
                        System.err.println("Sound file missing: " + pd.soundFilePath);
                    }
                }

                space.addPad(btn);
            }

            spaceManager.addSpace(space);
            spaceListPanel.refresh();
        }
    }

    private static void showSpace(Space space) {
        int cols = 4;

        if (space.getPads().isEmpty()) {
            for (int i = 0; i < cols * 2; i++) {
                PadButton btn = new PadButton("Pad " + (i + 1), i, null);
                space.addPad(btn);
            }
        }

        JPanel grid = new JPanel(new GridLayout(0, cols, 10, 10));
        grid.setBackground(new Color(25, 25, 25));
        grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        for (PadButton pad : space.getPads()) {
            pad.setBaseColor(space.getColor().darker());
            pad.setVisualizer(visualizer, space); // use the shared visualizer
            pad.addPropertyChangeListener("padRemoved", evt -> {
                if (space.getPads().size() <= 1) {
                    JOptionPane.showMessageDialog(null,
                            "A space must have at least one pad.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(null,
                        "Remove \"" + pad.getText() + "\"?", "Remove pad",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    space.removePad(pad);
                    showSpace(space);
                }
            });
            grid.add(pad);
        }

        // add pad button
        JButton addPad = new JButton("+ Pad");
        addPad.setBackground(new Color(50, 50, 50));
        addPad.setForeground(new Color(180, 180, 180));
        addPad.setBorderPainted(false);
        addPad.setFocusPainted(false);
        addPad.setFont(new Font("SansSerif", Font.PLAIN, 12));
        addPad.addActionListener(e -> {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(mainWrapper);
            AddPadDialog dialog = new AddPadDialog(parentFrame);
            dialog.setVisible(true);

            if (!dialog.isConfirmed()) return;

            AddPadDialog.PadConfig cfg = dialog.getResult();

            int idx = space.getPads().size();
            PadButton btn = new PadButton(cfg.name, idx, null);
            btn.setBaseColor(space.getColor().darker());
            btn.setVisualizer(visualizer, space); // use the shared visualizer

            if (cfg.soundFile != null) {
                try {
                    //PadSound sound = PadSound.fromFile(cfg.soundFile, cfg.startTimestamp);
                    //btn.setSound(sound);
                    btn.setSoundFromFile(cfg.soundFile, cfg.startTimestamp);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainWrapper,
                            "Could not load sound: " + ex.getMessage(),
                            "Audio error", JOptionPane.ERROR_MESSAGE);
                }
            }

            space.addPad(btn);
            showSpace(space);
        });

        grid.add(addPad); // this was missing

        JLabel header = new JLabel(space.getName());
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 0, 0));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(20, 20, 20));
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(grid,   BorderLayout.CENTER);

        mainWrapper.remove(padArea);
        padArea = wrapper;
        mainWrapper.add(padArea, BorderLayout.CENTER);
        mainWrapper.revalidate();
        mainWrapper.repaint();
    }
    private static void styleAddButton(JButton btn) {
        btn.setBackground(new Color(50, 50, 50));
        btn.setForeground(new Color(180, 180, 180));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }
}