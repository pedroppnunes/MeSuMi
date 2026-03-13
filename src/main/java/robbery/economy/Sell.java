package robbery.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.backpacks.Backpacks;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static robbery.attribute.Attribute.*;

/**
 * Handles the /sell command, allowing players to sell all items in their
 * backpack
 * for in-game currency.
 * <p>
 * The command can be executed by players to sell their own items, or by an
 * admin
 * on behalf of another player by specifying their name as an argument.
 * </p>
 * <p>
 * There is a chance, determined by the player's SPShop upgrade, to trigger a
 * "lucky sell"
 * which gives bonus money.
 * </p>
 */
public class Sell implements CommandExecutor {

    /** Random number generator used for lucky sell chance. */
    private final Random random = new Random();
    private final Robbery main;

    private static final double BASE_HP_XP_FACTOR = 1.0; // xp = initialHp * BASE_HP_XP_FACTOR * storeMultiplier

    /**
     * Constructs a new Sell command executor.
     *
     * @param main the main plugin instance
     */
    public Sell(Robbery main) {
        this.main = main;
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
     * @param sender  the command sender (player or console)
     * @param command the command object
     * @param label   the command label
     * @param args    command arguments
     * @return true if the command was executed (even if no items were sold)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
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
        Backpacks backpack = p.getBackpack();

        long totalXp = computeXpFromItems(backpack.getItems(),p);

        double chance = p.getPerkValue(PERK_DOUBLE_INV1);
        long amountToAdd = p.getBackpack().sell();
        if (amountToAdd == 0)
            return true;
        Economy econ = Robbery.getEconomy();
        String title = "&aYou sold your items for &2" + NumberFormatter.formatDoubleNumber(amountToAdd) + "$";
        String subtitle = "";

        boolean lucky = false;

        if (chance > 0 && random.nextDouble() < chance) {
                lucky = true;

                long newAmount = amountToAdd * 2;
                econ.depositPlayer(player, newAmount);

                title = "&aYou sold your items for &2" + NumberFormatter.formatDoubleNumber(newAmount) + "$";
                subtitle = "&6You got Lucky! &e+" + (int) (chance * 100) + "% Bonus!";
        }

        if (!lucky) {
            econ.depositPlayer(player, amountToAdd);
        }

        player.sendTitle(ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 60, 10);

        Map<String, String> placeholders = Map.of(
                "amount",
                NumberFormatter.formatDoubleNumber(lucky ? (amountToAdd * 2) : amountToAdd));

        Messages.sendFormatted(player, "command.sell.sold", placeholders);

        if (lucky) {
            Map<String, String> luckyPlaceholders = Map.of("bonus", String.valueOf((int) (chance * 100)));
            Messages.sendFormatted(player, "command.sell.lucky", luckyPlaceholders);
        }

        if (totalXp > 0) {
            try {
                main.getXpManager().addXP(player, totalXp);
                long playerXP = p.getXp();
                int level = p.getLevel();
                long xpNeeded = main.getXpManager().xpNext(level);
                long xpRemaining = main.getXpManager().xpRemainingForNextLevel(playerXP, level);
                long xpIntoLevel = xpNeeded - xpRemaining;

                player.sendActionBar(
                        Component.text("+" + totalXp + " Robbery XP (" + xpIntoLevel + "/" + xpNeeded + " XP)")
                                .color(NamedTextColor.DARK_AQUA));
                Map<String, String> xpearned = Map.of("xp", String.valueOf(totalXp));
                Messages.sendFormatted(player, "command.sell.xp-earned", xpearned);
            } catch (Exception ex) {
                main.getLogger().warning("Failed to award XP on /sell for " + player.getName() + " : " + ex.getMessage());
            }
        }

        return true;
    }

    private long computeXpFromItems(List<Items> items,PlayerData pd) {
        long total = 0L;

        for (Items it : items) {
            if (it == null) continue;

            String itemId = it.getId();

            double hp = main.getItemConfig().getDouble("items." + itemId + ".hp", 1.0);

            double xpPerItem = getXpPerItem(itemId, hp) * (1 + pd.getXPBoost());

            total += (long) xpPerItem;
        }
        return total;
    }

    private double getXpPerItem(String itemId, double hp) {
        double storeMultiplier = 1.0;

        int underscoreIndex = itemId.indexOf("_");

        if (itemId.startsWith("s") && underscoreIndex > 1) {
            try {
                String storeNumStr = itemId.substring(1, underscoreIndex);
                int storeNumber = Integer.parseInt(storeNumStr);
                storeMultiplier = 1.0 + (storeNumber * 0.10);
            } catch (NumberFormatException ignored) {
            }
        }

        double baseValue = Math.pow(hp, 0.85) * 1.4;
        return baseValue * storeMultiplier;
    }

}
