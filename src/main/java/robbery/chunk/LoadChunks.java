package robbery.chunk;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import robbery.Robbery;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadChunks {

    private final Robbery plugin;
    private static final Set<String> CHUNK_COORDS = new HashSet<>();

    public LoadChunks(Robbery plugin) {
        this.plugin = plugin;
        loadChunkFile();
    }

    private void loadChunkFile() {
        File file = new File(plugin.getDataFolder(), "chunks.yml");
        if (!file.exists()) {
            plugin.saveResource("chunks.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains("chunks")) {
            CHUNK_COORDS.addAll(config.getStringList("chunks"));
            plugin.getLogger().info("Loaded " + CHUNK_COORDS.size() + " chunk coordinates from chunks.yml");
        } else {
            plugin.getLogger().warning("No chunks found in chunks.yml!");
        }
    }

    /**
     * Loads all chunks defined in CHUNK_COORDS asynchronously.
     * Once all are loaded, it runs the given callback on the main thread.
     */
    public void loadAllChunks(Runnable onComplete) {
        if (CHUNK_COORDS.isEmpty()) {
            plugin.getLogger().info("No chunks defined in LoadChunks.");
            Bukkit.getScheduler().runTask(plugin, onComplete);
            return;
        }

        plugin.getLogger().info("Loading " + CHUNK_COORDS.size() + " chunks...");

        AtomicInteger remaining = new AtomicInteger(CHUNK_COORDS.size());

        for (String entry : CHUNK_COORDS) {
            String[] parts = entry.split(":");
            if (parts.length != 3) {
                plugin.getLogger().warning("Invalid chunk format: " + entry);
                if (remaining.decrementAndGet() == 0) Bukkit.getScheduler().runTask(plugin, onComplete);
                continue;
            }

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                plugin.getLogger().warning("World not found for chunk: " + entry);
                if (remaining.decrementAndGet() == 0) Bukkit.getScheduler().runTask(plugin, onComplete);
                continue;
            }

            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            world.getChunkAtAsync(x, z, true, chunk -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    chunk.setForceLoaded(true);
                    if (remaining.decrementAndGet() == 0) {
                        plugin.getLogger().info("All defined chunks have been loaded.");
                        onComplete.run();
                    }
                });
            });
        }
    }

    // Inside LoadChunks class
    public void loadSpecificChunks(Set<String> chunkCoords, Runnable onComplete) {
        if (chunkCoords == null || chunkCoords.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, onComplete);
            return;
        }

        plugin.getLogger().info("Loading " + chunkCoords.size() + " chunks...");

        AtomicInteger remaining = new AtomicInteger(chunkCoords.size());

        for (String entry : chunkCoords) {
            String[] parts = entry.split(":");
            if (parts.length != 3) {
                plugin.getLogger().warning("Invalid chunk format: " + entry);
                if (remaining.decrementAndGet() == 0) Bukkit.getScheduler().runTask(plugin, onComplete);
                continue;
            }

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                plugin.getLogger().warning("World not found for chunk: " + entry);
                if (remaining.decrementAndGet() == 0) Bukkit.getScheduler().runTask(plugin, onComplete);
                continue;
            }

            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            world.getChunkAtAsync(x, z, true, chunk -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    chunk.setForceLoaded(true);
                    if (remaining.decrementAndGet() == 0) {
                        plugin.getLogger().info("All required chunks have been loaded.");
                        onComplete.run();
                    }
                });
            });
        }
    }

}
