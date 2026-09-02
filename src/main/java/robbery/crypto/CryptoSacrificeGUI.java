package robbery.crypto;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;

public class CryptoSacrificeGUI implements Listener {

    private final Robbery plugin;

    public CryptoSacrificeGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public static class SacrificeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final int[] LEFT_ITEM_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        Inventory gui = Bukkit.createInventory(new SacrificeHolder(), 54, getComponent("&8&lSacrifice Items for Battery"));

        ItemStack darkGlass = createGlass(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack lightGlass = createGlass(Material.GRAY_STAINED_GLASS_PANE, " ");

        // 1. Dark Glass Outer Border
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 51, 52, 53};
        for (int slot : borderSlots) {
            gui.setItem(slot, darkGlass);
        }

        // 2. Darker Glass Separation Line down center (slots 13, 22, 31, 40)
        int[] dividerSlots = {13, 22, 31, 40};
        for (int slot : dividerSlots) {
            gui.setItem(slot, darkGlass);
        }

        // 3. Help Sign at Center Upper Part (Slot 4)
        ItemStack helpSign = new ItemStack(Material.OAK_SIGN);
        ItemMeta signMeta = helpSign.getItemMeta();
        if (signMeta != null) {
            signMeta.displayName(getComponent("&e&lHow to Sacrifice"));
            List<Component> signLore = new ArrayList<>();
            signLore.add(getComponent("&7Left-Click: &a+1 Item"));
            signLore.add(getComponent("&7Right-Click: &c-1 Item"));
            signLore.add(getComponent("&7Shift + Left-Click: &aAdd ALL"));
            signLore.add(getComponent("&7Shift + Right-Click: &cClear All"));
            signMeta.lore(signLore);
            helpSign.setItemMeta(signMeta);
        }
        gui.setItem(4, helpSign);

        // 4. Populate Stolen Items on Left Side
        SacrificeManager sm = plugin.getSacrificeManager();
        UUID uuid = player.getUniqueId();
        List<Items> backpackItems = pd.getBackpack() != null ? pd.getBackpack().getItems() : Collections.emptyList();

        Map<String, Items> uniqueMap = new LinkedHashMap<>();
        for (Items item : backpackItems) {
            if (item != null && item.getId() != null) {
                uniqueMap.putIfAbsent(item.getId().toLowerCase(), item);
            }
        }
        List<Items> uniqueList = new ArrayList<>(uniqueMap.values());

