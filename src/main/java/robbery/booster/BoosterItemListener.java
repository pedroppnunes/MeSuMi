package robbery.booster;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener for handling player interactions with booster items.
 * <p>
 * Detects when a player right-clicks a booster potion and opens the
 * corresponding booster menu using a console command.
 * </p>
 */
public class BoosterItemListener implements Listener {

    /** NamespacedKey used to identify booster items in persistent data. */
    private final NamespacedKey key;

    /**
     * Constructs a new listener for booster items.
     *
     * @param plugin the plugin instance, used to create a NamespacedKey
     */
    public BoosterItemListener(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "booster_item");
    }

    /**
     * Handles player interaction events.
     * <p>
     * If a player right-clicks an ItemStack of type POTION containing the booster
     * persistent data, this method opens the booster menu via a console command.
     * The event is then cancelled to prevent normal interaction behavior.
     * </p>
     *
     * @param event the PlayerInteractEvent triggered when a player interacts
     */
    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(key, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();
        String menuName = "booster_menu";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menuName + " " + player.getName());

        event.setCancelled(true);
    }
}
