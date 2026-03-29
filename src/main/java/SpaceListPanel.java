
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class SpaceListPanel extends JPanel {

    private SpaceManager spaceManager;
    private Consumer<Space> onSpaceSelected;
    private Space activeSpace;

    public SpaceListPanel(SpaceManager spaceManager, Consumer<Space> onSpaceSelected) {
        this.spaceManager     = spaceManager;
        this.onSpaceSelected  = onSpaceSelected;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(30, 30, 30));
        setPreferredSize(new Dimension(180, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        refresh();
    }

    public void refresh() {
        removeAll();

        JLabel title = new JLabel("Spaces");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setBorder(BorderFactory.createEmptyBorder(4, 4, 12, 0));
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);

        for (Space space : spaceManager.getSpaces()) {
            add(buildSpaceRow(space));
            add(Box.createVerticalStrut(6));
        }

        // "Add space" button
        JButton addBtn = new JButton("+ New space");
        addBtn.setForeground(new Color(180, 180, 180));
        addBtn.setBackground(new Color(50, 50, 50));
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        addBtn.setAlignmentX(LEFT_ALIGNMENT);
        addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Space name:", "New space", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.isBlank()) {
                Space created = spaceManager.createSpace(name.trim());
                refresh();
                selectSpace(created);
            }
        });
        add(addBtn);

        revalidate();
        repaint();
    }

    private JPanel buildSpaceRow(Space space) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBackground(activeSpace == space
                ? space.getColor().darker()
                : new Color(50, 50, 50));
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // colored dot to identify the space
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(space.getColor());
                g2.fillOval(0, 3, 10, 10);
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 16));

        JLabel nameLabel = new JLabel(space.getName());
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        // delete button (shown on hover)

        row.add(dot, BorderLayout.WEST);
        row.add(nameLabel, BorderLayout.CENTER);

        // round the row corners via a custom border
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(6, 10, 6, 6)
        ));
        // right-click context menu
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> startRename(space, nameLabel));

        JMenuItem deleteItem = new JMenuItem("Delete space");
        deleteItem.setForeground(new Color(200, 80, 80));
        deleteItem.addActionListener(e -> {
            if (spaceManager.getSpaces().size() <= 1) {
                JOptionPane.showMessageDialog(this, "You must keep at least one space.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + space.getName() + "\"?",
                    "Delete space",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                spaceManager.deleteSpace(space);
                Space fallback = spaceManager.getSpaces().get(0);
                refresh();
                selectSpace(fallback);
            }
        });

        contextMenu.add(renameItem);
        contextMenu.addSeparator();
        contextMenu.add(deleteItem);

        row.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(row, e.getX(), e.getY());
                }
            }

            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                if (e.getClickCount() == 2) {
                    startRename(space, nameLabel);
                } else {
                    selectSpace(space);
                }
            }

        });

        return row;
    }

    private void startRename(Space space, JLabel nameLabel) {
        String newName = (String) JOptionPane.showInputDialog(
                this, "Rename space:", "Rename",
                JOptionPane.PLAIN_MESSAGE, null, null, space.getName());
        if (newName != null && !newName.isBlank()) {
            space.setName(newName.trim());
            nameLabel.setText(space.getName());
            refresh();
        }
    }

    public void selectSpace(Space space) {
        this.activeSpace = space;
        onSpaceSelected.accept(space);
        refresh();
    }

    public Space getActiveSpace() { return activeSpace; }
}