package robbery.items;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.messages.Messages;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /additem command, allowing OP players to spawn a floating custom item
 * in front of them for demonstration or testing purposes.
 * <p>
 * When executed, this command spawns an invisible ArmorStand and a floating Item entity
 * representing the selected {@link Items} object. The item cannot be picked up and has
 * a permanent display using custom skulls or item models.
 * </p>
 */
public class AddItem implements CommandExecutor {

    private final Robbery main;

    /**
     * Creates a new AddItem command handler.
     *
     * @param main the main plugin instance
     */
    public AddItem(Robbery main) {
        this.main = main;
    }

    /**
     * Executes the /additem command.
     * <p>
     * Usage: /additem &lt;itemName&gt;
     * Only players with the "robbery.op" permission can execute this command.
     * Spawns the item in front of the player and sends confirmation messages.
     * </p>
     *
     * @param sender  the sender of the command
     * @param command the command being executed
     * @param label   the alias used
     * @param args    command arguments (expects at least the item name)
     * @return true if the command executed, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("robbery.op")) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        if (args.length < 1) {
            Messages.send(player, "command.additem.usage");
            return true;
        }

        String itemName = args[0].toLowerCase();
        Items writtenItem = Robbery.getItemsMap().get(itemName);

        if (writtenItem == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("items", String.join(", ", Robbery.getItemsMap().keySet()));
            Messages.sendFormatted(player, "command.additem.invalid-item", placeholders);
            return true;
        }

        Items selectedItem = new Items(writtenItem);
        spawnFloatingItem(player, selectedItem);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", selectedItem.getName());
        Messages.sendFormatted(player, "command.additem.spawn-success", placeholders);
        return true;
    }

    /**
     * Spawns a floating item with an invisible ArmorStand for display.
     * <p>
     * The item entity is set to be unpickable, have unlimited lifetime, and positioned
     * exactly in front of the player. An ArmorStand is used to hold metadata and optional
     * visual features.
     * </p>
     *
     * @param player the player in front of whom the item is spawned
     * @param item   the {@link Items} object to spawn
     * @return the spawned Item entity
     */
    public Item spawnFloatingItem(Player player, Items item) {
        Location spawnLoc = player.getLocation();
        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);
        
        World world = player.getWorld();

        item.setPosition(spawnLoc.clone());
        ItemStack skull = item.getSkull();

        ArmorStand stand = world.spawn(spawnLoc.clone(), ArmorStand.class);
        stand.setInvisible(true);
        stand.setHealth(20);
        stand.setArms(false);
        stand.setBasePlate(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setCustomNameVisible(false);
        stand.setCustomName(item.getUniqueId().toString());
        stand.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());

        NamespacedKey key = new NamespacedKey("robbery", "item_uuid");
        PersistentDataContainer dataContainer = stand.getPersistentDataContainer();
        dataContainer.set(key, PersistentDataType.STRING, item.getUniqueId().toString());

        Item droppedItem = world.spawn(spawnLoc.clone(), Item.class);
        droppedItem.setItemStack(skull);
        droppedItem.setPickupDelay(Integer.MAX_VALUE);
        droppedItem.setUnlimitedLifetime(true);
        droppedItem.setVelocity(new Vector(0, 0, 0));
        droppedItem.setGravity(false); // Prevents falling/clipping
        droppedItem.setCustomName(item.getUniqueId().toString());

        item.setDroppedItem(droppedItem);
        main.addItems(item);

        return droppedItem;
    }
}
