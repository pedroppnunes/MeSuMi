package robbery.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Random;

/**
 * Handles the /sell command, allowing players to sell all items in their backpack
 * for in-game currency.
 * <p>
 * The command can be executed by players to sell their own items, or by an admin
 * on behalf of another player by specifying their name as an argument.
 * </p>
 * <p>
 * There is a chance, determined by the player's SPShop upgrade, to trigger a "lucky sell"
 * which gives bonus money.
 * </p>
 */
public class Sell implements CommandExecutor {

    /** Random number generator used for lucky sell chance. */
    private final Random random = new Random();

    /**
     * Constructs a new Sell command executor.
     *
     * @param main the main plugin instance
     */
    public Sell(Robbery main) {
    }

    /**
     * Executes the /sell command.
     * <p>
     * If a player name is provided as an argument, the command will attempt to sell
     * items for that player. Otherwise, the sender must be a player and will sell
     * their own items.
     * </p>
     * <p>
     * The total amount earned is deposited into the player's account. If a lucky
     * sell occurs, the player receives a bonus and is notified via an action bar.
     * </p>
     *
     * @param sender the command sender (player or console)
     * @param command the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command was executed (even if no items were sold)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;

        if (args.length > 0) {
            player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline()) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            Messages.send(sender, "global.player-only");
            return true;
        }

        PlayerData p = PlayerDataManager.getPlayerData(player);
        double chance = p.getSPShop().moneypouchChance();
        long amountToAdd = p.getBackpack().sell();
        if(amountToAdd == 0)
            return true;
        Economy econ = Robbery.getEconomy();
        if(chance != 0) {
            int roll = random.nextInt((int) (1/chance)) + 1;
            if(roll == 1){
                long newAmount = Long.parseLong(NumberFormatter.formatDouble((long) (amountToAdd*(1+chance))));
                econ.depositPlayer(player, newAmount);

                String title = "&aYou sold your items for &2" + NumberFormatter.formatDoubleNumber(newAmount) + "$";
                String subtitle = "&6You got Lucky! &e+" + (int)(chance * 100) + "% Bonus!";

                player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', title),
                        ChatColor.translateAlternateColorCodes('&', subtitle),
                        10, 60, 10
                );

                return true;
            }
        }
        econ.depositPlayer(player, amountToAdd);
        String title = "&aYou sold your items for &2" + NumberFormatter.formatDoubleNumber(amountToAdd) + "$";
        String subtitle = "";

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 60, 10
        );

        return true;
    }
}
