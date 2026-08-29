package robbery.ranks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

/**
 * Handles the /rankup command which allows players to upgrade to the next store rank.
 * <p>
 * This command checks the player's current rank, prestige, and balance, then attempts to
 * upgrade them to the next store if they meet the requirements. If successful, it deducts
 * the cost from their balance, updates their store key, and adds them to the corresponding
 * region in the world.
 * </p>
 *
 * <p>Key behaviors:
 * <ul>
 *     <li>Checks if the player has reached the highest available store.</li>
 *     <li>Ensures the next store is unlocked according to prestige.</li>
 *     <li>Checks the player's balance before attempting a purchase.</li>
 *     <li>Updates player's key and store membership upon success.</li>
 * </ul>
 *
 */
public class RankUp implements CommandExecutor {

    /**
     * Constructs the RankUp command.
     *
     * @param main instance of the Robbery plugin
     */
    public RankUp(Robbery main) {
    }

    /**
     * Executes the /rankup command.
     *
     * @param sender the sender of the command
     * @param command the command object
     * @param label the command label
     * @param args command arguments (not used)
     * @return true if the command was executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.only-players");
            return true;
        }

        PlayerData data = PlayerDataManager.getPlayerData(p);
        Keys current = data.getKey();
        Keys next = KeyManager.getKeyByOrder(current.getOrder() + 1);

        // Check if player is already at the highest store
        if (next == null) {
            Messages.send(p, "command.rankup.highest-store");
            return true;
        }

        // Ensure next store is allowed according to prestige
        int prestige = data.getPrestige();
        int maxOrder = (prestige == 0 ? 10 : (prestige == 1 ? 11 : Integer.MAX_VALUE));
        if (next.getOrder() > maxOrder) {
            Messages.send(p, "command.rankup.store-locked");
            return true;
        }

        Economy econ   = Robbery.getEconomy();
        double price = next.getPrice(data);
        String priceString = NumberFormatter.formatDoubleNumber((long) price);
        // Check if player has enough money
        if (econ.getBalance(p) < price) {
            Messages.sendFormatted(p, "command.rankup.not-enough-money", Map.of("price", priceString, "store", next.getName()));
            return true;
        }

        // Deduct price and update player's key
        econ.withdrawPlayer(p, price);
        data.addKey(next.getId());
        data.setKey(next);

        // Add player to the region corresponding to the store
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "rg addmember -w world " + next.getId() + " " + p.getName());

        if (next.getOrder() == 12 && data.getPrestige() >= 3 && data.getStoreMilestone("store12") >= 5) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rg addmember -w world store13 " + p.getName());
        }

        Messages.sendFormatted(p, "command.rankup.success", Map.of("store", next.getName(), "price", priceString));

        return true;
    }
}
