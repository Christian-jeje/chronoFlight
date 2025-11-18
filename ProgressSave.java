import java.io.*;
import java.util.Properties;

/**
 * Simple progress save/load using a properties file.
 * Saves: unlockedLevel (1..10), coins, highScore
 */
public class ProgressSave {

    private static final String FILE_NAME = "progress.properties";

    public static class Progress {
        public int unlockedLevel = 1;
        public int coins = 0;
        public int highScore = 0;
    }

    public static Progress load() {
        Progress p = new Progress();
        Properties props = new Properties();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            // default progress
            save(p);
            return p;
        }
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            p.unlockedLevel = Integer.parseInt(props.getProperty("unlockedLevel", "1"));
            p.coins = Integer.parseInt(props.getProperty("coins", "0"));
            p.highScore = Integer.parseInt(props.getProperty("highScore", "0"));
        } catch (Exception e) {
            System.err.println("Error loading progress: " + e.getMessage());
            // return defaults if parse fails
        }
        return p;
    }

    public static void save(Progress p) {
        Properties props = new Properties();
        props.setProperty("unlockedLevel", String.valueOf(Math.max(1, Math.min(10, p.unlockedLevel))));
        props.setProperty("coins", String.valueOf(Math.max(0, p.coins)));
        props.setProperty("highScore", String.valueOf(Math.max(0, p.highScore)));
        try (OutputStream out = new FileOutputStream(FILE_NAME)) {
            props.store(out, "FlappyGame Progress");
        } catch (Exception e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }
}
