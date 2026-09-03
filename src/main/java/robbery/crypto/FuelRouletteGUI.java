package robbery.crypto;

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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.*;

public class FuelRouletteGUI implements Listener {

    private final Robbery plugin;
    private static class SpinInfo {
        int taskId;
        ItemStack winningItem;
        SpinInfo(int taskId, ItemStack winningItem) {
            this.taskId = taskId;
            this.winningItem = winningItem;
        }
    }
    private final Map<UUID, SpinInfo> activeSpins = new HashMap<>();

    public FuelRouletteGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public void startSpin(Player player, long sacrificedValue) {
        String titleStr = Messages.get("crypto.roulette.title");
        if (titleStr == null || titleStr.isEmpty()) titleStr = "&6&lRolling Fuel Quality...";
        
        Inventory inv = Bukkit.createInventory(null, 27, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(titleStr));
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17) {
                inv.setItem(i, glass);
            }
        }
        
        String topBanner = Messages.get("crypto.roulette.indicator-top");
        if (topBanner == null || topBanner.isEmpty()) topBanner = "&e&lV WINNER V";
        
        ItemStack indicator = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta indMeta = indicator.getItemMeta();
        if (indMeta != null) {
            indMeta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(topBanner));
            indicator.setItemMeta(indMeta);
        }
        inv.setItem(4, indicator);
        
        String bottomBanner = Messages.get("crypto.roulette.indicator-bottom");
        if (bottomBanner == null || bottomBanner.isEmpty()) bottomBanner = "&e&l^ WINNER ^";
        
        ItemStack indicator2 = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta indMeta2 = indicator2.getItemMeta();
        if (indMeta2 != null) {
            indMeta2.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(bottomBanner));
            indicator2.setItemMeta(indMeta2);
        }
        inv.setItem(22, indicator2);

        player.openInventory(inv);

        List<ItemStack> items = generateSpinItems(sacrificedValue);
        
        final int[] ticks = {0};
        final int[] itemIndex = {0};
        final int totalSpins = 40 + new Random().nextInt(20); 
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int delay = 0;
            int counter = 0;
            
            @Override
            public void run() {
                if (counter < delay) {
                    counter++;
                    return;
                }
                counter = 0;
                
                if (ticks[0] >= totalSpins) {
                    SpinInfo info = activeSpins.remove(player.getUniqueId());
                    if (info != null) {
                        Bukkit.getScheduler().cancelTask(info.taskId);
                        finishSpin(player, info.winningItem, true); // using precalculated
                    }
                    return;
                }
                
                for (int i = 0; i < 9; i++) {
                    int index = (itemIndex[0] + i) % items.size();
                    inv.setItem(9 + i, items.get(index));
                }
                
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                
                itemIndex[0]++;
                ticks[0]++;
                
                if (totalSpins - ticks[0] <= 15) delay = 1;
                if (totalSpins - ticks[0] <= 7) delay = 2;
                if (totalSpins - ticks[0] <= 3) delay = 4;
                if (totalSpins - ticks[0] <= 1) delay = 8;
            }
        }, 0L, 1L).getTaskId();
        
        activeSpins.put(player.getUniqueId(), new SpinInfo(taskId, items.get((totalSpins + 3) % items.size())));
    }
    
    private void finishSpin(Player player, ItemStack winner, boolean autoClose) {
        if (winner == null || !winner.hasItemMeta()) return;
        
        Integer qualObj = winner.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "fuel_quality"), PersistentDataType.INTEGER);
        if (qualObj == null) return;
        int quality = qualObj;
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        CryptoMachine machine = plugin.getCryptoManager().getOrCreateMachine(player.getUniqueId());
        if (machine != null) {
            machine.addStoredFuel(new StoredFuel(quality));
            plugin.getCryptoManager().saveMachine(machine);
        }

        Messages.sendFormatted(player, "crypto.spin-won-single", "quality", String.valueOf(quality));
        
        if (autoClose) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 40L);
        }
    }
    
    private List<ItemStack> generateSpinItems(long value) {
        List<ItemStack> items = new ArrayList<>();
        Random r = new Random();
        
        for (int i = 0; i < 60; i++) {
            items.add(getRandomMineral(value, r));
        }
        return items;
    }
    
    private ItemStack getRandomMineral(long value, Random r) {
        int roll = r.nextInt(100);
        
        Map<String, Double> chances = SacrificeManager.getCalculatedChancesByValue(value);
        
        int coalChance = chances.get("coal").intValue();
        int copperChance = chances.get("copper").intValue();
        int ironChance = chances.get("iron").intValue();
        int goldChance = chances.get("gold").intValue();
        int diamondChance = chances.get("diamond").intValue();
        
        int quality;
        Material mat;
        String nameKey;
        
        if (roll < coalChance) {
            mat = Material.COAL; nameKey = "crypto.roulette.coal";
            quality = 1 + r.nextInt(20);
        } else if (roll < coalChance + copperChance) {
            mat = Material.COPPER_INGOT; nameKey = "crypto.roulette.copper";
            quality = 21 + r.nextInt(20);
        } else if (roll < coalChance + copperChance + ironChance) {
            mat = Material.IRON_INGOT; nameKey = "crypto.roulette.iron";
            quality = 41 + r.nextInt(20);
        } else if (roll < coalChance + copperChance + ironChance + goldChance) {
            mat = Material.GOLD_INGOT; nameKey = "crypto.roulette.gold";
            quality = 61 + r.nextInt(20);
        } else if (roll < coalChance + copperChance + ironChance + goldChance + diamondChance) {
            mat = Material.DIAMOND; nameKey = "crypto.roulette.diamond";
            quality = 81 + r.nextInt(18);
        } else {
            mat = Material.EMERALD; nameKey = "crypto.roulette.emerald";
            quality = 100;
        }
        
        String nameStr = Messages.get(nameKey);
        if (nameStr == null || nameStr.isEmpty()) nameStr = nameKey;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(nameStr));
            meta.lore(Arrays.asList(Component.text("Quality: " + quality + "%").color(NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "fuel_quality"), PersistentDataType.INTEGER, quality);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (activeSpins.containsKey(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        SpinInfo info = activeSpins.remove(uuid);
        if (info != null) {
            Bukkit.getScheduler().cancelTask(info.taskId);
            finishSpin((Player) event.getPlayer(), info.winningItem, false);
        }
    }
}
