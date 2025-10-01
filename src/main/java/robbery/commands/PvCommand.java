package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles the /pv command for opening and managing player private chests.
 * <p>
 * Each player can have multiple private chests (slots), each saved separately.
 * Players can only access these chests in allowed worlds ("SuperiorWorld" or "outpost").
 * </p>
 *
 * <p>The class handles:
 * <ul>
 *     <li>Opening a private chest inventory.</li>
 *     <li>Creating new inventories if they don't exist.</li>
 *     <li>Saving and loading inventories from disk in YAML format.</li>
 * </ul>
 * </p>
 */
public class PvCommand implements CommandExecutor {

    /** Maps player UUIDs to their private chest slots and inventories. */
    private static final Map<UUID, Map<Integer, Inventory>> data = new HashMap<>();
    private static final Robbery main = Robbery.getInstance();

    /**
     * Executes the /pv command.
     *
     * @param s the sender of the command
     * @param c the command object
     * @param l the command label
     * @param args command arguments (optional slot number)
     * @return true if command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, String @NotNull [] args) {
        if (!(s instanceof Player p)) return false;

        PlayerData pd = PlayerDataManager.getPlayerData(p);

        // Restrict to allowed worlds
        if (!(p.getWorld().getName().equals("SuperiorWorld") || p.getWorld().getName().equals("outpost"))) {
            Messages.send(p, "command.pv.no-permission-world");
            return true;
        }

        // Determine which slot the player wants
        int slot = 1;
        if (args.length > 0) {
            try {
                slot = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                Messages.send(p, "command.pv.invalid-usage");
                return true;
            }
        }

        int totalslots = pd.getExtraSlots() - pd.getSPShop().extraSlots();
        if (Objects.equals(pd.getRank(), "rank0")) totalslots = 2;

        if (slot < 1 || slot > totalslots) {
            Messages.sendFormatted(p, "command.pv.invalid-slot", "max", String.valueOf(totalslots));
            return true;
        }

        Inventory inv = getOrCreate(p.getUniqueId(), slot);
        p.openInventory(inv);
        return true;
    }

    /**
     * Retrieves an existing inventory or creates a new one if it does not exist.
     *
     * @param playerId the UUID of the player
     * @param slot the private chest slot number
     * @return the Inventory instance for the slot
     */
    private Inventory getOrCreate(UUID playerId, int slot) {
        data.putIfAbsent(playerId, new HashMap<>());
        Map<Integer, Inventory> map = data.get(playerId);

        return map.computeIfAbsent(slot, i -> loadInventory(playerId, slot));
    }

    /**
     * Saves a specific inventory to disk as a YAML file.
     *
     * @param uuid the player's UUID
     * @param slot the private chest slot
     * @param inventory the inventory to save
     */
    public static void saveInventory(UUID uuid, int slot, Inventory inventory) {
        File playerFolder = new File(main.getDataFolder(), "player/" + uuid + "/pv");
        if (!playerFolder.exists()) playerFolder.mkdirs();

        File file = new File(playerFolder, "slot_" + slot + ".yml");
        FileConfiguration cfg = new YamlConfiguration();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                cfg.set("contents." + i, item);
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a private chest inventory from disk.
     *
     * @param uuid the player's UUID
     * @param slot the private chest slot
     * @return the loaded Inventory, or an empty inventory if none exists
     */
    public Inventory loadInventory(UUID uuid, int slot) {
        File file = new File(main.getDataFolder(), "player/" + uuid + "/pv/slot_" + slot + ".yml");
        Inventory inventory = Bukkit.createInventory(null, 54, "§6Private Chest #" + slot);

        if (!file.exists()) return inventory;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (cfg.contains("contents")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("contents")).getKeys(false)) {
                int index = Integer.parseInt(key);
                ItemStack item = cfg.getItemStack("contents." + key);
                inventory.setItem(index, item);
            }
        }

        return inventory;
    }

    /**
     * Saves all of a player's private chest inventories.
     *
     * @param uuid the player's UUID
     */
    public static void saveAllInventories(UUID uuid) {
        Map<Integer, Inventory> invs = data.get(uuid);
        if (invs == null) return;

        for (Map.Entry<Integer, Inventory> entry : invs.entrySet()) {
            saveInventory(uuid, entry.getKey(), entry.getValue());
        }
    }
}
