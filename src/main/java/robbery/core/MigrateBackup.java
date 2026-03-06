package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.items.Items;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MigrateBackup implements CommandExecutor {

    private final Robbery plugin;
    private final NamespacedKey ITEM_KEY;

    public MigrateBackup(Robbery plugin) {
        this.plugin = plugin;
        this.ITEM_KEY = new NamespacedKey(plugin, "item_uuid");
    }

    /**
     * Command entry point.
     * Usage: /migrate
     * Runs both:
     *  1) backupitems.yml migration (adds world/x/y/z when dropped entity is loaded)
     *  2) items migration from armor stands (attach position + dropped item if present)
     *
     * Silent: no output to console or player.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Run only when typed as /migrate with no args
        if (args.length == 0) {
            migrateBackups();
            int attached = migrateItemsFromArmorStands();
            sender.sendMessage("§aMigration completed! §7Attached " + attached + " items to existing entities.");
            return true;
        }
        return false;
    }

    /**
     * 1) Migrate backupitems.yml: write coordinates when the dropped Item entity is currently loaded.
     */
    private void migrateBackups() {
        File backupFile = new File(plugin.getDataFolder(), "backupitems.yml");
        if (!backupFile.exists()) return;

        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        ConfigurationSection section = backupConfig.getConfigurationSection("items");
        if (section == null) return;

        boolean changed = false;

        for (String key : section.getKeys(false)) {
            try {
                UUID droppedUUID = UUID.fromString(key);
                Entity ent = Bukkit.getEntity(droppedUUID);
                if (ent instanceof Item droppedEntity) {
                    String basePath = "items." + key + ".";
                    if (backupConfig.getString(basePath + "world") == null
                            || backupConfig.get(basePath + "x") == null
                            || backupConfig.get(basePath + "y") == null
                            || backupConfig.get(basePath + "z") == null) {

                        Location loc = droppedEntity.getLocation();
                        backupConfig.set(basePath + "world", loc.getWorld().getName());
                        backupConfig.set(basePath + "x", loc.getX());
                        backupConfig.set(basePath + "y", loc.getY());
                        backupConfig.set(basePath + "z", loc.getZ());
                        changed = true;
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (changed) {
            try {
                backupConfig.save(backupFile);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 2) Migrate Items list using ArmorStands:
     *    - For each Items without position: try to find its ArmorStand; if found set position and attach existing dropped item (no spawning)
     *    - Save plugin items via plugin.saveItems() if something changed
     */
    public int migrateItemsFromArmorStands() {
        List<Items> items = plugin.getItems();
        if (items == null || items.isEmpty()) return 0;

        int attachedCount = 0;
        boolean changed = false;

        for (Items item : new ArrayList<>(items)) {
            if (item.getPosition() != null) continue;

            UUID uid = item.getUniqueId();
            ArmorStand stand = findArmorStandFor(uid);
            boolean attached = false;

            if (stand != null) {
                Location dropLoc = stand.getLocation().clone().add(0, 1, 0);
                item.setPosition(dropLoc);
                changed = true;

                Item dropped = findDroppedItemNear(dropLoc, uid, 2.0);
                if (dropped != null) {
                    item.setDroppedItem(dropped);
                    attached = true;
                }
            }

            if (!attached) {
                Item ent = findDroppedItemGlobally(uid);
                if (ent != null) {
                    item.setDroppedItem(ent);
                    item.setPosition(ent.getLocation());
                    attached = true;
                    changed = true;
                }
            }

            if (attached) attachedCount++;
        }

        if (changed) {
            try {
                plugin.saveItems();
            } catch (Exception ignored) {
            }
        }

        return attachedCount;
    }

    private ArmorStand findArmorStandFor(UUID id) {
        String idStr = id.toString();
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!(e instanceof ArmorStand)) continue;
                ArmorStand stand = (ArmorStand) e;
                try {
                    if (stand.getPersistentDataContainer().has(ITEM_KEY, PersistentDataType.STRING)) {
                        String stored = stand.getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
                        if (idStr.equals(stored)) return stand;
                    }
                } catch (Exception ignored) {}
                String cn = stand.getCustomName();
                if (cn != null && cn.equals(idStr)) return stand;
            }
        }
        return null;
    }

    private Item findDroppedItemNear(Location loc, UUID id, double radius) {
        if (loc == null) return null;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof Item it) {
                String cn = it.getCustomName();
                if (cn != null && cn.equals(id.toString())) return it;
            }
        }
        return null;
    }

    private Item findDroppedItemGlobally(UUID id) {
        String idStr = id.toString();
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Item it) {
                    String cn = it.getCustomName();
                    if (cn != null && cn.equals(idStr)) return it;
                }
            }
        }
        return null;
    }
}
