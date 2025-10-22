package robbery.events;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import robbery.Robbery;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.tool.ToolManager;
import robbery.tool.Tools;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for player interactions with ArmorStands representing items in the world.
 * <p>
 * This class handles the logic for picking up items from ArmorStands, canceling picking
 * when the player changes held items or clicks in the inventory, and manages instasteal
 * chance using the PickingTask system.
 * </p>
 */
public class ArmorStandInteractionListener implements Listener {

    private final NamespacedKey key;
    private final Map<UUID, PickingTask> pickingTasks = new ConcurrentHashMap<>();
    private final Robbery main;
    private final Random random = new Random();

    /**
     * Constructs a new listener for ArmorStand interactions.
     *
     * @param main The main plugin instance for accessing items and tasks.
     */
    public ArmorStandInteractionListener(Robbery main) {
        this.main = main;
        this.key = new NamespacedKey(main, "item_uuid");
    }

    /**
     * Handles left-click damage events on ArmorStands by players.
     * <p>
     * If the ArmorStand represents a pickable item, starts a PickingTask for the player.
     * Considers instasteal chance from skill points and prevents picking if the backpack is full.
     * </p>
     *
     * @param event The entity damage event.
     */
    @EventHandler
    public void onArmorStandLeftClick(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (!entity.getWorld().getName().equalsIgnoreCase("world")) return;

        if (entity.getType() == EntityType.ARMOR_STAND && damager instanceof Player player) {
            event.setCancelled(true);
            ArmorStand stand = (ArmorStand) entity;
            PersistentDataContainer dataContainer = stand.getPersistentDataContainer();

            if (dataContainer.has(key, PersistentDataType.STRING)) {
                String uuidString = dataContainer.get(key, PersistentDataType.STRING);
                assert uuidString != null;
                UUID itemUUID = UUID.fromString(uuidString);

                Items item = getItemByUUID(itemUUID);
                Tools tool = ToolManager.getToolFromItem(player.getItemInHand());
                PlayerData p = PlayerDataManager.getPlayerData(player);

                if (pickingTasks.containsKey(player.getUniqueId())) return;

                if (p.getBackpack().isFull()) {
                    Messages.sendActionBar(player, "events.backpack-full");
                    return;
                }

                if (item != null && tool != null && item.isPickable() && item.getHp() > 0 && !p.getBackpack().isFull()) {
                    if (pickingTasks.containsKey(player.getUniqueId())) return;
                    item.setPickable();
                    int roll = 0;
                    if (p.getSPShop().instastealChance() != 0)
                        roll = random.nextInt((int) (1 / p.getSPShop().instastealChance())) + 1;

                    Runnable onFinish = () -> pickingTasks.remove(player.getUniqueId());
                    PickingTask task = new PickingTask(player, item, stand, tool, main, roll == 1, onFinish);
                    pickingTasks.put(player.getUniqueId(), task);
                    task.runTaskTimer(main, 0L, 1L);
                }
            }
        }
    }

    /**
     * Cancels any active picking task if the player changes the item held in hand.
     *
     * @param event The item held change event.
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PickingTask task = pickingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Messages.sendActionBar(player, "events.picking-canceled");
        }
    }

    /**
     * Cancels any active picking task if the player interacts with their inventory.
     *
     * @param event The inventory click event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PickingTask task = pickingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Messages.sendActionBar(player, "events.picking-canceled");
        }
    }

    /**
     * Finds an item by its UUID from the list of items in the world.
     *
     * @param uuid The UUID of the item.
     * @return The corresponding Items object, or null if not found.
     */
    private Items getItemByUUID(UUID uuid) {
        for (Items item : main.getItems()) {
            if (item.getUniqueId().equals(uuid)) {
                return item;
            }
        }
        return null;
    }
}
