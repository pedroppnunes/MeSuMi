package robbery.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Prevents players from interacting with their inventory in specific worlds unless they have a bypass permission.
 * <p>
 * Restrictions:
 * <ul>
 *     <li>Inventory clicks</li>
 *     <li>Dropping items</li>
 *     <li>Picking up items</li>
 *     <li>Swapping items between main hand and off-hand</li>
 * </ul>
 * Players with the permission {@code robbery.bypass} are exempt.
 * Only applies in the world named "world".
 * </p>
 */
public class InventoryLockListener implements Listener {

    private static final String BYPASS_PERMISSION = "robbery.bypass";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission(BYPASS_PERMISSION) && player.getWorld().getName().equals("world")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(BYPASS_PERMISSION) && player.getWorld().getName().equals("world")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(BYPASS_PERMISSION) && player.getWorld().getName().equals("world")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOffhandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(BYPASS_PERMISSION) && player.getWorld().getName().equals("world")) {
            event.setCancelled(true);
        }
    }
}
