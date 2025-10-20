package robbery.keys;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import robbery.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

public class DoorVisibilityListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(Robbery.getInstance(), () -> updateVisibleDoors(p), 20L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        for (Player p : e.getWorld().getPlayers()) {
            if (p.getWorld() == e.getWorld() && p.getLocation().getChunk().equals(chunk)) {
                updateVisibleDoors(p);
            }
        }
    }

    private void updateVisibleDoors(Player player) {
        PlayerData data = PlayerDataManager.getPlayerData(player);
        if (data == null) return;

        for (Keys key : KeyManager.getAllKeys()) {
            boolean owns = data.hasKey(key.getId());
            if (owns) {
                key.hideIronBars(player);
            } else {
                key.showIronBars(player);
            }
        }
    }
}
