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
import robbery.number.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

public class CryptoMachineGUI implements Listener {

    private final Robbery plugin;
    private final String title = "&8&lCrypto Machine";

    public CryptoMachineGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    private Component getComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        Inventory gui = Bukkit.createInventory(null, 36, getComponent(title));

        // Border items
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.empty());
            border.setItemMeta(borderMeta);
        }

        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,9,
            17,18,
            26,27,28,29,30,31,32,33,34,35
        };
        for (int slot : borderSlots) {
            gui.setItem(slot, border);
        }

        CryptoMachine machine = plugin.getCryptoManager().getOrCreateMachine(player.getUniqueId());

        // Status Item (Slot 4)
        ItemStack statusItem = new ItemStack(Material.COMPASS);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(getComponent("&fMachine Info"));
            List<Component> lore = new ArrayList<>();

            String statusStr = !machine.isPlaced() ? "&7Not Placed" : (machine.getFuelTicks() > 0 ? "&aActive" : "&cDepleted");
            lore.add(getComponent("&7Status: " + statusStr));
            lore.add(getComponent("&7Unclaimed Balance: &a$" + NumberFormatter.formatDoubleNumber(machine.getUnclaimedMoneyDouble())));

            double moneyPerSec = plugin.getCryptoManager().getMoneyPerSecond(machine);
            lore.add(getComponent("&7Generating: &a$" + NumberFormatter.formatDoubleNumber(moneyPerSec) + " / sec"));
            lore.add(getComponent("&7Active Battery Quality: &e" + String.format("%.1f", machine.getFuelQuality()) + "%"));

            long totalSeconds = machine.getFuelTicks();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String batteryRem = hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);

            lore.add(getComponent("&7Active Battery Remaining: &a" + batteryRem));
            lore.add(getComponent("&7Active Battery Duration: &b" + CryptoMachine.getFuelDurationFormattedForLevel(machine.getFuelTimeLevel())));
            lore.add(Component.empty());
            lore.add(getComponent("&7Upgrade Levels (Max 10):"));
            lore.add(getComponent(" &8- &7Speed: &e" + machine.getSpeedLevel() + " &8/ &710"));
            lore.add(getComponent(" &8- &7Duration: &b" + machine.getFuelTimeLevel() + " &8/ &710"));
            lore.add(getComponent(" &8- &7Reward: &a" + machine.getRewardLevel() + " &8/ &710"));
            lore.add(Component.empty());

            double mult = plugin.getCryptoManager().getMultiplier(machine);
            lore.add(getComponent("&7Total Multiplier: &a" + String.format("%.2f", mult) + "x"));

            statusMeta.lore(lore);
            statusItem.setItemMeta(statusMeta);
        }
        gui.setItem(4, statusItem);

        // Virtual Battery Storage (Slot 12)
        ItemStack storageItem = new ItemStack(Material.COAL);
        ItemMeta storageMeta = storageItem.getItemMeta();
        if (storageMeta != null) {
            storageMeta.displayName(getComponent("&fVirtual Battery Storage"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Stored Battery Canisters: &e" + machine.getStoredFuels().size()));
            lore.add(Component.empty());
            lore.add(getComponent("&7Select which battery canister to load"));
            lore.add(getComponent("&7into your machine or delete canisters."));
            lore.add(Component.empty());
            lore.add(getComponent("&eClick to open!"));
            storageMeta.lore(lore);
            storageItem.setItemMeta(storageMeta);
        }
        gui.setItem(12, storageItem);

        // Claim Money (Slot 14)
        ItemStack claimItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta claimMeta = claimItem.getItemMeta();
        if (claimMeta != null) {
            claimMeta.displayName(getComponent("&fClaim Money"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Unclaimed Balance: &a$" + NumberFormatter.formatDoubleNumber(machine.getUnclaimedMoneyDouble())));
            lore.add(Component.empty());
            lore.add(getComponent("&eClick to deposit into bank!"));
            claimMeta.lore(lore);
            claimItem.setItemMeta(claimMeta);
        }
        gui.setItem(14, claimItem);

        // Upgrade Speed (Slot 20)
        ItemStack speedItem = new ItemStack(Material.SUGAR);
        ItemMeta speedMeta = speedItem.getItemMeta();
        if (speedMeta != null) {
            speedMeta.displayName(getComponent("&fUpgrade Speed"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Current Level: &e" + machine.getSpeedLevel() + " &8/ &710"));
            lore.add(getComponent("&7Increases generation speed per second."));
            lore.add(Component.empty());

            int nextLvl = machine.getSpeedLevel() + 1;
            if (machine.getSpeedLevel() >= CryptoUpgradeManager.getMaxLevel()) {
                lore.add(getComponent("&fNext Cost: &aMAXED"));
            } else {
                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
            }
            lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
            lore.add(Component.empty());
            lore.add(getComponent("&eClick to upgrade!"));
            speedMeta.lore(lore);
            speedItem.setItemMeta(speedMeta);
        }
        gui.setItem(20, speedItem);

        // Upgrade Battery Duration (Slot 22)
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = durationItem.getItemMeta();
        if (durationMeta != null) {
            durationMeta.displayName(getComponent("&fUpgrade Battery Duration"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Current Level: &b" + machine.getFuelTimeLevel() + " &8/ &710"));
            lore.add(getComponent("&7Current Duration: &b" + CryptoMachine.getFuelDurationFormattedForLevel(machine.getFuelTimeLevel())));
            lore.add(getComponent("&7Increases active battery duration."));
            lore.add(Component.empty());

            int nextLvl = machine.getFuelTimeLevel() + 1;
            if (machine.getFuelTimeLevel() >= CryptoUpgradeManager.getMaxLevel()) {
                lore.add(getComponent("&fNext Cost: &aMAXED"));
            } else {
                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
            }
            lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
            lore.add(Component.empty());
            lore.add(getComponent("&eClick to upgrade!"));
            durationMeta.lore(lore);
            durationItem.setItemMeta(durationMeta);
        }
        gui.setItem(22, durationItem);

        // Upgrade Money Reward (Slot 24)
        ItemStack rewardItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta rewardMeta = rewardItem.getItemMeta();
        if (rewardMeta != null) {
            rewardMeta.displayName(getComponent("&fUpgrade Money Reward"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Current Level: &a" + machine.getRewardLevel() + " &8/ &710"));
            lore.add(getComponent("&7Increases money payout multiplier."));
            lore.add(Component.empty());

            int nextLvl = machine.getRewardLevel() + 1;
            if (machine.getRewardLevel() >= CryptoUpgradeManager.getMaxLevel()) {
                lore.add(getComponent("&fNext Cost: &aMAXED"));
            } else {
                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
            }
            lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
            lore.add(Component.empty());
            lore.add(getComponent("&eClick to upgrade!"));
            rewardMeta.lore(lore);
            rewardItem.setItemMeta(rewardMeta);
        }
        gui.setItem(24, rewardItem);

        // Pick Up Machine (Slot 31)
        ItemStack pickupItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta pickupMeta = pickupItem.getItemMeta();
        if (pickupMeta != null) {
            pickupMeta.displayName(getComponent("&cPick Up Machine"));
            List<Component> lore = new ArrayList<>();
            lore.add(getComponent("&7Picks up your machine into your inventory."));
            lore.add(getComponent("&cWARNING: &7This will pause generation"));
            lore.add(getComponent("&7until you place it again."));
            lore.add(Component.empty());
            lore.add(getComponent("&cClick to pick up!"));
            pickupMeta.lore(lore);
            pickupItem.setItemMeta(pickupMeta);
        }
        gui.setItem(31, pickupItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView() == null || event.getView().getTitle() == null) return;
        String rawTitle = LegacyComponentSerializer.legacyAmpersand().serialize(event.getView().title());
        if (!rawTitle.contains("Crypto Machine")) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;

        CryptoMachine machine = plugin.getCryptoManager().getOrCreateMachine(player.getUniqueId());

        if (slot == 12) { // Virtual Battery Storage
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            plugin.getCryptoBatteryStorageGUI().open(player);
        } else if (slot == 14) { // Claim Money
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            plugin.getCryptoManager().claimMoney(player);
            open(player);
        } else if (slot == 20) { // Upgrade Speed
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeMachine(player, machine);
            open(player);
        } else if (slot == 22) { // Upgrade Duration
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeMachine(player, machine);
            open(player);
        } else if (slot == 24) { // Upgrade Reward
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeMachine(player, machine);
            open(player);
        } else if (slot == 31) { // Pickup Machine
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            plugin.getCryptoManager().pickupMachine(player);
            player.closeInventory();
        }
    }
}
