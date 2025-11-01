package robbery.prestige;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.Robbery;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PrestigeCountManager {

    private static final Map<Integer, Integer> prestigeCounts = new HashMap<>();
    private static File file;
    private static FileConfiguration config;

    public static void load() {
        file = new File(Robbery.getInstance().getDataFolder(), "prestige_counts.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("PrestigeCounts")) {
            for (String key : config.getConfigurationSection("PrestigeCounts").getKeys(false)) {
                int prestige = Integer.parseInt(key);
                int count = config.getInt("PrestigeCounts." + key);
                prestigeCounts.put(prestige, count);
            }
        }
    }

    public static void save() {
        for (Map.Entry<Integer, Integer> entry : prestigeCounts.entrySet()) {
            config.set("PrestigeCounts." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Increment and return the new position for this prestige.
     *
     * @param prestige the prestige number the player reached
     * @return the position (1st, 2nd, 3rd, etc.)
     */
    public static int incrementPrestigeCount(int prestige) {
        int newCount = prestigeCounts.getOrDefault(prestige, 0) + 1;
        prestigeCounts.put(prestige, newCount);
        save();
        return newCount;
    }
}

