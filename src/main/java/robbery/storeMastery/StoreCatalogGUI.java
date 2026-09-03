package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreCatalogGUI implements Listener {

    public static final String UNKNOWN_ITEM_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM1OWQ5MTI3NzI0MmZjMDFjMzA5YWNjYjg3YjUzM2YxOTI5YmUxNzZlY2JhMmNkZTYzYmY2MzVlMDVlNjk5YiJ9fX0=";
    private final Robbery plugin;

    public StoreCatalogGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, String storeId, int page) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        if (storeId == null || storeId.isEmpty()) storeId = "store1";
        String storeName = KeyManager.getStoreN(storeId);
        if (storeName == null) storeName = "Store Catalog";

        List<Items> storeItemList = getItemsForStore(storeId);
        int totalItems = storeItemList.size();

        int[] itemSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        int itemsPerPage = itemSlots.length;
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        String title = storeName + " - Catalog (P." + page + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title).color(NamedTextColor.GOLD));

        // Background Glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Count unlocked items for completion calculation
        int unlockedCount = 0;
        for (Items itemObj : storeItemList) {
            if (pd.getItemStolenCount(itemObj.getId()) > 0) {
                unlockedCount++;
            }
        }
        double completionPercent = totalItems > 0 ? ((double) unlockedCount / totalItems) * 100.0 : 0.0;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            Items itemObj = storeItemList.get(i);
            if (itemObj == null) continue;

            int stolenCount = pd.getItemStolenCount(itemObj.getId());
            boolean isUnlocked = stolenCount > 0;

            ItemStack headStack;
            ItemMeta meta;

            if (!isUnlocked) {
                headStack = Items.getPlayerHead(UNKNOWN_ITEM_HEAD);
                meta = headStack.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("???").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Status: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("??? (Not Discovered)").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Value: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("???").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Robbery XP: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("???").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Times Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("0").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                    meta.lore(lore);
                }
            } else {
                headStack = itemObj.getSkull().clone();
                meta = headStack.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(itemObj.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

                    long baseValue = itemObj.getValue();
                    int storeNum = extractStoreNum(storeId);
                    long robberyXp = (long) (itemObj.getInitialhp() * (1.0 + storeNum * 0.10));

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Status: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("Unlocked").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Value: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("$" + NumberFormatter.formatDoubleNumber((double) baseValue)).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Robbery XP: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("+" + NumberFormatter.formatDoubleNumber((double) robberyXp) + " XP").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Times Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(stolenCount)).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
                    meta.lore(lore);
                }
            }

            if (meta != null) {
                headStack.setItemMeta(meta);
            }
            int targetSlot = itemSlots[i - startIndex];
            inv.setItem(targetSlot, headStack);
        }

        // Bottom Controls & Statistics Panel

        // 1. Completion & Stats Summary (Slot 49 - Nether Star)
        ItemStack statsItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.displayName(Component.text(storeName + " Statistics").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            int currentPrestige = pd.getPrestige();
            long totalStoreTime = pd.getStorePlaytime(storeId);
            long pStoreTime = pd.getPrestigeStorePlaytime(currentPrestige, storeId);
            int pStoreStolen = pd.getPrestigeStoreItemsStolen(currentPrestige, storeId);
            String mostTimeStore = pd.getMostTimeConsumingStore(currentPrestige);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Catalog Progress: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(unlockedCount + "/" + totalItems + " (" + String.format("%.1f", completionPercent) + "%)").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Lifetime Store Playtime: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatSeconds(totalStoreTime)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Prestige " + currentPrestige + " Playtime in Store: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatSeconds(pStoreTime)).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Prestige " + currentPrestige + " Items Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pStoreStolen)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Times Busted: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getBustedCount())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Prestige " + currentPrestige + " Most Time In: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(mostTimeStore).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            statsMeta.lore(lore);
            statsItem.setItemMeta(statsMeta);
        }
        inv.setItem(49, statsItem);

        // 2. Store Switcher Button (Slot 48 - Compass)
        ItemStack switcherItem = new ItemStack(Material.COMPASS);
        ItemMeta switcherMeta = switcherItem.getItemMeta();
        if (switcherMeta != null) {
            switcherMeta.displayName(Component.text("Select Store").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            switcherMeta.lore(List.of(
                    Component.text("Click to browse catalog for other stores.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            switcherItem.setItemMeta(switcherMeta);
        }
        inv.setItem(48, switcherItem);

        // 3. Navigation Buttons (Slots 45 & 53)
        if (page > 1) {
            ItemStack prevItem = createNavButton("Previous Page (" + (page - 1) + ")", storeId, page - 1);
            inv.setItem(45, prevItem);
        }
        if (page < maxPages) {
            ItemStack nextItem = createNavButton("Next Page (" + (page + 1) + ")", storeId, page + 1);
            inv.setItem(53, nextItem);
        }

        player.openInventory(inv);
    }

    public void openStoreSelectorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Select Store Catalog").color(NamedTextColor.DARK_PURPLE));
        PlayerData pd = PlayerDataManager.getPlayerData(player);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.text(" "));
            glass.setItemMeta(gMeta);
        }
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
        for (int i = 1; i <= 12; i++) {
            String sId = "store" + i;
            Keys k = KeyManager.getStoreName(sId);
            String name = k != null ? k.getName() : sId;

            ItemStack book = new ItemStack(Material.BOOK);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                if (pd != null) {
                    List<Items> storeItems = getItemsForStore(sId);
                    int unlocked = 0;
                    for (Items item : storeItems) {
                        if (pd.getItemStolenCount(item.getId()) > 0) unlocked++;
                    }
                    lore.add(Component.text("Discovered: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(unlocked + "/" + storeItems.size()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Playtime: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(formatSeconds(pd.getStorePlaytime(sId))).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
                }
                lore.add(Component.text("Click to view store catalog!").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);

                NamespacedKey key = new NamespacedKey(plugin, "catalog_store_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sId);
                book.setItemMeta(meta);
            }
            if (i - 1 < slots.length) {
                inv.setItem(slots[i - 1], book);
            }
        }
        player.openInventory(inv);
    }

    private ItemStack createNavButton(String name, String storeId, int targetPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            NamespacedKey sKey = new NamespacedKey(plugin, "catalog_nav_store");
            NamespacedKey pKey = new NamespacedKey(plugin, "catalog_nav_page");
            meta.getPersistentDataContainer().set(sKey, PersistentDataType.STRING, storeId);
            meta.getPersistentDataContainer().set(pKey, PersistentDataType.INTEGER, targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = title.toString();

        if (titleStr.contains("Catalog") || titleStr.contains("Select Store Catalog")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            // Handle navigation click
            NamespacedKey sKey = new NamespacedKey(plugin, "catalog_nav_store");
            NamespacedKey pKey = new NamespacedKey(plugin, "catalog_nav_page");
            if (meta.getPersistentDataContainer().has(sKey, PersistentDataType.STRING)) {
                String targetStore = meta.getPersistentDataContainer().get(sKey, PersistentDataType.STRING);
                int targetPage = meta.getPersistentDataContainer().getOrDefault(pKey, PersistentDataType.INTEGER, 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, targetStore, targetPage);
                return;
            }

            // Handle store selector click
            NamespacedKey selKey = new NamespacedKey(plugin, "catalog_store_id");
            if (meta.getPersistentDataContainer().has(selKey, PersistentDataType.STRING)) {
                String targetStore = meta.getPersistentDataContainer().get(selKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, targetStore, 1);
                return;
            }

            if (clicked.getType() == Material.COMPASS) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openStoreSelectorGUI(player);
            }
        }
    }

    private List<Items> getItemsForStore(String storeId) {
        List<Items> result = new ArrayList<>();
        int targetStoreNum = extractStoreNum(storeId);

        for (Map.Entry<String, Items> entry : Robbery.getItemsMap().entrySet()) {
            String itemId = entry.getKey();
            Items itemObj = entry.getValue();
            if (itemId == null || itemObj == null) continue;

            int itemStoreNum = extractStoreNum(itemId);
            if (itemStoreNum == targetStoreNum) {
                result.add(itemObj);
            }
        }
        return result;
    }

    private int extractStoreNum(String id) {
        if (id == null) return 1;
        Matcher m = Pattern.compile("\\d+").matcher(id);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group());
            } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    private String formatSeconds(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
