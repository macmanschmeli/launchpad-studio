
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class AddPadDialog extends JDialog {

    public static class PadConfig {
        public String name;
        public File   soundFile;
        public double startTimestamp; // in seconds, 0 = from beginning
    }

    private JTextField nameField;
    private JLabel     soundLabel;
    private JSpinner   timestampSpinner;
    private File       selectedFile;
    private boolean    confirmed = false;

    public AddPadDialog(Frame parent) {
        super(parent, "Add pad", true); // true = modal
        setSize(420, 340);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(28, 28, 28));

        // --- scrollable content ---
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(28, 28, 28));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel("New pad");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(18));

        content.add(fieldLabel("Pad name"));
        content.add(Box.createVerticalStrut(4));
        nameField = new JTextField("New pad");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        nameField.setAlignmentX(LEFT_ALIGNMENT);
        styleTextField(nameField);
        content.add(nameField);
        content.add(Box.createVerticalStrut(14));

        content.add(fieldLabel("Sound file"));
        content.add(Box.createVerticalStrut(4));

        JPanel soundRow = new JPanel(new BorderLayout(8, 0));
        soundRow.setOpaque(false);
        soundRow.setAlignmentX(LEFT_ALIGNMENT);
        soundRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        soundLabel = new JLabel("No file selected");
        soundLabel.setForeground(new Color(140, 140, 140));
        soundLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        soundLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JButton browseBtn = new JButton("Browse");
        browseBtn.setPreferredSize(new Dimension(80, 34));
        styleButton(browseBtn);
        browseBtn.addActionListener(e -> pickFile());

        soundRow.add(soundLabel, BorderLayout.CENTER);
        soundRow.add(browseBtn,  BorderLayout.EAST);
        content.add(soundRow);
        content.add(Box.createVerticalStrut(14));

        content.add(fieldLabel("Start at (seconds)  —  optional"));
        content.add(Box.createVerticalStrut(4));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, 3600.0, 0.1);
        timestampSpinner = new JSpinner(spinnerModel);
        timestampSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        timestampSpinner.setAlignmentX(LEFT_ALIGNMENT);
        styleSpinner(timestampSpinner);
        content.add(timestampSpinner);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(28, 28, 28));
        scrollPane.getViewport().setBackground(new Color(28, 28, 28));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // --- fixed footer (never scrolls away) ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        footer.setBackground(new Color(35, 35, 35));
        footer.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(55, 55, 55))); // top separator line

        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn);
        cancelBtn.addActionListener(e -> dispose());

        JButton addBtn = new JButton("Add pad");
        addBtn.setBackground(new Color(29, 158, 117));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setPreferredSize(new Dimension(90, 32));
        addBtn.addActionListener(e -> confirm());

        footer.add(cancelBtn);
        footer.add(addBtn);

        // wire Enter key to confirm
        getRootPane().setDefaultButton(addBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(footer,     BorderLayout.SOUTH);
    }

    private void pickFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a sound file");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Audio files (wav, mp3, aiff)", "wav", "mp3", "mp2", "aiff", "aif"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            // truncate long filenames for display
            String display = selectedFile.getName();
            if (display.length() > 35) display = display.substring(0, 32) + "...";
            soundLabel.setText(display);
            soundLabel.setForeground(Color.WHITE);
        }
    }

    private void confirm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            shake();
            nameField.requestFocus();
            return;
        }
        confirmed = true;
        dispose();
    }

    // wobble the dialog if validation fails
    private void shake() {
        Point origin = getLocation();
        Timer t = new Timer(30, null);
        int[] offsets = {-8, 8, -6, 6, -4, 4, 0};
        final int[] step = {0};
        t.addActionListener(e -> {
            setLocation(origin.x + offsets[step[0]], origin.y);
            step[0]++;
            if (step[0] >= offsets.length) {
                setLocation(origin);
                t.stop();
            }
        });
        t.start();
    }

    // --- result accessors ---
    public boolean isConfirmed()  { return confirmed; }

    public PadConfig getResult() {
        PadConfig cfg = new PadConfig();
        cfg.name           = nameField.getText().trim();
        cfg.soundFile      = selectedFile;
        cfg.startTimestamp = (Double) timestampSpinner.getValue();
        return cfg;
    }

    // --- styling helpers ---
    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(160, 160, 160));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleTextField(JTextField field) {
        field.setBackground(new Color(40, 40, 40));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(40, 40, 40));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.00");
        spinner.setEditor(editor);
        editor.getTextField().setBackground(new Color(40, 40, 40));
        editor.getTextField().setForeground(Color.WHITE);
        editor.getTextField().setCaretColor(Color.WHITE);
        editor.getTextField().setFont(new Font("SansSerif", Font.PLAIN, 13));
        editor.getTextField().setBorder(
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    private void styleButton(JButton btn) {
        btn.setBackground(new Color(50, 50, 50));
        btn.setForeground(new Color(200, 200, 200));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(80, 32));
    }
}