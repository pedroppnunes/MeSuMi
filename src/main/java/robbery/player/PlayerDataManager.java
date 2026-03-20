package robbery.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages PlayerData instances for all online players.
 * <p>
 * Provides methods to get, set, and retrieve the storage folder path for a player.
 * Uses a thread-safe ConcurrentHashMap to store PlayerData instances keyed by the player's UUID string.
 */
public class PlayerDataManager {

    /** Map storing PlayerData instances keyed by player UUID as a string. */
    private static final Map<String, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    /**
     * Retrieves the PlayerData for the given player.
     * If the player does not have existing data, a new PlayerData instance is created and stored.
     *
     * @param p the player to retrieve data for
     * @return the PlayerData instance associated with the player
     */
    public static PlayerData getPlayerData(Player p) {
        if (!playerDataMap.containsKey(p.getUniqueId().toString())) {
            PlayerData m = new PlayerData(p);
            playerDataMap.put(p.getUniqueId().toString(), m);
            return m;
        }
        return playerDataMap.get(p.getUniqueId().toString());
    }

    /**
     * Sets or updates the PlayerData for a player.
     * If the provided PlayerData is null, the player’s data is removed.
     *
     * @param p the player whose data is being set
     * @param m the PlayerData instance to associate with the player, or null to remove
     */
    public static void setPlayerData(Player p, PlayerData m) {
        if (m == null) playerDataMap.remove(p.getUniqueId().toString());
        else playerDataMap.put(p.getUniqueId().toString(), m);
    }

    /**
     * Returns the folder path for storing a player's data files.
     *
     * @param p the player whose folder path is being retrieved
     * @return the absolute path to the player’s data folder
     */
    public static String getFolderPath(Player p) {
        return Bukkit.getPluginsFolder().getAbsolutePath() + "/player/" + p.getUniqueId();
    }

    public static Map<String, PlayerData> getAllPlayers(){
        return playerDataMap;
    }
}
