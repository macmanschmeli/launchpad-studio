import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

public class PadButton extends JButton {

    private static final DataFlavor PAD_FLAVOR =
            new DataFlavor(Integer.class, "Pad Index");

    private PadSound sound;
    private int padIndex;
    private Color activeColor  = new Color(0, 200, 80);
    private Color baseColor    = new Color(60, 60, 60);
    private Color currentColor = baseColor;
    private int arc = 20;
    private WaveformVisualizer visualizer;
    private Space space;
    private Clip clip;

    private GroupManager groupManager;
    private java.util.List<javax.sound.sampled.LineListener> cachedListeners = new java.util.ArrayList<>();

    public PadButton(String label, int padIndex, GroupManager groupManager) {
        super(label);
        this.padIndex      = padIndex;
        this.groupManager  = groupManager;

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 12));

        // action listener goes here, replacing the old one
        addActionListener(e -> {
            if (sound != null) {
                sound.play();
                this.clip = sound.getClip();

                for (javax.sound.sampled.LineListener l : cachedListeners) {
                    clip.removeLineListener(l);
                }
                cachedListeners.clear();

                javax.sound.sampled.LineListener listener = event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                        SwingUtilities.invokeLater(() -> {
                            if (visualizer != null) visualizer.notifyStop();
                        });
                    }
                };
                cachedListeners.add(listener);
                clip.addLineListener(listener);
            }
            flashActive();
        });

        setupDragSource();
        setupDropTarget();
        setupContextMenu();
    }

    public void setVisualizer(WaveformVisualizer visualizer, Space space) {
        this.visualizer = visualizer;
        this.space      = space;
    }

    public void setClip(Clip clip) {
        this.clip = clip;
    }

    private void flashActive() {
        currentColor = activeColor;
        repaint();

        if (visualizer != null) {
            visualizer.notifyPlay(getText(), space.getName(), space.getColor(), clip);
        }

        // only revert the button color after 150ms — do NOT notify the visualizer here
        Timer t = new Timer(150, e -> {
            currentColor = baseColor;
            repaint();
        });
        t.setRepeats(false);
        t.start();
    }

    // --- Drag source: export this pad's index when dragged ---
    private void setupDragSource() {
        DragSource ds = DragSource.getDefaultDragSource();
        ds.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_MOVE, dge -> {
                    Transferable transferable = new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[]{ PAD_FLAVOR };
                        }
                        public boolean isDataFlavorSupported(DataFlavor f) {
                            return f.equals(PAD_FLAVOR);
                        }
                        public Object getTransferData(DataFlavor f) {
                            return padIndex;
                        }
                    };
                    dge.startDrag(DragSource.DefaultMoveDrop, transferable);
                });
    }

    // --- Drop target: receive another pad's index and merge groups ---
    private void setupDropTarget() {
        new DropTarget(this, DnDConstants.ACTION_MOVE,
                new DropTargetAdapter() {
                    @Override
                    public void drop(DropTargetDropEvent dtde) {
                        try {
                            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                            int draggedIndex = (int) dtde.getTransferable()
                                    .getTransferData(PAD_FLAVOR);

                            if (draggedIndex != padIndex) {
                                groupManager.mergePads(draggedIndex, padIndex);
                                // notify the grid to refresh all button colors
                                SwingUtilities.getWindowAncestor(PadButton.this)
                                        .repaint();
                            }
                            dtde.dropComplete(true);
                        } catch (Exception ex) {
                            dtde.dropComplete(false);
                        }
                    }

                    @Override
                    public void dragOver(DropTargetDragEvent dtde) {
                        // highlight the target while hovering
                        currentColor = activeColor;
                        repaint();
                    }

                    @Override
                    public void dragExit(DropTargetEvent dte) {
                        refreshColor();
                        repaint();
                    }
                });
    }

    public void refreshColor() {
        baseColor    = groupManager.getColor(padIndex);
        currentColor = baseColor;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean contains(int x, int y) {
        return new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc)
                .contains(x, y);
    }

    public void setSound(PadSound sound) { this.sound = sound; }

    public void setBaseColor(Color color) {
        this.baseColor    = color;
        this.currentColor = color;
        repaint();
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem renameItem = new JMenuItem("Rename pad");
        renameItem.addActionListener(e -> {
            String newName = (String) JOptionPane.showInputDialog(
                    this, "Pad name:", "Rename pad",
                    JOptionPane.PLAIN_MESSAGE, null, null, getText()
            );
            if (newName != null && !newName.isBlank()) {
                setText(newName.trim());
            }
        });

        JMenuItem removeItem = new JMenuItem("Remove pad");
        removeItem.setForeground(new Color(200, 80, 80));
        removeItem.addActionListener(e -> {
            // fire a removal event that Main can listen to
            firePropertyChange("padRemoved", false, true);
        });

        contextMenu.add(renameItem);
        contextMenu.addSeparator();
        contextMenu.add(removeItem);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.show(PadButton.this, e.getX(), e.getY());
                }
            }
        });
    }
}
