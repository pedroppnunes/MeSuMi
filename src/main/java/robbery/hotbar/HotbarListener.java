package robbery.hotbar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;

public class HotbarListener implements Listener {

    private final NamespacedKey key;

    public HotbarListener(Robbery plugin){
        this.key = new NamespacedKey(plugin, "mainmenu_item");
    }

    public static ItemStack createMainMenuItem(Robbery plugin) {
        ItemStack sun = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = sun.getItemMeta();
        if (meta == null) return sun;

        meta.setDisplayName("§bMain Menu");
        NamespacedKey key = new NamespacedKey(plugin, "mainmenu_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        sun.setItemMeta(meta);
        return sun;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (robbery.storeMastery.PlayerStatsGUI.isRecentPlayerClick(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.NETHER_STAR) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(key, PersistentDataType.BYTE)) return;

        String menuName = "general_menu";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menuName + " " + player.getName());
        event.setCancelled(true);
    }
}
