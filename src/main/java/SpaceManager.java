import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SpaceManager {

    private static final Color[] SPACE_COLORS = {
            new Color(29, 158, 117),   // teal
            new Color(127, 119, 221),  // purple
            new Color(216, 90,  48),   // coral
            new Color(55,  138, 221),  // blue
            new Color(186, 117, 23),   // amber
    };

    private List<Space> spaces = new ArrayList<>();
    private int colorIndex = 0;

    public SpaceManager() {
        // one default space to start
        createSpace("Default");
    }

    public Space createSpace(String name) {
        Color color = SPACE_COLORS[colorIndex % SPACE_COLORS.length];
        colorIndex++;
        Space space = new Space(name, color);
        spaces.add(space);
        return space;
    }

    public void deleteSpace(Space space) {
        spaces.remove(space);
    }

    public List<Space> getSpaces() { return spaces; }
}