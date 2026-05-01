
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Sidebar panel that lists all spaces as clickable rows.
 * Single-click selects a space and fires the onSpaceSelected callback.
 * Double-click opens an inline rename dialog.
 * Right-click shows a context menu with rename and delete options.
 * Call refresh() after any structural change to the space list.
 */
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

    /**
     * Builds the complete row panel for a space entry in the sidebar.
     * Composes the dot, label, context menu and mouse listeners.
     */
    private JPanel buildSpaceRow(Space space) {
        JLabel nameLabel  = buildNameLabel(space);
        JPanel row        = buildRowPanel(space);
        JPopupMenu menu   = buildContextMenu(space, nameLabel);

        row.add(buildDot(space),  BorderLayout.WEST);
        row.add(nameLabel,        BorderLayout.CENTER);

        attachMouseListener(row, space, nameLabel, menu);

        return row;
    }

    /**
     * Creates and styles the outer row panel with correct background,
     * size constraints, cursor and rounded border.
     *
     * @param space used to determine active highlight color
     */
    private JPanel buildRowPanel(Space space) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBackground(activeSpace == space
                ? space.getColor().darker()
                : new Color(50, 50, 50));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(6, 10, 6, 6)
        ));
        return row;
    }

    /**
     * Creates a small circle panel painted in the space's accent color.
     * Used as a visual identifier on the left side of each row.
     */
    private JPanel buildDot(Space space) {
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(space.getColor());
                g2.fillOval(0, 3, 10, 10);
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 16));
        return dot;
    }

    /**
     * Creates the label showing the space name.
     */
    private JLabel buildNameLabel(Space space) {
        JLabel label = new JLabel(space.getName());
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        return label;
    }

    /**
     * Builds the right-click context menu with rename and delete actions.
     *
     * @param space     the space this menu acts on
     * @param nameLabel updated in-place when the space is renamed
     */
    private JPopupMenu buildContextMenu(Space space, JLabel nameLabel) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> startRename(space, nameLabel));

        JMenuItem deleteItem = new JMenuItem("Delete space");
        deleteItem.setForeground(new Color(200, 80, 80));
        deleteItem.addActionListener(e -> handleDeleteSpace(space));

        menu.add(renameItem);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    /**
     * Confirms and performs deletion of a space.
     * Refuses if only one space remains.
     * Falls back to the first remaining space after deletion.
     */
    private void handleDeleteSpace(Space space) {
        if (spaceManager.getSpaces().size() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "You must keep at least one space.");
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
            refresh();
            selectSpace(spaceManager.getSpaces().get(0));
        }
    }

    /**
     * Attaches mouse behavior to a row panel.
     * Right-click shows the context menu.
     * Single left-click selects the space.
     * Double left-click opens the rename dialog.
     *
     * @param row       the panel receiving the listener
     * @param space     the space this row represents
     * @param nameLabel passed through to the rename dialog
     * @param menu      the context menu to show on right-click
     */
    private void attachMouseListener(JPanel row, Space space,
                                     JLabel nameLabel, JPopupMenu menu) {
        row.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(row, e.getX(), e.getY());
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