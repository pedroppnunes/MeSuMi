/*package robbery.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import robbery.Robbery;

public class PickupPreventionListener implements Listener {

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {

        if (!event.getPlayer().getWorld().getName().equals("world")) {
            return;
        }

        String itemName = event.getItem().getItemStack().getItemMeta().getDisplayName();
        if (Robbery.getItemsMap().containsKey(itemName)) {
            event.setCancelled(true);
        }
    }
}
 */
