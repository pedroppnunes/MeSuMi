package robbery.mechanics;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static robbery.attribute.Attribute.PERK_INSTA_STEAL1;

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
        this.key = new NamespacedKey("robbery", "item_uuid");
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
    public void onArmorStandDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ARMOR_STAND) {
            ArmorStand stand = (ArmorStand) entity;
            if (stand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                // Cancel all damage to these custom armor stands unless it's a player left-click
                // Player left-clicks are handled specifically in EntityDamageByEntityEvent below
                if (!(event instanceof EntityDamageByEntityEvent edbe && edbe.getDamager() instanceof Player)) {
                    event.setCancelled(true);
                }
            }
        }
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
        if(entity instanceof Player) return;
        if (entity.getType() == EntityType.ARMOR_STAND && damager instanceof Player player) {
            event.setCancelled(true);
            ArmorStand stand = (ArmorStand) entity;
            PersistentDataContainer dataContainer = stand.getPersistentDataContainer();

            if (dataContainer.has(key, PersistentDataType.STRING)) {
                String uuidString = dataContainer.get(key, PersistentDataType.STRING);
                assert uuidString != null;
                UUID itemUUID = UUID.fromString(uuidString);

                Items item = getItemByUUID(itemUUID);
                Tools tool = ToolManager.getToolFromItem(player.getInventory().getItemInMainHand());
                PlayerData p = PlayerDataManager.getPlayerData(player);

                if (pickingTasks.containsKey(player.getUniqueId())) return;

                if (p.getBackpack().isFull()) {
                    Messages.sendActionBar(player, "events.backpack-full");
                    return;
                }

                if (item == null || tool == null) return;
                
                String itemId = item.getId();
                String playerStoreKey = p.getKey().getId();

                double itemStoreNum = extractStoreNumber(itemId);
                double playerStoreNum = extractStoreNumber(playerStoreKey);

                int playerPrestige = p.getPrestige();

                if (itemStoreNum <= 12) {
                    if (itemStoreNum > playerStoreNum) {
                        Messages.sendActionBar(player, "events.wrong-store");
                        return;
                    }
                }
                if (itemStoreNum > 12) {
                    int requiredPrestige = 3;
                    int requiredMastery = 5;

                    if (playerPrestige < requiredPrestige || p.getKey().getOrder() != 12 || p.getStoreMilestone("store12") < requiredMastery) {
                        Messages.sendActionBar(player, "events.wrong-store");
                        return;
                    }
                }

                if (!item.isPickable() && item.getHp() > 0) {
                    Messages.sendActionBar(player, "events.already-being-picked");
                    return;
                }

                if (item.isPickable() && item.getHp() > 0 && !p.getBackpack().isFull()) {

                    if (pickingTasks.containsKey(player.getUniqueId())) return;
                    item.togglePickable();
                    Runnable onFinish = () -> pickingTasks.remove(player.getUniqueId());
                    PickingTask task = new PickingTask(player, item, stand, tool, main, onFinish);
                    pickingTasks.put(player.getUniqueId(), task);
                    task.runTaskTimer(main, 0L, 1L);
                }
            }
        }
    }

    /**
     * Handles right-click on an item armor stand to show base price and player's sell price.
     */
    @EventHandler
    public void onArmorStandRightClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;
        Player player = event.getPlayer();

        PersistentDataContainer pdc = stand.getPersistentDataContainer();
        if (!pdc.has(key, PersistentDataType.STRING)) return;

        event.setCancelled(true);

        String uuidStr = pdc.get(key, PersistentDataType.STRING);
        if (uuidStr == null) return;

        Items item;
        try {
            item = getItemByUUID(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (item == null) return;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        double basePrice = item.getValue();
        // "Your Price" includes booster and mastery money multiplier
        String storeId = "store1";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(item.getId());
        if (m.find()) {
            storeId = "store" + m.group();
        }

        double yourPrice = basePrice * pd.getBoost() * (1.0 + pd.getStoreMasteryMoneyMultiplier(storeId));

        String baseFormatted = robbery.number.NumberFormatter.formatDoubleNumber(basePrice) + "$";
        String yourFormatted = robbery.number.NumberFormatter.formatDoubleNumber(yourPrice) + "$";

        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7&l" + item.getName() + " &8| &7Price: &a" + baseFormatted + " &8| &7Your Price: &a" + yourFormatted));
    }

    private double extractStoreNumber(String id) {
        Matcher matcher = Pattern.compile("\\d+(\\.\\d+)?").matcher(id);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        return 0;
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
            task.resetAndCancel();
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
            task.resetAndCancel();
            Messages.sendActionBar(player, "events.picking-canceled");
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PickingTask task = pickingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.resetAndCancel();
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
