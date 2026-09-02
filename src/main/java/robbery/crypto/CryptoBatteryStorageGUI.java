package robbery.crypto;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.ArrayList;
import java.util.List;

public class CryptoBatteryStorageGUI implements Listener {

    private final Robbery plugin;
    private final String title = "&8&lCrypto Battery Storage";

    public CryptoBatteryStorageGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    private Component getComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, getComponent(title));

        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bg.setItemMeta(bgMeta);
        }

        int[] bgSlots = {
            0,1,2,3,4,5,6,7,8,
            9,10,11,12,13,14,15,16,17,
            18,19,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,41,42,43,44,
            45,46,47,48,50,51,52,53
        };
        for (int slot : bgSlots) {
            gui.setItem(slot, bg);
        }

        CryptoMachine machine = plugin.getCryptoManager().getOrCreateMachine(player.getUniqueId());

        // Status Item (Slot 40)
        ItemStack statusItem = new ItemStack(Material.COMPASS);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(getComponent("&b&lMachine Battery Status"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Active Battery Quality: &e" + String.format("%.1f", machine.getFuelQuality()) + "%"));
            
            long totalSeconds = machine.getFuelTicks();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String activeBattery = hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
            
            lore.add(getComponent("&7Active Battery Remaining: &a" + activeBattery));
            lore.add(getComponent("&7Active Duration Setting: &b" + CryptoMachine.getFuelDurationFormattedForLevel(machine.getFuelTimeLevel())));
            lore.add(getComponent("&7Total Stored Batteries: &e" + machine.getStoredFuels().size()));
            
            if (machine.getFuelTicks() > 0) {
                lore.add(Component.empty());
                lore.add(getComponent("&cRight-Click to Stop & Trash Active Battery!"));
            }
            
            statusMeta.lore(lore);
            statusItem.setItemMeta(statusMeta);
        }
        gui.setItem(40, statusItem);

        // Batteries (Slots 20, 21, 22, 23, 24)
        List<StoredFuel> storedFuels = machine.getStoredFuels();
        for (int i = 0; i < 5; i++) {
            if (i < storedFuels.size()) {
                StoredFuel fuel = storedFuels.get(i);
                double qual = fuel.getQuality();
                Material mat;
                String tier;
                if (qual >= 100.0) { mat = Material.EMERALD; tier = "Emerald (Jackpot)"; }
                else if (qual >= 80.0) { mat = Material.DIAMOND; tier = "Diamond"; }
                else if (qual >= 60.0) { mat = Material.GOLD_INGOT; tier = "Gold"; }
                else if (qual >= 40.0) { mat = Material.IRON_INGOT; tier = "Iron"; }
                else if (qual >= 20.0) { mat = Material.COPPER_INGOT; tier = "Copper"; }
                else { mat = Material.COAL; tier = "Coal"; }

                ItemStack batteryItem = new ItemStack(mat);
                ItemMeta batMeta = batteryItem.getItemMeta();
                if (batMeta != null) {
                    batMeta.displayName(getComponent("&6&lBattery &7(#" + (i + 1) + ")"));
                    List<Component> loreItem = new ArrayList<>();
                    loreItem.add(getComponent("&7Quality: &e" + String.format("%.1f", qual) + "%"));
                    loreItem.add(getComponent("&7Tier: &b" + tier));
                    long baseDurationTicks = machine.getFuelDurationTicks();
                    long scaledDurationTicks = (long) (baseDurationTicks * (qual / 100.0));
                    loreItem.add(getComponent("&7Duration when loaded: &b" + CryptoMachine.getFuelDurationFormattedForTicks(scaledDurationTicks)));
                    loreItem.add(Component.empty());
                    loreItem.add(getComponent("&eLeft-Click to load into machine!"));
                    loreItem.add(getComponent("&cRight-Click to trash / remove from storage!"));
                    batMeta.lore(loreItem);
                    batteryItem.setItemMeta(batMeta);
                }
                gui.setItem(20 + i, batteryItem);
            } else {
                gui.setItem(20 + i, null);
            }
        }

        // Close Button (Slot 49)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(getComponent("&c&lClose"));
            closeItem.setItemMeta(closeMeta);
        }
        gui.setItem(49, closeItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!titleStr.contains("Crypto Battery Storage")) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) return;

        int slot = event.getRawSlot();
        
        if (slot == 40 && event.isRightClick()) {
            CryptoMachine machine = plugin.getCryptoManager().getMachine(player.getUniqueId());
            if (machine != null && machine.getFuelTicks() > 0) {
                machine.setFuelTicks(0);
                machine.setFuelQuality(0);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                Messages.send(player, "crypto.battery-stopped");
                open(player);
            }
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }
        
        if (slot >= 20 && slot <= 24) {
            int index = slot - 20;
            CryptoMachine machine = plugin.getCryptoManager().getMachine(player.getUniqueId());
            if (machine != null && index < machine.getStoredFuels().size()) {
                StoredFuel fuelObj = machine.getStoredFuels().get(index);
                if (event.isLeftClick()) {
                    if (machine.getFuelTicks() > 0) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        Messages.send(player, "crypto.battery-already-active");
                        return;
                    }
                    long baseDurationTicks = machine.getFuelDurationTicks();
                    long scaledDurationTicks = (long) (baseDurationTicks * (fuelObj.getQuality() / 100.0));
                    machine.setFuelTicks(scaledDurationTicks);
                    machine.setFuelQuality(fuelObj.getQuality());
                    machine.getStoredFuels().remove(index);

                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    Messages.sendFormatted(player, "crypto.battery-loaded", java.util.Map.of("quality", String.format("%.1f", fuelObj.getQuality()), "duration", CryptoMachine.getFuelDurationFormattedForTicks(scaledDurationTicks)));
                    open(player);
                } else if (event.isRightClick()) {
                    machine.getStoredFuels().remove(index);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    Messages.send(player, "crypto.battery-trashed");
                    open(player);
                }
            }
        }
    }
}
