package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CryptoMachine {

    private final UUID ownerId;
    private String worldName;
    private Integer x;
    private Integer y;
    private Integer z;

    private long unclaimedMoney;
    private long fuelTicks;
    private double fuelQuality;
    
    // 3 Upgrade Tracks
    private int speedLevel;
    private int fuelTimeLevel;
    private int rewardLevel;

    // Virtual Fuel Storage
    private final List<StoredFuel> storedFuels = new ArrayList<>();

    private org.bukkit.entity.ArmorStand holoLine1;
    private org.bukkit.entity.ArmorStand holoLine2;

    private long lastUpdated;

    public CryptoMachine(UUID ownerId, String worldName, Integer x, Integer y, Integer z,
                         long unclaimedMoney, long fuelTicks, double fuelQuality,
                         int speedLevel, int fuelTimeLevel, int rewardLevel, long lastUpdated) {
        this.ownerId = ownerId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.unclaimedMoney = unclaimedMoney;
        this.fuelTicks = fuelTicks;
        this.fuelQuality = fuelQuality;
        this.speedLevel = speedLevel;
        this.fuelTimeLevel = fuelTimeLevel;
        this.rewardLevel = rewardLevel;
        this.lastUpdated = lastUpdated;
    }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public Integer getX() { return x; }
    public Integer getY() { return y; }
    public Integer getZ() { return z; }

    public Location getLocation() {
        if (worldName == null || x == null || y == null || z == null) return null;
        if (Bukkit.getWorld(worldName) == null) return null;
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public void setLocation(Location loc) {
        if (loc == null) {
            this.worldName = null;
            this.x = null;
            this.y = null;
            this.z = null;
        } else {
            this.worldName = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
        }
    }

    public boolean isPlaced() {
        return worldName != null;
    }

    public long getUnclaimedMoney() {
        return unclaimedMoney;
    }

    public void setUnclaimedMoney(long unclaimedMoney) {
        this.unclaimedMoney = unclaimedMoney;
    }
    
    public void addUnclaimedMoney(long amount) {
        this.unclaimedMoney += amount;
    }

    public long getFuelTicks() {
        return fuelTicks;
    }

    public void setFuelTicks(long fuelTicks) {
        this.fuelTicks = Math.max(0, fuelTicks);
    }

    public double getFuelQuality() {
        return fuelQuality;
    }

    public void setFuelQuality(double fuelQuality) {
        this.fuelQuality = fuelQuality;
    }

    // Upgrades
    public int getSpeedLevel() { return speedLevel; }
    public void setSpeedLevel(int speedLevel) { this.speedLevel = speedLevel; }

    public int getFuelTimeLevel() { return fuelTimeLevel; }
    public void setFuelTimeLevel(int fuelTimeLevel) { this.fuelTimeLevel = fuelTimeLevel; }

    public int getRewardLevel() { return rewardLevel; }
    public void setRewardLevel(int rewardLevel) { this.rewardLevel = rewardLevel; }

    // Stored Fuels
    public List<StoredFuel> getStoredFuels() {
        return storedFuels;
    }

    public void addStoredFuel(StoredFuel fuel) {
        if (fuel != null) {
            storedFuels.add(fuel);
        }
    }

    public boolean removeStoredFuel(UUID fuelId) {
        return storedFuels.removeIf(f -> f.getId().equals(fuelId));
    }

    // Duration mapping based on fuelTimeLevel / batteryTimeLevel (Levels 0-39)
    public long getFuelDurationTicks() {
        return getFuelDurationTicksForLevel(this.fuelTimeLevel);
    }

    public long getBatteryDurationTicks() {
        return getFuelDurationTicks();
    }

    public static long getFuelDurationTicksForLevel(int level) {
        if (level <= 0) return 600L; // 10 min
        if (level >= 39) return 86400L; // 24 Hours max level

        // Tier 0 (1 to 9): 20m up to 2.5h
        if (level < 10) {
            return switch (level) {
                case 1 -> 1200L;   // 20 min
                case 2 -> 1800L;   // 30 min
                case 3 -> 2700L;   // 45 min
                case 4 -> 3600L;   // 1 hour
                case 5 -> 4500L;   // 1h 15m
                case 6 -> 5400L;   // 1h 30m
                case 7 -> 6300L;   // 1h 45m
                case 8 -> 7200L;   // 2 hours
                case 9 -> 9000L;   // 2.5 hours
                default -> 1200L;
            };
        }

        // Tier 1 (10 to 19): 3h up to 10h
        if (level < 20) {
            if (level <= 14) {
                return 10800L + (long) (level - 10) * 1800L; // 3h to 5h
            }
            return 21600L + (long) (level - 15) * 3600L; // 6h to 10h
        }

        // Tier 2 (20 to 29): 11h up to 20h
        if (level < 30) {
            return 39600L + (long) (level - 20) * 3600L; // 11h to 20h
        }

        // Tier 3 (30 to 39): 20.5h up to 24h (Max level 39 = 24h)
        if (level < 35) {
            return 73800L + (long) (level - 30) * 1800L; // 20.5h to 22.5h
        }
        return Math.min(86400L, 82800L + (long) (level - 35) * 900L); // 23h to 24h
    }

    public static String getFuelDurationFormattedForTicks(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours == 0) {
            return minutes + " Minutes";
        } else if (minutes == 0) {
            if (hours >= 24 && hours % 24 == 0) {
                long days = hours / 24;
                return days + (days == 1 ? " Day" : " Days");
            }
            return hours + (hours == 1 ? " Hour" : " Hours");
        } else {
            return hours + "h " + minutes + "m";
        }
    }

    public static String getFuelDurationFormattedForLevel(int level) {
        return getFuelDurationFormattedForTicks(getFuelDurationTicksForLevel(level));
    }

    public static String getBatteryDurationFormattedForLevel(int level) {
        return getFuelDurationFormattedForLevel(level);
    }

    public int getBatteryTimeLevel() {
        return fuelTimeLevel;
    }

    public void setBatteryTimeLevel(int batteryTimeLevel) {
        this.fuelTimeLevel = batteryTimeLevel;
    }

    public double getRewardMultiplier() {
        // Each reward level adds 2% money multiplier (Level 0 = 1.0x, Level 20 = 1.4x, Level 39 = 1.78x)
        return 1.0 + (rewardLevel * 0.02);
    }
    
    public void updateHologram() {
        if (!isPlaced()) {
            removeHologram();
            return;
        }
        
        Location loc = getLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        // Prevent loading chunks just to update the hologram
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        
        Location centerLoc = loc.clone().add(0.5, 1.25, 0.5);
        
        // Aggressively clean up ANY ghost / duplicate armor stands in the area
        for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(centerLoc, 2.0, 3.0, 2.0)) {
            if (entity instanceof org.bukkit.entity.ArmorStand as) {
                if (as == holoLine1 || as == holoLine2) continue;
                
                boolean isGhost = false;
                if (as.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.BYTE)
                        || as.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.STRING)) {
                    isGhost = true;
                } else if (as.getCustomName() != null) {
                    String cleanName = org.bukkit.ChatColor.stripColor(as.getCustomName());
                    if (cleanName.startsWith("Owner:") || cleanName.startsWith("Current Money:")) {
                        isGhost = true;
                    }
                }
                
                if (isGhost) {
                    as.remove();
                }
            }
        }

        if (holoLine1 == null || !holoLine1.isValid() || holoLine1.isDead()) {
            Location line1Loc = loc.clone().add(0.5, 1.35, 0.5);
            holoLine1 = loc.getWorld().spawn(line1Loc, org.bukkit.entity.ArmorStand.class, as -> {
                as.setPersistent(false);
                as.setInvisible(true);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setRemoveWhenFarAway(true);
                as.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            });
        }
        
        if (holoLine2 == null || !holoLine2.isValid() || holoLine2.isDead()) {
            Location line2Loc = loc.clone().add(0.5, 1.1, 0.5);
            holoLine2 = loc.getWorld().spawn(line2Loc, org.bukkit.entity.ArmorStand.class, as -> {
                as.setPersistent(false);
                as.setInvisible(true);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setRemoveWhenFarAway(true);
                as.getPersistentDataContainer().set(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            });
        }
        
        String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        if (ownerName == null) ownerName = "Unknown";
        
        String moneyStr = robbery.number.NumberFormatter.formatDoubleNumber((double) unclaimedMoney);
        
        if (holoLine1 != null && holoLine1.isValid()) {
            holoLine1.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Owner: &6" + ownerName));
        }
        if (holoLine2 != null && holoLine2.isValid()) {
            holoLine2.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&fCurrent Money: &a$" + moneyStr));
        }
    }
    
    public void removeHologram() {
        if (holoLine1 != null) {
            holoLine1.remove();
            holoLine1 = null;
        }
        if (holoLine2 != null) {
            holoLine2.remove();
            holoLine2 = null;
        }
        
        Location loc = getLocation();
        if (loc != null && loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            Location centerLoc = loc.clone().add(0.5, 1.25, 0.5);
            for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(centerLoc, 2.5, 3.5, 2.5)) {
                if (entity instanceof org.bukkit.entity.ArmorStand as) {
                    boolean isGhost = false;
                    if (as.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.BYTE)
                            || as.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("robbery", "crypto_holo"), org.bukkit.persistence.PersistentDataType.STRING)) {
                        isGhost = true;
                    } else if (as.getCustomName() != null) {
                        String cleanName = org.bukkit.ChatColor.stripColor(as.getCustomName());
                        if (cleanName.startsWith("Owner:") || cleanName.startsWith("Current Money:")) {
                            isGhost = true;
                        }
                    }
                    
                    if (isGhost) {
                        as.remove();
                    }
                }
            }
        }
    }

    public double getSpeedMultiplier() {
        // Each speed level adds 2% speed multiplier (Level 0 = 1.0x, Level 20 = 1.4x, Level 39 = 1.78x)
        return 1.0 + (speedLevel * 0.02);
    }

    public double getQualityMultiplier() {
        // Quality ranges: 1% (0.5x), 50% (1.0x), 100% (1.6x)
        if (fuelQuality <= 1.0) {
            return 0.50;
        } else if (fuelQuality <= 50.0) {
            return 0.50 + (0.50 * ((fuelQuality - 1.0) / 49.0));
        } else {
            return 1.00 + (0.60 * ((fuelQuality - 50.0) / 50.0));
        }
    }
}
