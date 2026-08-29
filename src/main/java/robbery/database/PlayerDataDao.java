package robbery.database;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerDataDao {

    private final DatabaseManager databaseManager;

    public PlayerDataDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Loads a player's YAML configuration from the database.
     * @param uuid the player's UUID
     * @return the YamlConfiguration, or null if the player has no data in the database
     */
    public YamlConfiguration loadPlayerData(UUID uuid) {
        String sql = "SELECT data FROM player_data WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dataStr = rs.getString("data");
                    if (dataStr != null && !dataStr.isEmpty()) {
                        YamlConfiguration cfg = new YamlConfiguration();
                        cfg.loadFromString(dataStr);
                        return cfg;
                    }
                }
            }
        } catch (SQLException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Saves a player's YAML configuration and top-level stats to the database.
     * @param uuid the player's UUID
     * @param username the player's username
     * @param prestige the player's prestige
     * @param rankName the player's rank
     * @param skillpoints the player's skillpoints
     * @param itemsStolen the player's items stolen count
     * @param cfg the YamlConfiguration containing all serialized data
     */
    public void savePlayerData(UUID uuid, String username, int prestige, String rankName, int skillpoints, int itemsStolen, YamlConfiguration cfg) {
        String dataStr = cfg.saveToString();
        String sql = "INSERT INTO player_data (uuid, username, prestige, rank_name, skillpoints, items_stolen, data) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "username = VALUES(username), " +
                     "prestige = VALUES(prestige), " +
                     "rank_name = VALUES(rank_name), " +
                     "skillpoints = VALUES(skillpoints), " +
                     "items_stolen = VALUES(items_stolen), " +
                     "data = VALUES(data)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setInt(3, prestige);
            stmt.setString(4, rankName);
            stmt.setInt(5, skillpoints);
            stmt.setInt(6, itemsStolen);
            stmt.setString(7, dataStr);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
