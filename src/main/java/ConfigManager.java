import com.google.gson.*;
import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of the application configuration.
 * Saves and loads the full space/pad layout as a JSON file
 * written to the working directory alongside the JAR.
 * Separates deserialization (load) from object reconstruction
 * (restore) so config can be validated before being applied.
 */
public class ConfigManager {

    // --- data classes that mirror the runtime model ---

    public static class PadData {
        public String name;
        public String soundFilePath;  // absolute path, or null for bundled
        public double startTimestamp;
    }

    public static class SpaceData {
        public String      name;
        public int         colorRgb;  // packed int from Color.getRGB()
        public List<PadData> pads = new ArrayList<>();
    }

    public static class AppConfig {
        public List<SpaceData> spaces = new ArrayList<>();
    }

    // --- path resolution ---

    private static Path getConfigPath() {
        // store next to the JAR / working directory
        return Paths.get(System.getProperty("user.dir"), "launchpad-config.json");
    }

    // --- save ---

    public static void save(SpaceManager spaceManager) {
        AppConfig config = new AppConfig();

        for (Space space : spaceManager.getSpaces()) {
            SpaceData sd = new SpaceData();
            sd.name     = space.getName();
            sd.colorRgb = space.getColor().getRGB();

            for (PadButton pad : space.getPads()) {
                PadData pd = new PadData();
                pd.name           = pad.getText();
                pd.soundFilePath  = pad.getSoundFilePath();
                pd.startTimestamp = pad.getStartTimestamp();
                sd.pads.add(pd);
            }

            config.spaces.add(sd);
        }

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(getConfigPath(), gson.toJson(config));
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    // --- load ---

    public static AppConfig load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return null;

        try {
            String json = Files.readString(path);
            return new Gson().fromJson(json, AppConfig.class);
        } catch (Exception e) {
            System.err.println("Could not load config: " + e.getMessage());
            return null;
        }
    }
}