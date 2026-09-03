package robbery.claim;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import robbery.core.RewardHolder;
import robbery.core.Robbery;
import robbery.keys.Rcrate;
import robbery.messages.Messages;
import robbery.ranks.RankPaper;
import robbery.crypto.CryptoItemHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles clicks in the "Claim Rewards" GUI for the Robbery plugin.
 * <p>
 * Supports both claiming individual items or rank vouchers and claiming all available rewards at once.
 * Checks for available inventory space and updates pending rewards accordingly.
 * </p>
 */
public class ClaimGuiListener implements Listener {

    /**
     * Handles inventory click events in the reward GUI.
     * <p>
     * - Cancels default click behavior.
     * - Handles "Claim All" button (LIME_WOOL) to give as many rewards as possible.
     * - Handles individual reward clicks:
     *   - Rank vouchers are processed via RankPaper.
     *   - Crypto Machines are given as custom player head items.
     *   - Other items are added based on inventory space and stack size.
     * - Updates pending reward maps and saves after each claim.
     * - Reopens GUI if rewards remain; closes if empty or inventory full.
     * </p>
     *
     * @param e the inventory click event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof RewardHolder)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID uuid = p.getUniqueId();
        var itemRewards = Rcrate.getPendingItemRewards().get(uuid);
        var rankRewards = Rcrate.getPendingRankRewards().get(uuid);

        String claimAllName = Messages.get("command.claim.button-claim-all-name");

        // Handle "Claim All" button
        if (clicked.getType() == Material.LIME_WOOL &&
                clicked.hasItemMeta() &&
                claimAllName.equals(clicked.getItemMeta().getDisplayName())) {

            claimAllRankRewards(p, rankRewards);
            claimAllCryptoMachineRewards(p);
            claimAllItemRewards(p, itemRewards);

            Rcrate.saveRewards(uuid);
            Messages.send(p, "command.claim.all-claimed");
            p.closeInventory();
            return;
        }

        // Handle single reward click
        claimSingleRankOrItem(p, clicked, rankRewards, itemRewards);

        // Reopen GUI if rewards remain, otherwise close
        int remainingCrypto = Rcrate.getPendingCryptoMachines().getOrDefault(uuid, 0);
        boolean empty = (itemRewards == null || itemRewards.isEmpty())
                && (rankRewards == null || rankRewards.isEmpty())
                && remainingCrypto <= 0;
        if (empty) p.closeInventory();
        else Rcrate.openRewardGUI(p);
    }

    /**
     * Calculates available inventory space for rank vouchers.
     *
     * @param p the player
     * @return number of empty slots in the player's inventory
     */
    private int calculatePaperSpace(Player p) {
        int space = 0;
        for (ItemStack slot : p.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) space++;
        }
        return space;
    }

    /**
     * Calculates available space for a given material in the player's inventory.
     *
     * @param p the player
     * @param mat the material
     * @return total number of items that can be added (accounting for stack sizes)
     */
    private int calculateSpace(Player p, Material mat) {
        int max = mat.getMaxStackSize();
        int space = 0;
        for (ItemStack slot : p.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                space += max;
            } else if (slot.getType() == mat) {
                space += (max - slot.getAmount());
            }
        }
        return space;
    }

    /**
     * Adds one rank voucher of the specified rank to the player's inventory.
     *
     * @param p the player
     * @param rankKey the rank key to give
     * @return true if the item was successfully added, false if inventory was full
     */
    private boolean giveOneRank(Player p, String rankKey) {
        ItemStack one = RankPaper.create(rankKey);
        one.setAmount(1);
        var leftover = p.getInventory().addItem(one);
        return leftover.isEmpty();
    }

    /**
     * Helper method to claim all rank rewards, respecting inventory space.
     */
    private void claimAllRankRewards(Player p, Map<String,Integer> rankRewards) {
        if (rankRewards == null) return;

        Iterator<Map.Entry<String,Integer>> rit = rankRewards.entrySet().iterator();
        while (rit.hasNext()) {
            var entry = rit.next();
            String key = entry.getKey();
            int count = entry.getValue();
            int space = calculatePaperSpace(p);

            int toGive = Math.min(space, count);
            for (int i = 0; i < toGive; i++) {
                if (!giveOneRank(p, key)) break;
            }

            if (toGive >= count) rit.remove();
            else entry.setValue(count - toGive);

            if (toGive < count) break;
        }
    }

    /**
     * Helper method to claim all pending Crypto Machine rewards, respecting inventory space.
     */
    private void claimAllCryptoMachineRewards(Player p) {
        UUID uuid = p.getUniqueId();
        int count = Rcrate.getPendingCryptoMachines().getOrDefault(uuid, 0);
        if (count <= 0) return;

        Robbery plugin = Robbery.getInstance();
        int given = 0;
        for (int i = 0; i < count; i++) {
            if (p.getInventory().firstEmpty() == -1) break;
            ItemStack machineItem = CryptoItemHelper.createMachineItem(plugin);
            p.getInventory().addItem(machineItem);
            given++;
        }

        if (given > 0) {
            int left = count - given;
            if (left <= 0) {
                Rcrate.getPendingCryptoMachines().remove(uuid);
            } else {
                Rcrate.getPendingCryptoMachines().put(uuid, left);
            }
        }
    }

    /**
     * Helper method to claim all item rewards, respecting inventory space and stack size.
     */
    private void claimAllItemRewards(Player p, Map<Material,Integer> itemRewards) {
        if (itemRewards == null) return;

        Iterator<Map.Entry<Material,Integer>> iit = itemRewards.entrySet().iterator();
        while (iit.hasNext()) {
            var entry = iit.next();
            Material mat = entry.getKey();
            int pending = entry.getValue();
            int cap = calculateSpace(p, mat);
            if (cap <= 0) break;

            int toGive = Math.min(cap, pending);
            int rem = toGive;
            while (rem > 0) {
                int stack = Math.min(rem, mat.getMaxStackSize());
                var leftover = p.getInventory().addItem(new ItemStack(mat, stack));
                if (!leftover.isEmpty()) break;
                rem -= stack;
            }

            if (toGive >= pending) iit.remove();
            else entry.setValue(pending - toGive);

            if (toGive < pending) break;
        }
    }

    /**
     * Helper method to claim a single rank voucher, Crypto Machine, or item when clicked in the GUI.
     */
    private void claimSingleRankOrItem(Player p, ItemStack clicked, Map<String,Integer> rankRewards, Map<Material,Integer> itemRewards) {
        Robbery plugin = Robbery.getInstance();

        // 1. Check if clicked item is a Crypto Machine
        if (CryptoItemHelper.isCryptoMachineItem(clicked, plugin)) {
            int pending = Rcrate.getPendingCryptoMachines().getOrDefault(p.getUniqueId(), 0);
            if (pending <= 0) return;

            if (p.getInventory().firstEmpty() == -1) {
                Messages.send(p, "command.claim.inv-full");
                p.closeInventory();
                return;
            }

            ItemStack machineItem = CryptoItemHelper.createMachineItem(plugin);
            p.getInventory().addItem(machineItem);

            int left = pending - 1;
            if (left <= 0) {
                Rcrate.getPendingCryptoMachines().remove(p.getUniqueId());
            } else {
                Rcrate.getPendingCryptoMachines().put(p.getUniqueId(), left);
            }

            Rcrate.saveRewards(p.getUniqueId());
            Messages.sendFormatted(p, "command.claim.claimed-one", Map.of("amount", "1", "material", "Crypto Machine"));
            return;
        }

        // 2. Check if rank voucher
        String rankKey = RankPaper.getRankKey(clicked);
        if (rankKey != null && rankRewards != null && rankRewards.containsKey(rankKey)) {
            if (!giveOneRank(p, rankKey)) {
                Messages.send(p, "command.claim.inv-full");
                p.closeInventory();
                return;
            }
            int left = rankRewards.get(rankKey) - 1;
            if (left <= 0) rankRewards.remove(rankKey);
            else rankRewards.put(rankKey, left);

            Rcrate.saveRewards(p.getUniqueId());
            Messages.sendFormatted(p, "command.claim.claimed-one", Map.of("amount","1","material","Rank Voucher"));
        } else if (clicked.getType() != Material.PAPER && itemRewards != null) {
            Material mat = clicked.getType();
            int pending = itemRewards.getOrDefault(mat, 0);
            int cap     = calculateSpace(p, mat);
            int toGive  = Math.min(pending, cap);
            if (toGive <= 0) {
                Messages.send(p, "command.claim.inv-full");
                p.closeInventory();
                return;
            }
            int rem = toGive;
            while (rem > 0) {
                int stack = Math.min(rem, mat.getMaxStackSize());
                var leftover = p.getInventory().addItem(new ItemStack(mat, stack));
                if (!leftover.isEmpty()) break;
                rem -= stack;
            }
            int actuallyGiven = toGive - rem;
            int left = pending - actuallyGiven;
            if (left <= 0) itemRewards.remove(mat);
            else            itemRewards.put(mat, left);

            Rcrate.saveRewards(p.getUniqueId());
            Messages.sendFormatted(p, "command.claim.claimed-one",
                    Map.of("amount", String.valueOf(actuallyGiven),
                            "material", mat.name()));
        }
    }
}
