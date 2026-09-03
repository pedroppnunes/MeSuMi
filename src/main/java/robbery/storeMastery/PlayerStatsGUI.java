package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import robbery.core.Robbery;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;

import static robbery.attribute.Attribute.*;

public class PlayerStatsGUI implements Listener {

    private final Robbery plugin;
    private final NamespacedKey mainmenuKey;

    public PlayerStatsGUI(Robbery plugin) {
        this.plugin = plugin;
        this.mainmenuKey = new NamespacedKey(plugin, "mainmenu_item");
    }

    public void openGUI(Player viewer, Player target) {
        if (viewer == null || target == null || !target.isOnline()) return;

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        Economy econ = Robbery.getEconomy();
        double balance = econ != null ? econ.getBalance(target) : 0.0;

        String titleStr = "Stats: " + target.getName();
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titleStr).color(NamedTextColor.GOLD));

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

        // Header Skull (Slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            headMeta.displayName(Component.text(target.getName()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            headMeta.lore(List.of(
                    Component.text("Robbery Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("Level " + pd.getLevel()).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Prestige: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("Prestige " + pd.getPrestige()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            ));
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // 1. Overview & Level Card (Slot 10)
        ItemStack overviewItem = new ItemStack(Material.CHEST);
        ItemMeta oMeta = overviewItem.getItemMeta();
        if (oMeta != null) {
            oMeta.displayName(Component.text("Overview & Account").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rank: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getRank()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Prestige Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Prestige " + pd.getPrestige()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Robbery Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Level " + pd.getLevel()).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(" (" + NumberFormatter.formatDoubleNumber((double) pd.getXp()) + " XP)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Balance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("$" + NumberFormatter.formatDoubleNumber(balance)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Skill Points: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getSkillPoints())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Total Items Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getItemsStolen())).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Total Times Busted: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getBustedCount())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            oMeta.lore(lore);
            overviewItem.setItemMeta(oMeta);
        }
        inv.setItem(10, overviewItem);

        // 2. Attributes & Multipliers Card (Slot 12)
        ItemStack attrItem = new ItemStack(Material.BEACON);
        ItemMeta aMeta = attrItem.getItemMeta();
        if (aMeta != null) {
            aMeta.displayName(Component.text("Attributes & Multipliers").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Money Multiplier: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.format("%.2f", pd.getBoost()) + "x").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Steal Speed Bonus: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getExtraDamage()) + "%").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Robbery XP Boost: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getXPBoost() * 100) + "%").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Skill Point Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.2f", pd.getPerkValue(PERK_CHANCE_SP1)) + "%").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Extra Backpack Slots: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + pd.getExtraSlots()).color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Double Item Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_DOUBLE_ITEM1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Triple Item Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_TRIPLE_ITEM1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Insta-Steal Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_INSTA_STEAL1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            aMeta.lore(lore);
            attrItem.setItemMeta(aMeta);
        }
        inv.setItem(12, attrItem);

        // 3. Equipment & Progression Card (Slot 14)
        ItemStack equipItem = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta eMeta = equipItem.getItemMeta();
        if (eMeta != null) {
            eMeta.displayName(Component.text("Equipment & Unlocks").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Current Backpack: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getBackpack().getName() + " (Cap: " + pd.getBackpack().getcapacity() + ")").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Backpacks Unlocked: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getBackpackUnlocked() + "/20").color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Current Tool: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getToolString()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Tools Unlocked: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getToolsUnlocked() + "/20").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Active Booster: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(pd.getActiveboost().getName()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Highest Key Tier: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Store " + pd.getHighestOwnedStoreTier()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            eMeta.lore(lore);
            equipItem.setItemMeta(eMeta);
        }
        inv.setItem(14, equipItem);

        // 4. Per-Store Detailed Progress Card (Slot 16)
        ItemStack storeProgressItem = new ItemStack(Material.GOLD_ORE);
        ItemMeta spMeta = storeProgressItem.getItemMeta();
        if (spMeta != null) {
            spMeta.displayName(Component.text("Store Progress & Playtimes").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            for (int i = 1; i <= 12; i++) {
                String sId = "store" + i;
                String storeName = KeyManager.getStoreN(sId);
                if (storeName == null) storeName = sId;

                int items = pd.getStoreItems(sId);
                long time = pd.getStorePlaytime(sId);
                int mastery = pd.getStoreMasteryLevel(sId);

                lore.add(Component.text("• ").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(storeName + ": ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(items + " items").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(formatSeconds(time)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(" | M" + mastery).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            }
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            spMeta.lore(lore);
            storeProgressItem.setItemMeta(spMeta);
        }
        inv.setItem(16, storeProgressItem);

        // 5. Per-Prestige Analytics Card (Slot 31)
        ItemStack prestigeAnalyticsItem = new ItemStack(Material.CLOCK);
        ItemMeta paMeta = prestigeAnalyticsItem.getItemMeta();
        if (paMeta != null) {
            paMeta.displayName(Component.text("Per-Prestige Analytics").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            int currentP = pd.getPrestige();
            for (int p = 0; p <= currentP; p++) {
                long pTime = pd.getPrestigePlaytime(p);
                int pStolen = pd.getPrestigeItemsStolen(p);
                String mostStore = pd.getMostTimeConsumingStore(p);

                lore.add(Component.text("Prestige " + p + ":").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("  Duration: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(formatSeconds(pTime)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(" | Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(String.valueOf(pStolen)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
                lore.add(Component.text("  Most Time Spent In: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(mostStore).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            }
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            paMeta.lore(lore);
            prestigeAnalyticsItem.setItemMeta(paMeta);
        }
        inv.setItem(31, prestigeAnalyticsItem);

        // 6. View Store Item Catalog Button (Slot 40)
        ItemStack catalogButton = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta cMeta = catalogButton.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("View Store Item Catalog").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            cMeta.lore(List.of(
                    Component.text("Click to open the completionist catalog menu").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("and view item values, robbery XP & discovered items!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            catalogButton.setItemMeta(cMeta);
        }
        inv.setItem(40, catalogButton);

        // Close Button (Slot 49)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(closeMeta);
        }
        inv.setItem(49, closeItem);

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = title.toString();

        // 1. Detect click in DeluxeMenus Main Menu / General Menu on slot 4 (Player Head)
        boolean isGeneralMenu = titleStr.toLowerCase().contains("general") || titleStr.toLowerCase().contains("main menu") || titleStr.toLowerCase().contains("robbery menu");
        if (isGeneralMenu && event.getSlot() == 4) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, player);
                return;
            }
        }

        // 2. Detect clicks inside PlayerStatsGUI
        if (titleStr.contains("Stats: ")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.WRITABLE_BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getStoreCatalogGUI().openStoreSelectorGUI(player);
            } else if (clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerRightClickPlayer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        Player clicker = event.getPlayer();
        ItemStack item = clicker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(mainmenuKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            openGUI(clicker, targetPlayer);
        }
    }

    @EventHandler
    public void onPlayerRightClickPlayerAt(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        Player clicker = event.getPlayer();
        ItemStack item = clicker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(mainmenuKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            openGUI(clicker, targetPlayer);
        }
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
