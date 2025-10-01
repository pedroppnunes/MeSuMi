package robbery.ranks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import robbery.commands.RankUpdate;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

/**
 * Listener for handling the use of rank vouchers (papers) by players.
 * <p>
 * When a player interacts with a rank voucher in their main hand, this listener:
 * <ul>
 *     <li>Checks if the player’s current rank is lower than the voucher rank.</li>
 *     <li>Updates the player's rank if eligible.</li>
 *     <li>Applies the new rank permissions using LuckPerms commands.</li>
 *     <li>Consumes one rank voucher from the player’s inventory.</li>
 *     <li>Sends feedback messages to the player.</li>
 * </ul>
 */
public class RankPaperListener implements Listener {

    /**
     * Handles the player interacting with a rank voucher.
     *
     * @param e the player interaction event
     */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = e.getItem();
        String rankToken = RankPaper.getRankKey(item);
        if (rankToken == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        PlayerData data = PlayerDataManager.getPlayerData(p);
        String currentRank = data.getRank();

        if (isBetterOrEqual(currentRank, rankToken)) {
            Messages.send(p, "rank.already_have");
            return;
        }

        data.setRank(rankToken);

        // Remove all previous rank groups
        RankUpdate.rankMap.values().forEach(group ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " parent remove " + group));

        // Add the new rank group
        String parent = RankUpdate.rankMap.get(rankToken);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " parent add " + parent);

        // Consume one voucher
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            p.getInventory().remove(item);
        }

        String nice = RankPaper.getDisplayName(rankToken);
        Messages.sendFormatted(p, "rank.upgraded", Map.of("rank_name", nice));
    }

    /**
     * Checks if the player's current rank is better than or equal to the incoming rank.
     *
     * @param current the player's current rank key (e.g., "rank1")
     * @param incoming the rank key of the voucher being used
     * @return true if the player already has an equal or higher rank, false otherwise
     */
    private boolean isBetterOrEqual(String current, String incoming) {
        if (current == null) return false;
        try {
            int cur = Integer.parseInt(current.replace("rank", ""));
            int inc = Integer.parseInt(incoming.replace("rank", ""));
            return cur >= inc;
        } catch (NumberFormatException ex) {
            return true; // Assume better/equal if parsing fails
        }
    }
}
