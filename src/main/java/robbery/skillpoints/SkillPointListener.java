package robbery.skillpoints;

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
 * Listener for skill point item interactions.
 * <p>
 * Detects when a player right-clicks a skill point item (SUNFLOWER with
 * the persistent data key "skillpoint_item") and opens the skill point menu for them.
 */
public class SkillPointListener implements Listener {

    private final NamespacedKey key;

    /**
     * Creates a new SkillPointListener with a plugin-specific persistent key.
     *
     * @param plugin the plugin instance used to create the NamespacedKey
     */
    public SkillPointListener(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "skillpoint_item");
    }

    /**
     * Handles player interactions with skill point items.
     * <p>
     * When a player right-clicks a SUNFLOWER with the persistent data key,
     * the skill point menu is opened via the "dm open skillpoint_menu" command.
     *
     * @param event the player interaction event
     */
    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SUNFLOWER) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(key, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();
        String menuName = "skillpoint_menu";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menuName + " " + player.getName());
        event.setCancelled(true);
    }
}
