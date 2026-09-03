package robbery.storeMastery;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StorePlaytimeTask extends BukkitRunnable {

    private final Robbery plugin;
    private final Map<String, Location> storeLocations = new HashMap<>();

    public StorePlaytimeTask(Robbery plugin) {
        this.plugin = plugin;
        initStoreLocations();
    }

    private void initStoreLocations() {
        World world = Bukkit.getWorld("world");
        if (world == null) return;
        storeLocations.put("store1",  new Location(world, 20124, 101, 20037));
        storeLocations.put("store2",  new Location(world, 20129, 102, 19984));
        storeLocations.put("store3",  new Location(world, 20175, 101, 20038));
        storeLocations.put("store4",  new Location(world, 20205, 101, 19989));
        storeLocations.put("store5",  new Location(world, 20259, 100, 20098));
        storeLocations.put("store6",  new Location(world, 20320, 101, 20021));
        storeLocations.put("store7",  new Location(world, 20328, 101, 20133));
        storeLocations.put("store8",  new Location(world, 20230, 101, 20201));
        storeLocations.put("store9",  new Location(world, 20188, 101, 20159));
        storeLocations.put("store10", new Location(world, 20180, 101, 20204));
        storeLocations.put("store11", new Location(world, 20130, 101, 20152));
        storeLocations.put("store12", new Location(world, 20090, 101, 20231));
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;
            PlayerData pd = PlayerDataManager.getPlayerData(player);
            if (pd == null) continue;

            String storeId = detectStore(player);
            if (storeId != null) {
                pd.addStorePlaytime(storeId, 1L);
            }
        }
    }

    /**
     * Detects which store the player is currently inside.
     * Uses WorldGuard regions if available, falling back to proximity checking.
     */
    public String detectStore(Player player) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().equalsIgnoreCase("world")) {
            return null;
        }

        // 1. Try WorldGuard API via Reflection
        String wgStore = getStoreFromWorldGuard(loc);
        if (wgStore != null) {
            return wgStore;
        }

        // 2. Fallback: Proximity to known store center locations (within 45 blocks)
        String closestStore = null;
        double minDistanceSq = 45.0 * 45.0;

        for (Map.Entry<String, Location> entry : storeLocations.entrySet()) {
            Location sLoc = entry.getValue();
            if (sLoc.getWorld() != null && sLoc.getWorld().equals(loc.getWorld())) {
                double distSq = sLoc.distanceSquared(loc);
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    closestStore = entry.getKey();
                }
            }
        }

        return closestStore;
    }

    private String getStoreFromWorldGuard(Location loc) {
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
            Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> adaptClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object wgWorld = adaptClass.getMethod("adapt", World.class).invoke(null, loc.getWorld());
            Object wgVector = adaptClass.getMethod("asBlockVector", Location.class).invoke(null, loc);

            Object query = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);
            Object regionSet = query.getClass().getMethod("getApplicableRegions",
                    Class.forName("com.sk89q.worldedit.world.World"),
                    Class.forName("com.sk89q.worldedit.math.BlockVector3"))
                    .invoke(query, wgWorld, wgVector);

            Set<?> regions = (Set<?>) regionSet.getClass().getMethod("getRegions").invoke(regionSet);
            for (Object reg : regions) {
                String regId = (String) reg.getClass().getMethod("getId").invoke(reg);
                if (regId == null) continue;
                String lower = regId.toLowerCase();
                for (int i = 12; i >= 1; i--) {
                    if (lower.startsWith("store" + i)) {
                        return "store" + i;
                    }
                }
            }
        } catch (Throwable ignored) {
            // WorldGuard not installed or API mismatch, ignore silently
        }
        return null;
    }
}
