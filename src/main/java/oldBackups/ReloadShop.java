/*package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import robbery.events.ShopManager;

public class ReloadShop implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("robbery.manageitem")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        ShopManager.reloadShop();
        sender.sendMessage("§a[Robbery] shop.yml reloaded.");
        return true;
    }
}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        if (!e.getView().getTitle().contains(Messages.get("command.claim.gui-title-base"))) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID uid = p.getUniqueId();
        Map<Material,Integer> rewards = pendingRewards.get(uid);
        if (rewards == null) return;

        String name = clicked.getItemMeta().getDisplayName();
        if (clicked.getType()==Material.LIME_WOOL && name.equals(Messages.get("command.claim.button-claim-all-name"))) {
            for (Iterator<Map.Entry<Material,Integer>> it = rewards.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Material,Integer> entry = it.next();
                Material m = entry.getKey();
                int left = entry.getValue();

                while (left>0) {
                    int give = Math.min(64,left);
                    var leftover = p.getInventory().addItem(new ItemStack(m,give));
                    if (!leftover.isEmpty()) {
                        Messages.send(p, "command.claim.inv-full");
                        openRewardGUI(p, rewards);
                        return;
                    }
                    left -= give;
                }
                it.remove();
            }
            Messages.send(p, "command.claim.all-claimed");
            p.closeInventory();
            return;
        }

        Material mat = clicked.getType();
        int left = rewards.getOrDefault(mat, 0);
        if (left <= 0) return;

        int maxStack = mat.getMaxStackSize();
        int freeSlots = 0;

        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSlots++;
            } else if (item.getType() == mat && item.getAmount() < maxStack) {
                freeSlots++;
            }
        }

        if (freeSlots == 0) {
            Messages.send(p, "command.claim.inv-full");
            p.closeInventory();
            return;
        }

        int maxGive = maxStack * freeSlots;
        int give = Math.min(left, maxGive);

        boolean gaveSuccessfully = true;

        if (mat == Material.PAPER) {
            int total = rewards.get(mat);

            int codeToGive = -1;
            for (int code : RankPaper.DISPLAY_NAMES.keySet()) {
                if (total % code == 0) {
                    codeToGive = code;
                    break;
                }
            }

            if (codeToGive == -1) {
                Messages.send(p, "command.claim.invalid-rank-code");
                return;
            }

            ItemStack paper = RankPaper.create(codeToGive);
            var leftover = p.getInventory().addItem(paper);
            if (!leftover.isEmpty()) {
                Messages.send(p, "command.claim.inv-full");
                p.closeInventory();
                return;
            }

            int newTotal = total - codeToGive;
            if (newTotal <= 0) {
                rewards.remove(mat);
            } else {
                rewards.put(mat, newTotal);
            }

            Messages.sendFormatted(p, "command.claim.claimed-one", Map.of("amount", "1", "material", mat.name()));
            if (rewards.isEmpty()) {
                p.closeInventory();
            } else {
                openRewardGUI(p, rewards);
            }
            return;
        } else {
            while (give > 0) {
                int stackAmount = Math.min(give, maxStack);
                ItemStack stack = new ItemStack(mat, stackAmount);
                var leftover = p.getInventory().addItem(stack);
                if (!leftover.isEmpty()) {
                    gaveSuccessfully = false;
                    break;
                }
                give -= stackAmount;
            }
        }

        if (!gaveSuccessfully) {
            Messages.send(p, "command.claim.inv-full");
            p.closeInventory();
            return;
        }

        if (left > give) {
            rewards.put(mat, left - give);
        } else {
            rewards.remove(mat);
        }

        Messages.sendFormatted(p, "command.claim.claimed-one", Map.of("amount", String.valueOf(give), "material", mat.name()));
        openRewardGUI(p, rewards);
    }


 */

