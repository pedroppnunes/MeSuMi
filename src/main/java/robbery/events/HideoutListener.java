package robbery.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import robbery.backpacks.Backpacks;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

/**
 * Handles hideout-related restrictions and world-specific protections.
 * <p>
 * Features:
 * <ul>
 *     <li>Prevents players from entering the hideout if they have items in their backpack.</li>
 *     <li>Blocks teleportation commands (/spawn, /lobby, /server) if the player has items in their backpack.</li>
 *     <li>Prevents access to certain world-specific interactive commands in "world" (hideout chests, enchanter, tinkerer, alchemist, workbench).</li>
 *     <li>Prevents copper oxidation in the world to keep copper blocks intact.</li>
 * </ul>
 *
 */
public class HideoutListener implements Listener {

    /**
     * Listens for player command input and cancels commands that are restricted based on backpack contents or world rules.
     *
     * @param event the PlayerCommandPreprocessEvent
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        PlayerData pData = PlayerDataManager.getPlayerData(player);
        Backpacks backpack = pData.getBackpack();

        // Block hideout entry or teleport commands if player has items
        if ((message.startsWith("/ho") || message.startsWith("/hideout") || message.contains("/ho") || message.contains("/hideout")) ||
                (message.startsWith("/spawn") || message.startsWith("/lobby") || message.contains("/spawn") || message.contains("/lobby") ||
                        message.startsWith("/server") || message.contains("/server"))) {

            if (backpack != null && containsItems(backpack)) {
                Messages.send(player, "events.hideout.has_items");
                event.setCancelled(true);
                return;
            }
        }

        // Block hideout chest usage in "world"
        if ((message.startsWith("/ho chest") || message.startsWith("/hideout chest") ||
                message.contains("/ho chest") || message.contains("/hideout chest")) &&
                player.getWorld().getName().equalsIgnoreCase("world")) {
            Messages.send(player, "events.hideout.chest_blocked");
            event.setCancelled(true);
            return;
        }

        // Block other interactive commands (enchanter, tinkerer, alchemist, workbench) in "world"
        if ((message.startsWith("/enchanter") || message.startsWith("/tinkerer") || message.contains("/enchanter") ||
                message.contains("/tinker") || message.startsWith("/alchemist") || message.contains("/alchem") ||
                message.contains("/work") || message.startsWith("/workbench")) &&
                player.getWorld().getName().equalsIgnoreCase("world")) {
            Messages.send(player, "events.hideout.chest_blocked");
            event.setCancelled(true);
        }
    }

    /**
     * Checks if the backpack contains any items.
     *
     * @param back the backpack
     * @return true if it has items, false otherwise
     */
    private boolean containsItems(Backpacks back) {
        return back.getSize() != 0;
    }

    /**
     * Prevents copper blocks from oxidizing.
     *
     * @param event the BlockFadeEvent triggered by copper oxidation
     */
    @EventHandler
    public void onCopperOxidation(BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        if (type == Material.COPPER_BLOCK || type == Material.EXPOSED_COPPER || type == Material.WEATHERED_COPPER) {
            event.setCancelled(true);
        }
    }
}
