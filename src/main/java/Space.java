import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a named group of pads with a shared accent color.
 * Owns its pad list — pads are not shared between spaces.
 * Spaces are created and managed by SpaceManager.
 */
public class Space {

    private String id;
    private String name;
    private Color color;
    private List<PadButton> pads = new ArrayList<>();

    public Space(String name, Color color) {
        this.id    = UUID.randomUUID().toString();
        this.name  = name;
        this.color = color;
    }

    public void addPad(PadButton pad) { pads.add(pad); }
    public void removePad(PadButton pad) { pads.remove(pad); }

    public String getId()        { return id; }
    public String getName()      { return name; }
    public void setName(String n){ this.name = n; }
    public Color getColor()      { return color; }
    public List<PadButton> getPads() { return pads; }
}