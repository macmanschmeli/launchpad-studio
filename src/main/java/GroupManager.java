import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GroupManager {
    // predefined group colors
    private static final Color[] GROUP_COLORS = {
            new Color(60, 60, 60),    // 0 = default (ungrouped)
            new Color(180, 60, 60),   // 1 = red
            new Color(60, 120, 180),  // 2 = blue
            new Color(60, 160, 80),   // 3 = green
            new Color(160, 100, 0),   // 4 = amber
    };

    private Map<Integer, Integer> padToGroup = new HashMap<>(); // padIndex -> groupId

    public int getGroup(int padIndex) {
        return padToGroup.getOrDefault(padIndex, 0);
    }

    public void setGroup(int padIndex, int groupId) {
        padToGroup.put(padIndex, groupId);
    }

    // dragging padA onto padB puts padA into padB's group
    public void mergePads(int draggedPad, int targetPad) {
        int targetGroup = getGroup(targetPad);

        if (targetGroup == 0) {
            // target has no group yet — create a new one using target's index as group id
            int newGroup = targetPad + 1;
            setGroup(targetPad, newGroup);
            setGroup(draggedPad, newGroup);
        } else {
            // join the existing group
            setGroup(draggedPad, targetGroup);
        }
    }

    public Color getColor(int padIndex) {
        int group = getGroup(padIndex);
        if (group == 0) return GROUP_COLORS[0];
        // cycle through colors for groups beyond the predefined list
        return GROUP_COLORS[(group % (GROUP_COLORS.length - 1)) + 1];
    }
}
