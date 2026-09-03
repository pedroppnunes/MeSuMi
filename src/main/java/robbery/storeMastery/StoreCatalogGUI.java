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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import me.clip.placeholderapi.PlaceholderAPI;
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
        openGUI(player, player.getName(), player.getUniqueId(), storeId, page);
    }

    public void openGUI(Player viewer, String targetName, UUID targetUuid, String storeId, int page) {
        if (viewer == null) return;
        if (targetName == null) targetName = viewer.getName();
        if (targetUuid == null) targetUuid = viewer.getUniqueId();

        PlayerData pd = getPlayerData(targetUuid);
        if (pd == null) pd = PlayerDataManager.getPlayerData(viewer);
        if (pd == null) return;

        if (storeId == null || storeId.isEmpty()) storeId = "store1";
        String storeName = KeyManager.getStoreN(storeId);
        if (storeName == null) storeName = "Store Catalog";

        List<Items> storeItemList = getItemsForStore(storeId);
        storeItemList.sort(Comparator.comparingInt(Items::getValue));
        int totalItems = storeItemList.size();

        int itemsPerPage;
        int invSize;
        int[] itemSlots;

        if (totalItems <= 7) {
            invSize = 27;
            itemSlots = new int[]{10, 11, 12, 13, 14, 15, 16};
        } else if (totalItems <= 14) {
            invSize = 36;
            itemSlots = new int[]{
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25
            };
        } else if (totalItems <= 21) {
            invSize = 45;
            itemSlots = new int[]{
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34
            };
        } else {
            invSize = 54;
            itemSlots = new int[]{
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };
        }

        itemsPerPage = itemSlots.length;
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;

        String title = "Catalog (" + page + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, invSize, Component.text(title).color(NamedTextColor.DARK_GRAY));

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < invSize; i++) {
            inv.setItem(i, glass);
        }

        // Header Player Skull (Slot 4)
        ItemStack pHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta pMeta = (SkullMeta) pHead.getItemMeta();
        if (pMeta != null) {
            pMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            pMeta.displayName(Component.text("[").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getLevel())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("] ").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(targetName).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            pMeta.lore(List.of(
                    Component.text("Inspecting Store Catalog: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(storeName).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            ));
            pHead.setItemMeta(pMeta);
        }
        inv.setItem(4, pHead);

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

        // --- Bottom Controls & Statistics Panel ---
        int bottomRowStart = invSize - 9;

        // 1. Prev Arrow (invSize - 9)
        if (page > 1) {
            ItemStack prevItem = createNavButton("Previous Page (" + (page - 1) + ")", storeId, page - 1, targetName, targetUuid);
            inv.setItem(bottomRowStart, prevItem);
        }

        // 2. Statistics Summary (invSize - 7)
        ItemStack statsItem = new ItemStack(Material.FEATHER);
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
        inv.setItem(invSize - 7, statsItem);

        // 3. Select Store Book (invSize - 6)
        ItemStack switcherItem = new ItemStack(Material.BOOK);
        ItemMeta switcherMeta = switcherItem.getItemMeta();
        if (switcherMeta != null) {
            switcherMeta.displayName(Component.text("Select Store Catalog").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            switcherMeta.lore(List.of(
                    Component.text("Click to browse catalog for other stores.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            attachTargetData(switcherMeta, targetName, targetUuid);
            switcherItem.setItemMeta(switcherMeta);
        }
        inv.setItem(invSize - 6, switcherItem);

        // 4. Close Button (invSize - 5)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(closeMeta);
        }
        inv.setItem(invSize - 5, closeItem);

        // 5. Store Masteries Button (invSize - 4) - ONLY FOR SELF PROFILE
        boolean isSelf = viewer.getUniqueId().equals(targetUuid);
        if (isSelf) {
            int storeNum = extractStoreNum(storeId);
            ItemStack masteryItem = new ItemStack(Material.PAPER);
            ItemMeta mMeta = masteryItem.getItemMeta();
            if (mMeta != null) {
                mMeta.displayName(Component.text("Store Masteries").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                mMeta.lore(List.of(
                        Component.text("Click to view mastery milestones").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("and rewards for this store!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ));
                NamespacedKey masteryKey = new NamespacedKey(plugin, "catalog_mastery_store");
                mMeta.getPersistentDataContainer().set(masteryKey, PersistentDataType.INTEGER, storeNum);
                masteryItem.setItemMeta(mMeta);
            }
            inv.setItem(invSize - 4, masteryItem);
        }

        // 6. Next Arrow (invSize - 1)
        if (page < maxPages) {
            ItemStack nextItem = createNavButton("Next Page (" + (page + 1) + ")", storeId, page + 1, targetName, targetUuid);
            inv.setItem(invSize - 1, nextItem);
        }

        viewer.openInventory(inv);
    }

    public void openStoreSelectorGUI(Player player) {
        openStoreSelectorGUI(player, player.getName(), player.getUniqueId());
    }

    public void openStoreSelectorGUI(Player viewer, String targetName, UUID targetUuid) {
        if (viewer == null) return;
        if (targetName == null) targetName = viewer.getName();
        if (targetUuid == null) targetUuid = viewer.getUniqueId();

        PlayerData pd = getPlayerData(targetUuid);
        if (pd == null) pd = PlayerDataManager.getPlayerData(viewer);

        Inventory inv = Bukkit.createInventory(null, 36, Component.text("Select Store Catalog").color(NamedTextColor.DARK_GRAY));

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.text(" "));
            glass.setItemMeta(gMeta);
        }
        for (int i = 0; i < 36; i++) inv.setItem(i, glass);

        // Header Skull at Slot 4 (Top row middle)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            int level = pd != null ? pd.getLevel() : 1;
            headMeta.displayName(Component.text("[").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(level)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("] ").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(targetName).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            headMeta.lore(null);
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // Centralized Store Item Slots: Row 1 (10..16), Row 2 (20..24)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
        for (int i = 1; i <= 12; i++) {
            String sId = "store" + i;
            String colorName = PlaceholderAPI.setPlaceholders(viewer, "%robbery_colorname_key_" + i + "%");
            if (colorName == null || colorName.startsWith("%")) {
                Keys k = KeyManager.getStoreName(sId);
                colorName = k != null ? k.getName() : sId;
            }

            ItemStack book = new ItemStack(Material.BOOK);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(colorName).decoration(TextDecoration.ITALIC, false));
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
                    // Make M1 etc red instead of pink
                    lore.add(Component.text("Mastery Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("M" + pd.getStoreMasteryLevel(sId)).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
                }
                lore.add(Component.text("Click to view store catalog & masteries!").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);

                NamespacedKey key = new NamespacedKey(plugin, "catalog_store_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sId);
                attachTargetData(meta, targetName, targetUuid);
                book.setItemMeta(meta);
            }
            if (i - 1 < slots.length) {
                inv.setItem(slots[i - 1], book);
            }
        }

        // Close Button (Slot 31 - middle of 4th row)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = closeItem.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(cMeta);
        }
        inv.setItem(31, closeItem);

        viewer.openInventory(inv);
    }

    private PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) return null;
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            return PlayerDataManager.getPlayerData(online);
        }
        org.bukkit.configuration.file.YamlConfiguration cfg = plugin.getPlayerDataDao().loadPlayerData(uuid);
        if (cfg == null) {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "Playerdata/" + uuid + ".yml");
            if (f.exists()) cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        }
        if (cfg != null) {
            PlayerData data = new PlayerData(null);
            plugin.getPlayerEventListener().loadPlayerDataFromDB(null, data, cfg);
            return data;
        }
        return null;
    }

    private void attachTargetData(ItemMeta meta, String targetName, UUID targetUuid) {
        if (meta == null) return;
        if (targetName != null) {
            NamespacedKey nKey = new NamespacedKey(plugin, "catalog_target_name");
            meta.getPersistentDataContainer().set(nKey, PersistentDataType.STRING, targetName);
        }
        if (targetUuid != null) {
            NamespacedKey uKey = new NamespacedKey(plugin, "catalog_target_uuid");
            meta.getPersistentDataContainer().set(uKey, PersistentDataType.STRING, targetUuid.toString());
        }
    }

    private ItemStack createNavButton(String name, String storeId, int targetPage, String targetName, UUID targetUuid) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            NamespacedKey sKey = new NamespacedKey(plugin, "catalog_nav_store");
            NamespacedKey pKey = new NamespacedKey(plugin, "catalog_nav_page");
            meta.getPersistentDataContainer().set(sKey, PersistentDataType.STRING, storeId);
            meta.getPersistentDataContainer().set(pKey, PersistentDataType.INTEGER, targetPage);
            attachTargetData(meta, targetName, targetUuid);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);

        if (titleStr.contains("Catalog") || titleStr.contains("Select Store Catalog")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            if (clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                return;
            }

            NamespacedKey nKey = new NamespacedKey(plugin, "catalog_target_name");
            NamespacedKey uKey = new NamespacedKey(plugin, "catalog_target_uuid");
            String targetName = meta.getPersistentDataContainer().get(nKey, PersistentDataType.STRING);
            String targetUuidStr = meta.getPersistentDataContainer().get(uKey, PersistentDataType.STRING);

            if (targetName == null) targetName = player.getName();
            UUID targetUuid = player.getUniqueId();
            if (targetUuidStr != null) {
                try {
                    targetUuid = UUID.fromString(targetUuidStr);
                } catch (IllegalArgumentException ignored) {}
            }

            // Handle Store Masteries Paper click -> open DeluxeMenus (Self only)
            NamespacedKey masteryKey = new NamespacedKey(plugin, "catalog_mastery_store");
            if (clicked.getType() == Material.PAPER && meta.getPersistentDataContainer().has(masteryKey, PersistentDataType.INTEGER)) {
                int storeNum = meta.getPersistentDataContainer().getOrDefault(masteryKey, PersistentDataType.INTEGER, 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open store" + storeNum + "mastery_menu " + player.getName());
                return;
            }

            // Handle navigation click
            NamespacedKey sKey = new NamespacedKey(plugin, "catalog_nav_store");
            NamespacedKey pKey = new NamespacedKey(plugin, "catalog_nav_page");
            if (meta.getPersistentDataContainer().has(sKey, PersistentDataType.STRING)) {
                String targetStore = meta.getPersistentDataContainer().get(sKey, PersistentDataType.STRING);
                int targetPage = meta.getPersistentDataContainer().getOrDefault(pKey, PersistentDataType.INTEGER, 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, targetName, targetUuid, targetStore, targetPage);
                return;
            }

            // Handle store selector click
            NamespacedKey selKey = new NamespacedKey(plugin, "catalog_store_id");
            if (meta.getPersistentDataContainer().has(selKey, PersistentDataType.STRING)) {
                String targetStore = meta.getPersistentDataContainer().get(selKey, PersistentDataType.STRING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, targetName, targetUuid, targetStore, 1);
                return;
            }

            if (clicked.getType() == Material.BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openStoreSelectorGUI(player, targetName, targetUuid);
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
