package robbery.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import robbery.messages.Messages;

import java.util.Iterator;
import java.util.Set;

/**
 * Listens for crafting and usage events of blocked items in the game world.
 * <p>
 * This class prevents players from crafting or using certain restricted items unless
 * they have the bypass permission. It also provides a utility to remove blocked recipes
 * from the server entirely.
 * </p>
 */
public class BlockCraftListener implements Listener {

    /** Permission that allows bypassing blocked item restrictions. */
    private static final String BYPASS_PERMISSION = "robbery.bypass";

    /** Set of blocked materials that players are not allowed to craft or use. */
    private final Set<Material> blockedItems = Set.of(
            Material.MINECART,
            Material.TNT,
            Material.REDSTONE,
            Material.REDSTONE_BLOCK,
            Material.HOPPER,
            Material.DROPPER,
            Material.DISPENSER,
            Material.OBSERVER,
            Material.COMPARATOR,
            Material.REPEATER,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.OAK_BOAT,
            Material.SPRUCE_BOAT,
            Material.BIRCH_BOAT,
            Material.JUNGLE_BOAT,
            Material.ACACIA_BOAT,
            Material.DARK_OAK_BOAT,
            Material.HOPPER_MINECART,
            Material.TNT_MINECART,
            Material.CHEST_MINECART,
            Material.FURNACE_MINECART,
            Material.END_CRYSTAL,
            Material.SCAFFOLDING,
            Material.ARMOR_STAND,
            Material.NOTE_BLOCK,
            Material.DAYLIGHT_DETECTOR,
            Material.REDSTONE_TORCH,
            Material.LEVER,
            Material.TRIPWIRE_HOOK,
            Material.OAK_CHEST_BOAT,
            Material.SPRUCE_CHEST_BOAT,
            Material.BIRCH_CHEST_BOAT,
            Material.JUNGLE_CHEST_BOAT,
            Material.ACACIA_CHEST_BOAT,
            Material.DARK_OAK_CHEST_BOAT,
            Material.MANGROVE_CHEST_BOAT,
            Material.BAMBOO_CHEST_RAFT,
            Material.OBSIDIAN,
            Material.FIREWORK_STAR,
            Material.CARROT_ON_A_STICK
    );

    /** Constructs a new BlockCraftListener. */
    public BlockCraftListener() {
    }

    /**
     * Checks if a material is blocked.
     *
     * @param mat The material to check.
     * @return True if the material is blocked, false otherwise.
     */
    private boolean isBlockedMaterial(Material mat) {
        return blockedItems.contains(mat);
    }

    /**
     * Cancels crafting of blocked items.
     * <p>
     * If the crafted item is blocked and the player does not have bypass permission,
     * the event is cancelled and a message is sent to the player.
     * </p>
     *
     * @param event The CraftItemEvent.
     */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Material result = event.getRecipe().getResult().getType();
        if (isBlockedMaterial(result)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && !player.hasPermission(BYPASS_PERMISSION)) {
                Messages.send(player, "events.craft-blocked");
            }
        }
    }

    /**
     * Cancels usage of blocked items in the world.
     * <p>
     * If the item in hand is blocked and the player does not have bypass permission,
     * the interaction is cancelled. A message is sent if the player is in the "world".
     * </p>
     *
     * @param event The PlayerInteractEvent.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (item != null && isBlockedMaterial(item.getType()) && !player.hasPermission(BYPASS_PERMISSION)) {
            event.setCancelled(true);
            if (!player.getWorld().getName().equalsIgnoreCase("world"))
                Messages.send(player, "events.use-blocked");
        }
    }

    /**
     * Removes all blocked recipes from the server.
     * <p>
     * Iterates through all registered recipes and removes any whose result matches a blocked item.
     * </p>
     */
    public void removeRecipes() {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe != null) {
                ItemStack result = recipe.getResult();
                if (result != null && blockedItems.contains(result.getType())) {
                    it.remove();
                }
            }
        }
    }
}
