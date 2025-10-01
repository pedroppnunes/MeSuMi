package robbery.skillpoints;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for creating skill point items.
 * <p>
 * Skill point items are represented by SUNFLOWERs with a persistent data key to identify them.
 * They are used in the plugin to grant skill points to players.
 */
public class SkillPointItem {

    /**
     * Creates a skill point item for use in the plugin.
     * <p>
     * The item will be a SUNFLOWER with the display name "§eSkillpoints" and
     * a persistent data container key "skillpoint_item" set to 1.
     *
     * @param plugin the plugin instance used to create the NamespacedKey
     * @return the ItemStack representing the skill point item
     */
    public static ItemStack createSkillPointItem(JavaPlugin plugin) {
        ItemStack sun = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = sun.getItemMeta();
        if (meta == null) return sun;

        meta.setDisplayName("§eSkillpoints");
        NamespacedKey key = new NamespacedKey(plugin, "skillpoint_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        sun.setItemMeta(meta);
        return sun;
    }
}
