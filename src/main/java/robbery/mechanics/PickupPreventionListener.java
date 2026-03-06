package robbery.mechanics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import robbery.core.Robbery;

/**
 * Listener that prevents players from picking up certain items in the "world" world.
 * <p>
 * Items are blocked based on their display name if they exist in the plugin's items map.
 */
public class PickupPreventionListener implements Listener {

    /**
     * Called when a player attempts to pick up an item.
     * <p>
     * Cancels the pickup if:
     * <ul>
     *     <li>The player is in the "world" world.</li>
     *     <li>The item's display name exists in {@link Robbery#getItemsMap()}.</li>
     * </ul>
     *
     * @param event The item pickup event.
     */
    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {

        // Only apply in the "world" world
        if (!event.getPlayer().getWorld().getName().equals("world")) {
            return;
        }

        // Cancel pickup if item is registered in the plugin's item map
        String itemName = event.getItem().getItemStack().getItemMeta().getDisplayName();
        if (Robbery.getItemsMap().containsKey(itemName)) {
            event.setCancelled(true);
        }
    }
}
