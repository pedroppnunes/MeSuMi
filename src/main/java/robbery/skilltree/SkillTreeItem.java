package robbery.skilltree;

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

public class SkillTreeItem implements Listener {

    private final NamespacedKey key;

    public SkillTreeItem(Robbery plugin){
        this.key = new NamespacedKey(plugin, "skilltree_item");
    }

    public static ItemStack createSkillTreeItem(Robbery plugin) {
        ItemStack sun = new ItemStack(Material.BOOK);
        ItemMeta meta = sun.getItemMeta();
        if (meta == null) return sun;

        meta.setDisplayName("§aSkill Tree");
        NamespacedKey key = new NamespacedKey(plugin, "skilltree_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        sun.setItemMeta(meta);
        return sun;
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BOOK) return;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(key, PersistentDataType.BYTE)) return;

        Player player = event.getPlayer();
        String menuName = "skilltree_menu1";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open " + menuName + " " + player.getName());
        event.setCancelled(true);
    }
}
