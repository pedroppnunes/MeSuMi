package robbery.skillpoints;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.skillpoints.SkillPoint;
import robbery.skillpoints.SkillUpgradeData;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /skillpointbuy command, which allows players to purchase upgrades
 * for their skills using skill points (SP).
 * <p>
 * Players can increase attributes such as damage, extra money, item chance,
 * skillpoint chance, moneypouch chance, extra backpack slots, or instasteal chance.
 * </p>
 * <p>
 * The cost of each upgrade follows a Fibonacci sequence based on the current upgrade level.
 * If the upgrade is maxed, the command informs the player. If they don't have enough
 * SP, they are also notified.
 * </p>
 */
public class SkillpointBuy implements CommandExecutor {

    /**
     * Constructs the SkillpointBuy command executor.
     *
     * @param main the main plugin instance (not used here)
     */
    public SkillpointBuy(Robbery main) {
    }

    /**
     * Executes the /skillpointbuy command.
     * <p>
     * Usage: /skillpointbuy &lt;attribute&gt; &lt;player&gt;
     * </p>
     * <p>
     * Valid attributes: "damage", "itemchance", "extramoney", "skillpointchance",
     * "moneypouchchance", "slots", "instastealchance".
     * </p>
     * <p>
     * The method validates the player, checks current upgrade value, determines cost,
     * applies the upgrade, and sends a formatted message to the player.
     * </p>
     *
     * @param sender the command sender (player or console)
     * @param command the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command was executed successfully or usage/error message sent
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 2) {
            Messages.send(sender, "command.skillpointbuy.usage");
            return true;
        }

        String attribute  = args[0].toLowerCase();
        String targetName = args[1];
        Player player = Bukkit.getPlayer(targetName);

        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData p = PlayerDataManager.getPlayerData(player);
        SkillPoint sp = p.getSPShop();
        int currentPoints = p.getSP();

        SkillUpgradeData.UpgradeInfo info = SkillUpgradeData.UPGRADE_INFO.get(attribute);

        if (info == null) {
            Messages.send(sender, "command.skillpointbuy.unknown-upgrade");
            return true;
        }

        double currentValue = getAttributeValue(sp, attribute);
        int currentLevel = (int) (currentValue / info.increment());
        int cost = SkillUpgradeData.fibonacci(currentLevel);

        if (currentValue >= info.max()) {
            Messages.send(player, "command.skillpointbuy.maxed-out");
            return true;
        }

        if (currentPoints < cost) {
            Messages.sendFormatted(player, "command.skillpointbuy.not-enough-points", "cost", String.valueOf(cost));
            return true;
        }

        double newValue = Math.min(currentValue + info.increment(), info.max());
        p.setSP(String.valueOf(currentPoints - cost));

        SkillPoint newSP = applyUpgrade(sp, attribute, newValue);
        p.setSPShop(newSP);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("attribute", attribute);
        placeholders.put("newvalue", attribute.equals("slots") ? String.valueOf((int)newValue) : String.valueOf((int)(newValue * 100)));
        placeholders.put("cost", String.valueOf(cost));

        Messages.sendFormatted(player, attribute.equals("slots") ? "command.skillpointbuy.upgraded-slots" : "command.skillpointbuy.upgraded-percent", placeholders);

        return true;
    }

    /**
     * Retrieves the current value of a given attribute from a SkillPoint object.
     *
     * @param sp the SkillPoint object
     * @param attribute the attribute name
     * @return the current value of the attribute
     */
    private double getAttributeValue(SkillPoint sp, String attribute) {
        return switch (attribute) {
            case "damage" -> sp.extraDamage();
            case "itemchance" -> sp.doubleItemChance();
            case "extramoney" -> sp.extraMoney();
            case "skillpointchance" -> sp.skillpointChance();
            case "moneypouchchance" -> sp.moneypouchChance();
            case "slots" -> sp.extraSlots();
            case "instastealchance" -> sp.instastealChance();
            default -> 0.0;
        };
    }

    /**
     * Applies an upgrade to a SkillPoint object and returns a new SkillPoint instance
     * with the updated attribute.
     *
     * @param sp the original SkillPoint object
     * @param attribute the attribute to upgrade
     * @param newValue the new value for the attribute
     * @return a new SkillPoint instance with the upgraded value
     */
    private SkillPoint applyUpgrade(SkillPoint sp, String attribute, double newValue) {
        return switch (attribute) {
            case "damage" -> new SkillPoint(sp.doubleItemChance(), newValue, sp.extraMoney(), sp.extraSlots(), sp.skillpointChance(), sp.moneypouchChance(),sp.instastealChance());
            case "itemchance" -> new SkillPoint(newValue, sp.extraDamage(), sp.extraMoney(), sp.extraSlots(), sp.skillpointChance(), sp.moneypouchChance(),sp.instastealChance());
            case "extramoney" -> new SkillPoint(sp.doubleItemChance(), sp.extraDamage(), newValue, sp.extraSlots(), sp.skillpointChance(), sp.moneypouchChance(),sp.instastealChance());
            case "skillpointchance" -> new SkillPoint(sp.doubleItemChance(), sp.extraDamage(), sp.extraMoney(), sp.extraSlots(), newValue, sp.moneypouchChance(),sp.instastealChance());
            case "moneypouchchance" -> new SkillPoint(sp.doubleItemChance(), sp.extraDamage(), sp.extraMoney(), sp.extraSlots(), sp.skillpointChance(), newValue,sp.instastealChance());
            case "slots" -> new SkillPoint(sp.doubleItemChance(), sp.extraDamage(), sp.extraMoney(), (int)newValue, sp.skillpointChance(), sp.moneypouchChance(),sp.instastealChance());
            case "instastealchance" -> new SkillPoint(sp.doubleItemChance(), sp.extraDamage(), sp.extraMoney(), sp.extraSlots(), sp.skillpointChance(), sp.moneypouchChance(),newValue);
            default -> sp;
        };
    }
}
