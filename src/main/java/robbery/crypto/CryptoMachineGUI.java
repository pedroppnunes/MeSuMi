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

            double batchPayout = plugin.getCryptoManager().getBatchPayout(machine);
            int intervalMin = Math.max(1, machine.getStealIntervalSeconds() / 60);
            lore.add(getComponent("&7Generating: &a$" + NumberFormatter.formatDoubleNumber(batchPayout) + " / batch &7(every " + intervalMin + " min)"));
            lore.add(getComponent("&7Rate per Minute: &a$" + NumberFormatter.formatDoubleNumber(batchPayout / (double) intervalMin) + " / min"));
            
            int capacity = machine.getCapacity();
            lore.add(getComponent("&7Steal Interval: &e" + intervalMin + " min &7(Batch: &e" + capacity + " items&7)"));
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
            
            String speedStr = machine.getSpeedLevel() >= 10 ? "&aMAXED" : machine.getSpeedLevel() + " &8/ &710";
            String capacityStr = machine.getCapacityLevel() >= 10 ? "&aMAXED" : machine.getCapacityLevel() + " &8/ &710";
            String durationStr = machine.getFuelTimeLevel() >= 10 ? "&aMAXED" : machine.getFuelTimeLevel() + " &8/ &710";
            String rewardStr = machine.getRewardLevel() >= 10 ? "&aMAXED" : machine.getRewardLevel() + " &8/ &710";

            lore.add(getComponent(" &8- &7Speed: &e" + speedStr));
            lore.add(getComponent(" &8- &7Capacity: &e" + capacityStr));
            lore.add(getComponent(" &8- &7Duration: &b" + durationStr));
            lore.add(getComponent(" &8- &7Reward: &a" + rewardStr));
            lore.add(Component.empty());

            double mult = plugin.getCryptoManager().getMultiplier(machine);
            lore.add(getComponent("&7Total Multiplier: &a" + String.format("%.2f", mult) + "x"));

            robbery.player.PlayerData pd = robbery.player.PlayerDataManager.getPlayerData(player);
            int storeOrder = (pd != null && pd.getKey() != null) ? pd.getKey().getOrder() : 1;
            double storeEff = CryptoMachine.getStoreEfficiencyMultiplier(storeOrder);
            lore.add(getComponent("&7Store Efficiency: &e" + String.format("%.1f", storeEff * 100.0) + "%"));

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

        // Upgrade Speed (Slot 19)
        ItemStack speedItem = new ItemStack(Material.SUGAR);
        ItemMeta speedMeta = speedItem.getItemMeta();
        if (speedMeta != null) {
            speedMeta.displayName(getComponent("&fUpgrade Steal Speed"));
            List<Component> lore = new ArrayList<>();
            int speedLvl = machine.getSpeedLevel();

            if (speedLvl >= 10) {
                lore.add(getComponent("&7Current Level: &aLevel 10 (MAX)"));
                lore.add(getComponent("&7Steal Interval: &e1 Minute per cycle"));
                lore.add(Component.empty());
                lore.add(getComponent("&fNext Cost: &aMAXED"));
                lore.add(getComponent("&fPrestige Req: &aMAXED"));
            } else {
                int nextLvl = speedLvl + 1;
                int curMin = Math.max(1, machine.getStealIntervalSeconds() / 60);
                int nextMin = Math.max(1, (600 - (Math.min(9, nextLvl) * 60)) / 60);

                lore.add(getComponent("&7Current Level: &eLevel " + speedLvl + " &8/ &710"));
                lore.add(getComponent("&7Steal Interval: &e" + curMin + "m &7-> &a" + nextMin + "m"));
                lore.add(getComponent("&7Reduces cycle interval time between steals."));
                lore.add(Component.empty());

                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
                lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
                lore.add(Component.empty());
                lore.add(getComponent("&eClick to upgrade!"));
            }
            speedMeta.lore(lore);
            speedItem.setItemMeta(speedMeta);
        }
        gui.setItem(19, speedItem);

        // Upgrade Batch Capacity (Slot 21)
        ItemStack capacityItem = new ItemStack(Material.CHEST);
        ItemMeta capacityMeta = capacityItem.getItemMeta();
        if (capacityMeta != null) {
            capacityMeta.displayName(getComponent("&fUpgrade Batch Capacity"));
            List<Component> lore = new ArrayList<>();
            int capLvl = machine.getCapacityLevel();

            if (capLvl >= 10) {
                lore.add(getComponent("&7Current Level: &aLevel 10 (MAX)"));
                lore.add(getComponent("&7Batch Capacity: &e10 Items per cycle"));
                lore.add(Component.empty());
                lore.add(getComponent("&fNext Cost: &aMAXED"));
                lore.add(getComponent("&fPrestige Req: &aMAXED"));
            } else {
                int nextLvl = capLvl + 1;
                int curCap = machine.getCapacity();
                int nextCap = Math.min(10, nextLvl + 1);

                lore.add(getComponent("&7Current Level: &eLevel " + capLvl + " &8/ &710"));
                lore.add(getComponent("&7Batch Capacity: &e" + curCap + " items &7-> &a" + nextCap + " items"));
                lore.add(getComponent("&7Increases item capacity stolen per cycle."));
                lore.add(Component.empty());

                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
                lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
                lore.add(Component.empty());
                lore.add(getComponent("&eClick to upgrade!"));
            }
            capacityMeta.lore(lore);
            capacityItem.setItemMeta(capacityMeta);
        }
        gui.setItem(21, capacityItem);

        // Upgrade Battery Duration (Slot 23)
        ItemStack durationItem = new ItemStack(Material.CLOCK);
        ItemMeta durationMeta = durationItem.getItemMeta();
        if (durationMeta != null) {
            durationMeta.displayName(getComponent("&fUpgrade Battery Duration"));
            List<Component> lore = new ArrayList<>();
            int durationLvl = machine.getFuelTimeLevel();

            if (durationLvl >= 10) {
                lore.add(getComponent("&7Current Level: &bLevel 10 (MAX)"));
                lore.add(getComponent("&7Active Duration: &b" + CryptoMachine.getFuelDurationFormattedForLevel(10)));
                lore.add(Component.empty());
                lore.add(getComponent("&fNext Cost: &aMAXED"));
                lore.add(getComponent("&fPrestige Req: &aMAXED"));
            } else {
                int nextLvl = durationLvl + 1;
                String curDur = CryptoMachine.getFuelDurationFormattedForLevel(durationLvl);
                String nextDur = CryptoMachine.getFuelDurationFormattedForLevel(nextLvl);

                lore.add(getComponent("&7Current Level: &bLevel " + durationLvl + " &8/ &710"));
                lore.add(getComponent("&7Active Duration: &b" + curDur + " &7-> &a" + nextDur));
                lore.add(getComponent("&7Increases active battery duration per canister (up to 24 Hours)."));
                lore.add(Component.empty());

                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
                lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
                lore.add(Component.empty());
                lore.add(getComponent("&eClick to upgrade!"));
            }
            durationMeta.lore(lore);
            durationItem.setItemMeta(durationMeta);
        }
        gui.setItem(23, durationItem);

        // Upgrade Money Reward (Slot 25)
        ItemStack rewardItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta rewardMeta = rewardItem.getItemMeta();
        if (rewardMeta != null) {
            rewardMeta.displayName(getComponent("&fUpgrade Money Reward"));
            List<Component> lore = new ArrayList<>();
            int rewardLvl = machine.getRewardLevel();

            if (rewardLvl >= 10) {
                lore.add(getComponent("&7Current Level: &aLevel 10 (MAX)"));
                lore.add(getComponent("&7Money Multiplier: &a" + String.format("%.2fx", machine.getRewardMultiplier())));
                lore.add(Component.empty());
                lore.add(getComponent("&fNext Cost: &aMAXED"));
                lore.add(getComponent("&fPrestige Req: &aMAXED"));
            } else {
                int nextLvl = rewardLvl + 1;
                double curMult = machine.getRewardMultiplier();
                double nextMult = (nextLvl >= 10) ? 3.0 : (1.0 + (nextLvl * (2.0 / 9.0)));

                lore.add(getComponent("&7Current Level: &aLevel " + rewardLvl + " &8/ &710"));
                lore.add(getComponent("&7Money Multiplier: &a" + String.format("%.2fx", curMult) + " &7-> &a" + String.format("%.2fx", nextMult)));
                lore.add(getComponent("&7Increases money payout multiplier."));
                lore.add(Component.empty());

                int cost = CryptoUpgradeManager.getCreditCost(nextLvl);
                lore.add(getComponent("&fNext Cost: &6&l⛁ " + cost + " Crypto Credits"));
                lore.add(getComponent("&fPrestige Req: &ePrestige " + CryptoUpgradeManager.getRequiredPrestige(nextLvl)));
                lore.add(Component.empty());
                lore.add(getComponent("&eClick to upgrade!"));
            }
            rewardMeta.lore(lore);
            rewardItem.setItemMeta(rewardMeta);
        }
        gui.setItem(25, rewardItem);

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
        } else if (slot == 19) { // Upgrade Speed
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeTrack(player, machine, "speed");
            open(player);
        } else if (slot == 21) { // Upgrade Batch Capacity
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeTrack(player, machine, "capacity");
            open(player);
        } else if (slot == 23) { // Upgrade Duration
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeTrack(player, machine, "duration");
            open(player);
        } else if (slot == 25) { // Upgrade Reward
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            CryptoUpgradeManager.upgradeTrack(player, machine, "reward");
            open(player);
        } else if (slot == 31) { // Pickup Machine
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            plugin.getCryptoManager().pickupMachine(player);
            player.closeInventory();
        }
    }
}
