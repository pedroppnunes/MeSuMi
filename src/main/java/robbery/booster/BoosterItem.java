package robbery.booster;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for creating a booster item in the Robbery plugin.
 * <p>
 * Provides a method to generate a custom {@link ItemStack} representing a booster.
 * The item uses persistent data to identify it as a booster item.
 * </p>
 */
public class BoosterItem {

    /**
     * Creates a custom booster item as an {@link ItemStack}.
     * <p>
     * The item is a potion with a display name of "§bBoosters" and contains a
     * persistent data marker to identify it as a booster item.
     * </p>
     *
     * @param plugin the instance of the JavaPlugin, used to create a NamespacedKey
     * @return a {@link ItemStack} representing the booster item
     */
    public static ItemStack createBoosterItem(JavaPlugin plugin) {
        ItemStack bottle = new ItemStack(Material.POTION);
        ItemMeta meta = bottle.getItemMeta();
        if (meta == null) return bottle;

        meta.setDisplayName("§bBoosters");
        NamespacedKey key = new NamespacedKey(plugin, "booster_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        bottle.setItemMeta(meta);
        return bottle;
    }

}
