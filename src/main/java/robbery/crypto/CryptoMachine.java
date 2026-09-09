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

    private double unclaimedMoney;
    private long fuelTicks;
    private double fuelQuality;
    
    // 4 Upgrade Tracks
    private int speedLevel;
    private int capacityLevel;
    private int fuelTimeLevel;
    private int rewardLevel;
    private int rewardTier;

    // Virtual Fuel Storage
    private final List<StoredFuel> storedFuels = new ArrayList<>();

    private org.bukkit.entity.ArmorStand holoLine1;
    private org.bukkit.entity.ArmorStand holoLine2;

    private long lastUpdated;

    public CryptoMachine(UUID ownerId, String worldName, Integer x, Integer y, Integer z,
                         double unclaimedMoney, long fuelTicks, double fuelQuality,
                         int speedLevel, int fuelTimeLevel, int rewardLevel, int capacityLevel, long lastUpdated) {
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
        this.capacityLevel = capacityLevel;
        this.lastUpdated = lastUpdated;
    }

    public CryptoMachine(UUID ownerId, String worldName, Integer x, Integer y, Integer z,
                         double unclaimedMoney, long fuelTicks, double fuelQuality,
                         int speedLevel, int fuelTimeLevel, int rewardLevel, long lastUpdated) {
        this(ownerId, worldName, x, y, z, unclaimedMoney, fuelTicks, fuelQuality, speedLevel, fuelTimeLevel, rewardLevel, 0, lastUpdated);
    }

    public CryptoMachine(UUID ownerId, String worldName, Integer x, Integer y, Integer z,
                         long unclaimedMoney, long fuelTicks, double fuelQuality,
                         int speedLevel, int fuelTimeLevel, int rewardLevel, long lastUpdated) {
        this(ownerId, worldName, x, y, z, (double) unclaimedMoney, fuelTicks, fuelQuality, speedLevel, fuelTimeLevel, rewardLevel, 0, lastUpdated);
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
        return (long) unclaimedMoney;
    }

    public double getUnclaimedMoneyDouble() {
        return unclaimedMoney;
    }

    public void setUnclaimedMoney(long unclaimedMoney) {
        this.unclaimedMoney = (double) unclaimedMoney;
    }

    public void setUnclaimedMoney(double unclaimedMoney) {
        this.unclaimedMoney = unclaimedMoney;
    }
    
    public void addUnclaimedMoney(long amount) {
        this.unclaimedMoney += amount;
    }

    public void addUnclaimedMoney(double amount) {
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

    public int getCapacityLevel() { return capacityLevel; }
    public void setCapacityLevel(int capacityLevel) { this.capacityLevel = capacityLevel; }

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

    // Duration mapping based on fuelTimeLevel / batteryTimeLevel (Levels 0-10)
    public long getFuelDurationTicks() {
        return getFuelDurationTicksForLevel(this.fuelTimeLevel);
    }

    public long getBatteryDurationTicks() {
        return getFuelDurationTicks();
    }

    public static long getFuelDurationTicksForLevel(int level) {
        if (level <= 0) return 600L; // 10 min
        return switch (level) {
            case 1 -> 1800L;   // 30 min
            case 2 -> 3600L;   // 1 hour
            case 3 -> 7200L;   // 2 hours
            case 4 -> 14400L;  // 4 hours
            case 5 -> 21600L;  // 6 hours
            case 6 -> 32400L;  // 9 hours
            case 7 -> 43200L;  // 12 hours
            case 8 -> 57600L;  // 16 hours
            case 9 -> 72000L;  // 20 hours
            default -> 86400L; // 24 hours (Level 10+)
        };
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

    public int getMachineLevel() {
        return Math.max(0, speedLevel);
    }

    public int getCapacity() {
        if (capacityLevel <= 0) return 1;
        return Math.min(10, capacityLevel + 1);
    }

    public int getStealIntervalSeconds() {
        if (speedLevel <= 0) return 600;
        return Math.max(60, 600 - (Math.min(9, speedLevel) * 60));
    }

    public double getRewardMultiplier() {
        if (rewardLevel <= 0) return 1.0;
        if (rewardLevel >= 10) return 3.0;
        return 1.0 + (rewardLevel * (2.0 / 9.0));
    }

    public double getSpeedMultiplier() {
        return 1.0;
    }

    public double getQualityMultiplier() {
        // Base/default battery starts at 1.0x so early game isn't heavily penalized
        if (fuelQuality <= 1.0) {
            return 1.00;
        } else if (fuelQuality <= 50.0) {
            return 1.00 + (0.25 * ((fuelQuality - 1.0) / 49.0));
        } else {
            return 1.25 + (0.35 * ((fuelQuality - 50.0) / 50.0));
        }
    }

    public static double getAverageTop5ItemValue(robbery.player.PlayerData pd) {
        if (pd == null || pd.getKey() == null) return 15.80;
        String storeId = pd.getKey().getName();
        int targetStoreNum = extractStoreNumStatic(storeId);

        java.util.List<robbery.items.Items> storeItems = new java.util.ArrayList<>();
        if (robbery.core.Robbery.getItemsMap() != null) {
            for (java.util.Map.Entry<String, robbery.items.Items> entry : robbery.core.Robbery.getItemsMap().entrySet()) {
                String itemId = entry.getKey();
                robbery.items.Items itemObj = entry.getValue();
                if (itemId == null || itemObj == null) continue;
                int itemStoreNum = extractStoreNumStatic(itemId);
                if (itemStoreNum == targetStoreNum || (targetStoreNum == 12 && itemStoreNum == 13) || (targetStoreNum == 13 && itemStoreNum == 12)) {
                    storeItems.add(itemObj);
                }
            }
        }

        if (storeItems.isEmpty()) return 15.80;

        storeItems.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int count = Math.min(5, storeItems.size());
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += storeItems.get(i).getValue();
        }
        return sum / count;
    }

    public static int extractStoreNumStatic(String id) {
        if (id == null) return 1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(id);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    public static double getStoreEfficiencyMultiplier(int storeOrder) {
        if (storeOrder <= 3) return 1.00;
        if (storeOrder <= 6) return 0.50;
        if (storeOrder <= 9) return 0.15;
        return 0.025;
    }
}
