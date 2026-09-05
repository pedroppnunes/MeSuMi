package robbery.core;

import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DisqualificationManager {

    private final Robbery plugin;
    private final File dqFile;
    private YamlConfiguration dqConfig;

    private final Map<UUID, String> dqPlayers = new HashMap<>();
    private final Map<UUID, String> dqIslands = new HashMap<>();

    public DisqualificationManager(Robbery plugin) {
        this.plugin = plugin;
        this.dqFile = new File(plugin.getDataFolder(), "disqualifications.yml");
        load();
    }

    public synchronized void load() {
        if (!dqFile.exists()) {
            try {
                dqFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create disqualifications.yml: " + e.getMessage());
            }
        }
        dqConfig = YamlConfiguration.loadConfiguration(dqFile);
        dqPlayers.clear();
        dqIslands.clear();

        if (dqConfig.isConfigurationSection("players")) {
            for (String key : dqConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = dqConfig.getString("players." + key, "Unknown");
                    dqPlayers.put(uuid, name);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (dqConfig.isConfigurationSection("islands")) {
            for (String key : dqConfig.getConfigurationSection("islands").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = dqConfig.getString("islands." + key, "Unknown");
                    dqIslands.put(uuid, name);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        dqConfig.set("players", null);
        dqConfig.set("islands", null);

        for (Map.Entry<UUID, String> entry : dqPlayers.entrySet()) {
            dqConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }

        for (Map.Entry<UUID, String> entry : dqIslands.entrySet()) {
            dqConfig.set("islands." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dqConfig.save(dqFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save disqualifications.yml: " + e.getMessage());
        }
    }

    /* ---------------- Player DQ ---------------- */

    public boolean isPlayerDisqualified(UUID playerUuid) {
        if (playerUuid == null) return false;
        return dqPlayers.containsKey(playerUuid);
    }

    public boolean isPlayerDisqualified(Player player) {
        if (player == null) return false;
        return isPlayerDisqualified(player.getUniqueId());
    }

    public void disqualifyPlayer(UUID playerUuid, String name) {
        if (playerUuid == null) return;
        dqPlayers.put(playerUuid, name != null ? name : "Unknown");
        save();
    }

    public boolean undisqualifyPlayer(UUID playerUuid) {
        if (playerUuid == null) return false;
        boolean removed = dqPlayers.remove(playerUuid) != null;
        if (removed) save();
        return removed;
    }

    public Map<UUID, String> getDisqualifiedPlayers() {
        return Collections.unmodifiableMap(dqPlayers);
    }

    /* ---------------- Island DQ ---------------- */

    public boolean isIslandDisqualified(UUID islandUuid) {
        if (islandUuid == null) return false;
        return dqIslands.containsKey(islandUuid);
    }

    public boolean isIslandDisqualified(Island island) {
        if (island == null) return false;
        return isIslandDisqualified(island.getUniqueId());
    }

    public void disqualifyIsland(UUID islandUuid, String name) {
        if (islandUuid == null) return;
        dqIslands.put(islandUuid, name != null ? name : "Unknown");
        save();
    }

    public boolean undisqualifyIsland(UUID islandUuid) {
        if (islandUuid == null) return false;
        boolean removed = dqIslands.remove(islandUuid) != null;
        if (removed) save();
        return removed;
    }

    public Map<UUID, String> getDisqualifiedIslands() {
        return Collections.unmodifiableMap(dqIslands);
    }
}
