package robbery.mechanics;

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
 */
public class HideoutListener implements Listener {

    /**
     * Listens for player command input and cancels commands that are restricted based on backpack contents or world rules.
     *
     * @param event the PlayerCommandPreprocessEvent
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase().trim();
        Player player = event.getPlayer();

        String[] parts = message.split("\\s+");
        if (parts.length >= 2) {
            String mainCmd = parts[0].toLowerCase();
            String subCmd = parts[1].toLowerCase();
            if (mainCmd.equals("/h") || mainCmd.equals("/ho") || mainCmd.equals("/hideout") ||
                mainCmd.equals("/is") || mainCmd.equals("/island")) {
                if (subCmd.equalsIgnoreCase("value") || subCmd.equalsIgnoreCase("values") ||
                    subCmd.equalsIgnoreCase("count") || subCmd.equalsIgnoreCase("counts")) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (message.startsWith("/h value") || message.startsWith("/h values") ||
            message.startsWith("/ho value") || message.startsWith("/ho values") ||
            message.startsWith("/hideout value") || message.startsWith("/hideout values") ||
            message.startsWith("/is value") || message.startsWith("/is values") ||
            message.startsWith("/island value") || message.startsWith("/island values")) {
            event.setCancelled(true);
            return;
        }

        if (message.equalsIgnoreCase("/hocontrib") || message.equalsIgnoreCase("/hideoutcontrib") ||
                message.equalsIgnoreCase("/hocontributors") || message.equalsIgnoreCase("/hideoutcontributors") ||
                message.equalsIgnoreCase("/ho contrib") || message.equalsIgnoreCase("/hideout contrib") ||
                message.equalsIgnoreCase("/ho contributors") || message.equalsIgnoreCase("/hideout contributors") ||
                message.equalsIgnoreCase("/ho topcontrib") || message.equalsIgnoreCase("/hideout topcontrib")) {
            event.setCancelled(true);
            robbery.core.HideoutTopCommand.sendHideoutTop(player);
            return;
        }

        PlayerData pData = PlayerDataManager.getPlayerData(player);
        Backpacks backpack = pData.getBackpack();
        if(player.hasPermission("robbery.bypass")) return;

        //Let people type ho chat with backpack items
        if(message.equalsIgnoreCase("/ho chat") || message.equalsIgnoreCase("/hideout chat")) return;

        // Block hideout entry or teleport commands if player has items
        if ((message.startsWith("/ho") || message.startsWith("/hideout") || message.contains("/ho") || message.contains("/hideout")) ||
                message.startsWith("/spawn") || message.startsWith("/lobby") || message.contains("/spawn") || message.contains("/lobby") ||
                        message.startsWith("/server") || message.contains("/server") || message.equalsIgnoreCase("/l") ||
                message.equalsIgnoreCase("/s") || message.equalsIgnoreCase("/outpost")) {

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

        // Block gkit in "world"
        if ((message.contains("/gkits") || message.contains("/gkit") ||
                message.startsWith("/gkit") || message.startsWith("/gkits")) &&
                player.getWorld().getName().equalsIgnoreCase("world")) {
            Messages.send(player, "events.hideout.gkit_blocked");
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

}
