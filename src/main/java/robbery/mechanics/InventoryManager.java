package robbery.mechanics;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryManager {

    private static final String WORLD_NAME = "world";

    public static void giveItem(Player player, ItemStack item, int slot) {
        if (player == null || item == null || !player.getWorld().getName().equals(WORLD_NAME)) return;

        player.getInventory().setItem(slot, item);
        player.updateInventory();
    }

    public static void giveItemAnywhere(Player player, ItemStack item) {
        if (player == null || item == null) return;

        player.getInventory().addItem(item);
        player.updateInventory();
    }

    public static void replaceChestplate(Player player, ItemStack item) {
        if (player == null || item == null || !player.getWorld().getName().equals(WORLD_NAME)) return;

        player.getInventory().setChestplate(item);
        player.updateInventory();
    }

    public static void updateHideDye(Player player, boolean hidden) {
        if (player == null || !player.getWorld().getName().equals(WORLD_NAME)) return;

        ItemStack dye = new ItemStack(hidden ? Material.RED_DYE : Material.LIME_DYE);
        ItemMeta meta = dye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hidden ? "§cUnhide Players" : "§aHide Players");
            dye.setItemMeta(meta);
        }
        player.getInventory().setItem(7, dye);
        player.updateInventory();
    }
}
