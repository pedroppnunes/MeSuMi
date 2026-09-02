package robbery.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.number.NumberFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public class BackpackGUI implements Listener {

    private final Robbery plugin;

    public BackpackGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, int page) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;
        
        List<Items> itemsList = pd.getBackpack().getItems();
        int totalItems = itemsList.size();
        int[] itemSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int itemsPerPage = itemSlots.length;
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;
        if (page < 1) page = 1;
        if (page > maxPages) page = maxPages;
        
        String title = "Your Backpack (Page " + page + "/" + maxPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title).color(NamedTextColor.DARK_GRAY));
        
        // Full outline
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        
        for (int i = startIndex; i < endIndex; i++) {
            Items itemObj = itemsList.get(i);
            if (itemObj == null) continue;
            
            ItemStack skull = itemObj.getSkull().clone();
            ItemMeta meta = skull.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(itemObj.getName()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                

                long xp = computeXp(itemObj, pd);
                
                String itemId = itemObj.getId();
                int baseValue = plugin.getItemConfig().getInt("items." + itemId + ".value", 0);
                double extraValue = Math.max(0, itemObj.getValue() - baseValue);
                
                // Recalculate base XP for display
                double hp = plugin.getItemConfig().getDouble("items." + itemId + ".hp", 1.0);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(itemId);
                int storeNum = m.find() ? Integer.parseInt(m.group()) : 1;
                double storeMultiplier = 1.0;
                int underscoreIndex = itemId.indexOf("_");
                if (itemId.startsWith("s") && underscoreIndex > 1) {
                    try {
                        storeMultiplier = 1.0 + (Integer.parseInt(itemId.substring(1, underscoreIndex)) * 0.10);
                    } catch (NumberFormatException ignored) {}
                }
                long baseXp = (long) (hp * storeMultiplier);
                long extraXp = Math.max(0, xp - baseXp);
                
                List<Component> lore = new ArrayList<>();
                
                // Value Lore
                Component valueComp = Component.text("Value: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("$" + NumberFormatter.formatDoubleNumber((double) baseValue)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                if (extraValue > 0) {
                    valueComp = valueComp.append(Component.text(" + $" + NumberFormatter.formatDoubleNumber(extraValue)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(valueComp);
                
                // XP Lore
                Component xpComp = Component.text("Robbery XP: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + NumberFormatter.formatDoubleNumber((double) baseXp)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                if (extraXp > 0) {
                    xpComp = xpComp.append(Component.text(" + " + NumberFormatter.formatDoubleNumber((double) extraXp)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(xpComp);

                
                meta.lore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "backpack_index"), PersistentDataType.INTEGER, i);
                skull.setItemMeta(meta);
            }
            inv.setItem(itemSlots[i - startIndex], skull);
        }
        
        // Navigation & Actions
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page").color(NamedTextColor.YELLOW));
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }
        
        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.displayName(Component.text("Next Page").color(NamedTextColor.YELLOW));
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }
        
        // Close Button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        closeBtn.setItemMeta(closeMeta);
        inv.setItem(49, closeBtn);
        
        player.openInventory(inv);
    }
    
    private long computeXp(Items item, PlayerData pd) {
        String itemId = item.getId();
        double hp = plugin.getItemConfig().getDouble("items." + itemId + ".hp", 1.0);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(itemId);
        int storeNum = matcher.find() ? Integer.parseInt(matcher.group()) : 1;
        String storeId = "store" + storeNum;
        
        double storeMultiplier = 1.0;
        int underscoreIndex = itemId.indexOf("_");
        if (itemId.startsWith("s") && underscoreIndex > 1) {
            try {
                String storeNumStr = itemId.substring(1, underscoreIndex);
                int storeNumber = Integer.parseInt(storeNumStr);
                storeMultiplier = 1.0 + (storeNumber * 0.10);
            } catch (NumberFormatException ignored) {}
        }
        
        double xpPerItem = hp * 1.0 * storeMultiplier * (1 + pd.getXPBoost(storeId));
        return (long) xpPerItem;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains("Your Backpack")) {
            event.setCancelled(true);
            
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            int slot = event.getRawSlot();
            
            // Navigation
            if (slot == 45 && clicked.getType() == Material.ARROW) {
                try {
                    int currentPage = Integer.parseInt(title.substring(title.indexOf("Page ") + 5, title.indexOf("/")));
                    openGUI(p, currentPage - 1);
                } catch (Exception ignored) {}
                return;
            }
            if (slot == 53 && clicked.getType() == Material.ARROW) {
                try {
                    int currentPage = Integer.parseInt(title.substring(title.indexOf("Page ") + 5, title.indexOf("/")));
                    openGUI(p, currentPage + 1);
                } catch (Exception ignored) {}
                return;
            }
            
            // Actions
            if (slot == 49 && clicked.getType() == Material.BARRIER) {
                p.closeInventory();
                return;
            }
        }
    }
}
