/*
package robbery.keys;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import robbery.Robbery;
import robbery.player.PlayerDataManager;

public class DoorVisibilityListener implements Listener {

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent e) {
        Player player = e.getPlayer();
        updateVisibleDoorsForChunk(player, e.getChunk());
    }

    private void updateVisibleDoorsForChunk(Player player, Chunk chunk) {
        for (Keys key : KeyManager.getAllKeys()) {
            boolean owns = PlayerDataManager.getPlayerData(player).hasKey(key.getId());

            for (DoorArea area : key.getDoorAreas()) {
                if (area.isInChunk(chunk)) {
                    if (owns) area.setMaterial(player, Material.AIR);
                    else area.setMaterial(player, Material.IRON_BARS);
                }
            }
        }
    }
}
 */
