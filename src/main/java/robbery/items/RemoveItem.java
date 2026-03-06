package robbery.items;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.messages.Messages;

/**
 * Handles the /removeitem command, allowing OP players to remove the nearest
 * custom item (represented by {@link Items}) within a 5-block radius.
 * <p>
 * The command searches for the closest {@link Items} object to the player's
 * location, removes its associated {@link ArmorStand} entity if present,
 * and unregisters it from the plugin.
 * </p>
 * <p>
 * Only players with the permission "robbery.op" can execute this command.
 * </p>
 */
public class RemoveItem implements CommandExecutor {

    /** Reference to the main Robbery plugin instance. */
    private final Robbery main;

    /**
     * Constructs a new RemoveItem command executor.
     *
     * @param main the main plugin instance
     */
    public RemoveItem(Robbery main) {
        this.main = main;
    }

    /**
     * Executes the /removeitem command.
     * <p>
     * The command searches for the nearest {@link Items} object to the executing
     * player within a 5-block radius. If found, it removes the associated
     * {@link ArmorStand} and unregisters the item from the plugin.
     * </p>
     *
     * @param sender the command sender
     * @param command the command object
     * @param label the command label
     * @param args command arguments (ignored)
     * @return true if the command executed (even if no item was found)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        if (!player.hasPermission("robbery.op")) {
            Messages.send(player, "global.no-permission");
            return true;
        }

        Location playerLocation = player.getLocation();
        Items nearestItem = null;
        double nearestDistance = Double.MAX_VALUE;

        // Find the nearest item to the player
        for (Items item : main.getItems()) {
            double distance = item.getPosition().distance(playerLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestItem = item;
            }
        }

        // If a nearby item is found, remove its ArmorStand and unregister it
        if (nearestItem != null && nearestDistance < 5) {
            for (Entity entity : player.getWorld().getEntities()) {
                if (entity instanceof ArmorStand
                        && entity.getCustomName() != null
                        && entity.getCustomName().equals(nearestItem.getUniqueId().toString())) {
                    entity.remove();
                    break;
                }
            }
            main.removeItem(nearestItem);
            Messages.sendFormatted(player, "command.removeitem.item-removed", "name", nearestItem.getName());
        } else {
            Messages.send(player, "command.removeitem.no-items-nearby");
        }

        return true;
    }
}
