package robbery.crypto;

import robbery.core.Robbery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CryptoMachineDao {

    private final Robbery plugin;

    public CryptoMachineDao(Robbery plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Table creation
                String createTable = "CREATE TABLE IF NOT EXISTS crypto_machines (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "world VARCHAR(64), " +
                        "x INT, y INT, z INT, " +
                        "unclaimed_money BIGINT DEFAULT 0, " +
                        "fuel_ticks BIGINT DEFAULT 0, " +
                        "fuel_quality DOUBLE DEFAULT 0.0, " +
                        "speed_level INT DEFAULT 0, " +
                        "fuel_time_level INT DEFAULT 0, " +
                        "reward_level INT DEFAULT 0, " +
                        "stored_fuels TEXT" +
                        ")";
                try (PreparedStatement stmt = conn.prepareStatement(createTable)) {
                    stmt.executeUpdate();
                }

                // Migration for existing tables
                addColumnIfNotExists(conn, "speed_level", "INT DEFAULT 0");
                addColumnIfNotExists(conn, "fuel_time_level", "INT DEFAULT 0");
                addColumnIfNotExists(conn, "reward_level", "INT DEFAULT 0");
                addColumnIfNotExists(conn, "last_updated", "BIGINT DEFAULT 0");
                addColumnIfNotExists(conn, "stored_fuels", "TEXT");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void addColumnIfNotExists(Connection conn, String column, String type) {
        try {
            String query = "ALTER TABLE crypto_machines ADD COLUMN " + column + " " + type;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.executeUpdate();
            }
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    
    public CompletableFuture<java.util.List<CryptoMachine>> loadAllMachines() {
        return CompletableFuture.supplyAsync(() -> {
            java.util.List<CryptoMachine> machines = new java.util.ArrayList<>();
            String query = "SELECT * FROM crypto_machines";
            try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                 
                java.sql.ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    machines.add(parseMachine(rs));
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
            return machines;
        });
    }

    public CompletableFuture<CryptoMachine> loadMachine(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT * FROM crypto_machines WHERE uuid = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                 
                stmt.setString(1, ownerId.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return parseMachine(rs);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new CryptoMachine(ownerId, null, null, null, null, 0, 0, 0.0, 0, 0, 0, 0L);
        });
    }

    private CryptoMachine parseMachine(ResultSet rs) throws SQLException {
        UUID ownerId = UUID.fromString(rs.getString("uuid"));
        String world = rs.getString("world");
        Integer x = rs.getObject("x") != null ? rs.getInt("x") : null;
        Integer y = rs.getObject("y") != null ? rs.getInt("y") : null;
        Integer z = rs.getObject("z") != null ? rs.getInt("z") : null;
        
        long money = rs.getLong("unclaimed_money");
        long fuel = rs.getLong("fuel_ticks");
        double quality = rs.getDouble("fuel_quality");
        
        int speedLvl = getIntSafe(rs, "speed_level", 0);
        int fuelTimeLvl = getIntSafe(rs, "fuel_time_level", 0);
        int rewardLvl = getIntSafe(rs, "reward_level", 0);
        long lastUpdated = getLongSafe(rs, "last_updated", 0L);
        
        // Backwards compatibility if machine_level column was present
        if (hasColumn(rs, "machine_level") && speedLvl == 0 && rewardLvl == 0) {
            int oldLvl = rs.getInt("machine_level");
            speedLvl = oldLvl;
            rewardLvl = oldLvl;
        }

        CryptoMachine machine = new CryptoMachine(ownerId, world, x, y, z, money, fuel, quality, speedLvl, fuelTimeLvl, rewardLvl, lastUpdated);
        
        // Parse stored fuels
        String storedRaw = getStringSafe(rs, "stored_fuels");
        if (storedRaw != null && !storedRaw.isEmpty()) {
            String[] parts = storedRaw.split(";");
            for (String part : parts) {
                if (part.contains(":")) {
                    String[] sub = part.split(":");
                    try {
                        UUID fId = UUID.fromString(sub[0]);
                        double fQual = Double.parseDouble(sub[1]);
                        machine.addStoredFuel(new StoredFuel(fId, fQual));
                    } catch (Exception ignored) {}
                }
            }
        }

        return machine;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private int getIntSafe(ResultSet rs, String col, int def) {
        try {
            return rs.getObject(col) != null ? rs.getInt(col) : def;
        } catch (SQLException e) {
            return def;
        }
    }

    private long getLongSafe(ResultSet rs, String col, long def) {
        try {
            return rs.getObject(col) != null ? rs.getLong(col) : def;
        } catch (SQLException e) {
            return def;
        }
    }

    private String getStringSafe(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (SQLException e) {
            return null;
        }
    }

    public void saveMachine(CryptoMachine machine) {
        CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO crypto_machines (uuid, world, x, y, z, unclaimed_money, fuel_ticks, fuel_quality, speed_level, fuel_time_level, reward_level, last_updated, stored_fuels) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world=?, x=?, y=?, z=?, unclaimed_money=?, fuel_ticks=?, fuel_quality=?, speed_level=?, fuel_time_level=?, reward_level=?, last_updated=?, stored_fuels=?";
                    
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                 
                // Serialize stored fuels
                StringBuilder sb = new StringBuilder();
                for (StoredFuel sf : machine.getStoredFuels()) {
                    if (!sb.isEmpty()) sb.append(";");
                    sb.append(sf.getId().toString()).append(":").append(sf.getQuality());
                }
                String storedStr = sb.toString();

                stmt.setString(1, machine.getOwnerId().toString());
                stmt.setString(2, machine.getWorldName());
                stmt.setObject(3, machine.getX());
                stmt.setObject(4, machine.getY());
                stmt.setObject(5, machine.getZ());
                stmt.setLong(6, machine.getUnclaimedMoney());
                stmt.setLong(7, machine.getFuelTicks());
                stmt.setDouble(8, machine.getFuelQuality());
                stmt.setInt(9, machine.getSpeedLevel());
                stmt.setInt(10, machine.getFuelTimeLevel());
                stmt.setInt(11, machine.getRewardLevel());
                stmt.setLong(12, machine.getLastUpdated());
                stmt.setString(13, storedStr);
                
                stmt.setString(14, machine.getWorldName());
                stmt.setObject(15, machine.getX());
                stmt.setObject(16, machine.getY());
                stmt.setObject(17, machine.getZ());
                stmt.setLong(18, machine.getUnclaimedMoney());
                stmt.setLong(19, machine.getFuelTicks());
                stmt.setDouble(20, machine.getFuelQuality());
                stmt.setInt(21, machine.getSpeedLevel());
                stmt.setInt(22, machine.getFuelTimeLevel());
                stmt.setInt(23, machine.getRewardLevel());
                stmt.setLong(24, machine.getLastUpdated());
                stmt.setString(25, storedStr);

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
