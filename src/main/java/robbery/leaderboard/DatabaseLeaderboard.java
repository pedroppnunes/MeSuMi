package robbery.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.core.Robbery;
import robbery.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseLeaderboard {

    public static class Entry {
        public final String username;
        public final int value;
        public Entry(String username, int value) {
            this.username = username;
            this.value = value;
        }
    }

    private static final List<Entry> topPrestige = new ArrayList<>();
    private static final List<Entry> topItemsStolen = new ArrayList<>();

    public static void startTask(Robbery plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaderboards(plugin);
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 1200L); // Update every 1 minute (1200 ticks)
    }

    private static void updateLeaderboards(Robbery plugin) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return;

        List<Entry> newPrestige = fetchTop(db, "prestige");
        List<Entry> newItems = fetchTop(db, "items_stolen");

        synchronized (topPrestige) {
            topPrestige.clear();
            topPrestige.addAll(newPrestige);
        }

        synchronized (topItemsStolen) {
            topItemsStolen.clear();
            topItemsStolen.addAll(newItems);
        }
    }

    private static List<Entry> fetchTop(DatabaseManager db, String column) {
        List<Entry> validEntries = new ArrayList<>();
        String sql = "SELECT uuid, username, " + column + " FROM player_data ORDER BY " + column + " DESC LIMIT 50";
        
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next() && validEntries.size() < 10) {
                String uuidStr = rs.getString("uuid");
                String username = rs.getString("username");
                int value = rs.getInt(column);
                
                if (uuidStr == null || username == null) continue;
                
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.isOp()) continue;
                    
                    if (Robbery.getPermissions() != null) {
                        if (Robbery.getPermissions().playerHas(null, op, "robbery.op") ||
                            Robbery.getPermissions().playerHas(null, op, "robbery.bypass")) {
                            continue;
                        }
                    }
                    
                    validEntries.add(new Entry(username, value));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception e) {
            Robbery.getInstance().getLogger().warning("Failed to fetch top " + column + ": " + e.getMessage());
        }
        
        return validEntries;
    }

    public static String getTopPrestigeName(int position) {
        synchronized (topPrestige) {
            if (position < 1 || position > topPrestige.size()) return "Unknown";
            return topPrestige.get(position - 1).username;
        }
    }

    public static String getTopPrestigeValue(int position) {
        synchronized (topPrestige) {
            if (position < 1 || position > topPrestige.size()) return "0";
            return String.valueOf(topPrestige.get(position - 1).value);
        }
    }
    
    public static String getTopItemsStolenName(int position) {
        synchronized (topItemsStolen) {
            if (position < 1 || position > topItemsStolen.size()) return "Unknown";
            return topItemsStolen.get(position - 1).username;
        }
    }

    public static String getTopItemsStolenValue(int position) {
        synchronized (topItemsStolen) {
            if (position < 1 || position > topItemsStolen.size()) return "0";
            return String.valueOf(topItemsStolen.get(position - 1).value);
        }
    }
}