        for (int i = 0; i < LEFT_ITEM_SLOTS.length; i++) {
            int slot = LEFT_ITEM_SLOTS[i];
            if (i < uniqueList.size()) {
                Items item = uniqueList.get(i);
                String itemId = item.getId();
                int available = sm.getAvailableAmountInBackpack(player, itemId);
                int selected = sm.getSelectedAmount(uuid, itemId);

                ItemStack icon;
                if (item.getSkull() != null) {
                    icon = item.getSkull().clone();
                } else {
                    icon = new ItemStack(Material.PAPER);
                }

                int displayAmount = Math.max(1, Math.min(64, selected > 0 ? selected : 1));
                icon.setAmount(displayAmount);

                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.displayName(getComponent("&e&l" + item.getName()));
                    List<Component> lore = new ArrayList<>();
                    lore.add(getComponent("&7Value: &a$" + NumberFormatter.formatDoubleNumber((double) item.getValue()) + " &7each"));
                    lore.add(getComponent("&7Available: &e" + available));
                    lore.add(getComponent("&7Selected: &6" + selected));
                    meta.lore(lore);
                    icon.setItemMeta(meta);
                }
                gui.setItem(slot, icon);
            } else {
                gui.setItem(slot, lightGlass);
            }
        }

        Map<String, Double> chances = sm.getCalculatedChances(player);

        gui.setItem(23, createCard(Material.COAL, "&8&lCoal", "&7Odds: &e" + formatOdds(chances.get("coal")), "&7Gives Quality: &b1-20%"));
        gui.setItem(24, createCard(Material.COPPER_INGOT, "&6&lCopper", "&7Odds: &e" + formatOdds(chances.get("copper")), "&7Gives Quality: &b21-40%"));
        gui.setItem(25, createCard(Material.IRON_INGOT, "&f&lIron", "&7Odds: &e" + formatOdds(chances.get("iron")), "&7Gives Quality: &b41-60%"));

        gui.setItem(32, createCard(Material.GOLD_INGOT, "&e&lGold", "&7Odds: &e" + formatOdds(chances.get("gold")), "&7Gives Quality: &b61-80%"));
        gui.setItem(33, createCard(Material.DIAMOND, "&b&lDiamond", "&7Odds: &e" + formatOdds(chances.get("diamond")), "&7Gives Quality: &b81-99%"));
        gui.setItem(34, createCard(Material.EMERALD, "&a&lEmerald (Jackpot)", "&7Odds: &e" + formatOdds(chances.get("emerald")), "&7Gives Quality: &b100%"));

        // Fill upper right slots (14, 15, 16) & lower right slots (41, 42, 43) with light glass
        int[] rightFillers = {14, 15, 16, 41, 42, 43};
        for (int slot : rightFillers) {
            gui.setItem(slot, lightGlass);
        }

        // 6. Centered Bottom Row Control Bar
        // Slot 48: Cancel Button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(getComponent("&c&lCancel"));
            cancelMeta.lore(Collections.singletonList(getComponent("&7Click to cancel selection and close")));
            cancel.setItemMeta(cancelMeta);
        }
        gui.setItem(48, cancel);

        // Slot 49: Nether Star Total Sacrifice Value (Formatted K, M, B)
        long totalVal = sm.getTotalSacrificeValue(player);
        String formattedVal = NumberFormatter.formatDoubleNumber((double) totalVal);
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta starMeta = star.getItemMeta();
        if (starMeta != null) {
            starMeta.displayName(getComponent("&6&lTotal Sacrifice Value"));
            List<Component> starLore = new ArrayList<>();
            starLore.add(getComponent("&7Total Value: &a$" + formattedVal));
            starLore.add(getComponent(""));
            starLore.add(getComponent("&7Select items on the left to increase"));
            starLore.add(getComponent("&7your battery quality!"));
            starMeta.lore(starLore);
            star.setItemMeta(starMeta);
        }
        gui.setItem(49, star);

        // Slot 50: Confirm Sacrifice Button
        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(getComponent("&a&lConfirm Sacrifice"));
            confirmMeta.lore(Arrays.asList(
                    getComponent("&7Click to sacrifice selected items"),
                    getComponent("&7and spin the Battery Roulette!")
            ));
            confirm.setItemMeta(confirmMeta);
        }
        gui.setItem(50, confirm);

        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SacrificeHolder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        SacrificeManager sm = plugin.getSacrificeManager();
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;
        UUID uuid = player.getUniqueId();

        // Slot 48: Cancel
        if (slot == 48) {
            sm.clear(uuid);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        // Slot 50: Confirm
        if (slot == 50) {
            long totalVal = sm.getTotalSacrificeValue(player);
            if (totalVal <= 0) {
                Messages.send(player, "crypto.sacrifice-empty");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // Deduct selected items from backpack
            Map<String, Integer> selectedMap = sm.getSelectedMap(uuid);
            List<Items> itemsList = pd.getBackpack().getItems();

            for (Map.Entry<String, Integer> entry : selectedMap.entrySet()) {
                String itemId = entry.getKey();
                int toRemove = entry.getValue();
                Iterator<Items> it = itemsList.iterator();
                while (it.hasNext() && toRemove > 0) {
                    Items item = it.next();
                    if (item != null && item.getId().equalsIgnoreCase(itemId)) {
                        it.remove();
                        toRemove--;
                    }
                }
            }

            sm.clear(uuid);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            // Start roulette animation
            plugin.getFuelRouletteGUI().startSpin(player, totalVal);
            return;
        }

        // Check if clicked one of the left item slots
        for (int i = 0; i < LEFT_ITEM_SLOTS.length; i++) {
            if (LEFT_ITEM_SLOTS[i] == slot) {
                List<Items> backpackItems = pd.getBackpack() != null ? pd.getBackpack().getItems() : Collections.emptyList();
                Map<String, Items> uniqueMap = new LinkedHashMap<>();
                for (Items item : backpackItems) {
                    if (item != null && item.getId() != null) {
                        uniqueMap.putIfAbsent(item.getId().toLowerCase(), item);
                    }
                }
                List<Items> uniqueList = new ArrayList<>(uniqueMap.values());

                if (i < uniqueList.size()) {
                    Items targetItem = uniqueList.get(i);
                    String itemId = targetItem.getId();
                    int available = sm.getAvailableAmountInBackpack(player, itemId);
                    ClickType click = event.getClick();

                    if (click.isShiftClick()) {
                        if (click.isLeftClick()) {
                            sm.setSelectedAmount(uuid, itemId, available);
                        } else if (click.isRightClick()) {
                            sm.setSelectedAmount(uuid, itemId, 0);
                        }
                    } else if (click.isLeftClick()) {
                        sm.addSelectedAmount(uuid, itemId, 1, available);
                    } else if (click.isRightClick()) {
                        sm.addSelectedAmount(uuid, itemId, -1, available);
                    }

                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> open(player)); // Refresh GUI
                }
                break;
            }
        }
    }

    private String formatOdds(double chancePct) {
        if (chancePct <= 0.0) return "0%";
        if (chancePct >= 100.0) return "100%";
        return String.format("%.1f%%", chancePct);
    }

    private ItemStack createGlass(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(getComponent(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCard(Material material, String name, String line1, String line2) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(getComponent(name));
            meta.lore(Arrays.asList(getComponent(line1), getComponent(line2)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component getComponent(String text) {
        return Component.text()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(text))
                .build();
    }
}
