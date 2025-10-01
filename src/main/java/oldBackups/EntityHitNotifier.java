/*package robbery.events;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class EntityHitNotifier implements Listener {
    @EventHandler
    public void onEntityHit(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!(entity instanceof ArmorStand)) {
            return;
        }

        String entityName = entity.getName();
        String entityType = entity.getType().toString();

        player.sendMessage("§eYou are hitting: " + entityName + " (§6" + entityType +  "§e)");
    }
}
*/