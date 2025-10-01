package robbery.skillpoints;

import robbery.player.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handles skill upgrade data for players.
 * <p>
 * This class provides:
 * <ul>
 *     <li>Upgrade information for different skill categories (damage, item chance, extra money, slots, etc.).</li>
 *     <li>Methods to calculate upgrade percentages based on player data.</li>
 *     <li>Methods to calculate the next skill upgrade cost for a player.</li>
 * </ul>
 */
public class SkillUpgradeData {

    /**
     * Represents the increment per upgrade and the maximum allowed value for a skill.
     *
     * @param increment the value gained per upgrade level
     * @param max the maximum allowed value for this skill
     */
    public record UpgradeInfo(double increment, double max) {}

    /** Stores the upgrade information for each skill category. */
    public static final Map<String, UpgradeInfo> UPGRADE_INFO = new HashMap<>();

    static {
        UPGRADE_INFO.put("damage", new UpgradeInfo(0.05, 0.25));
        UPGRADE_INFO.put("itemchance", new UpgradeInfo(0.02, 0.20));
        UPGRADE_INFO.put("extramoney", new UpgradeInfo(0.05, 0.25));
        UPGRADE_INFO.put("slots", new UpgradeInfo(1, 10));
        UPGRADE_INFO.put("skillpointchance", new UpgradeInfo(0.01, 0.1));
        UPGRADE_INFO.put("moneypouchchance", new UpgradeInfo(0.02, 0.1));
        UPGRADE_INFO.put("instastealchance", new UpgradeInfo(0.01,0.05));
    }

    /**
     * Computes a Fibonacci-like number for skill scaling. Currently returns n+1.
     *
     * @param n the input number
     * @return n + 1
     */
    public static int fibonacci(int n) {
        return n + 1;
    }

    /**
     * Returns the upgrade percentage for a specific skill category for a player.
     *
     * @param p the player data
     * @param upgradeKey the skill category (e.g., "damage", "slots")
     * @param fibNumber a string representing either a level or "cost"/"max"
     * @return the upgrade percentage or maximum value depending on inputs
     */
    public static double getUpgradePercentage(PlayerData p, String upgradeKey, String fibNumber) {
        int fib = 0;

        if(fibNumber != null && fibNumber.equals("cost")) {
            return getCost(p, upgradeKey)/100;
        }

        if(fibNumber != null && !fibNumber.equals("max")) {
            fib = Integer.parseInt(fibNumber);
        } else if(fibNumber != null && !Objects.equals(upgradeKey, "slots")) {
            return UPGRADE_INFO.get(upgradeKey).max;
        } else if(fibNumber != null) {
            return UPGRADE_INFO.get(upgradeKey).max / 100;
        }

        UpgradeInfo upgradeInfo = UPGRADE_INFO.get(upgradeKey);
        if (upgradeInfo == null) return 0.0;

        double calculatedValue = fib * upgradeInfo.increment;

        if(Objects.equals(upgradeKey, "slots"))
            return Math.min(calculatedValue/100, upgradeInfo.max/100);

        return Math.min(calculatedValue, upgradeInfo.max);
    }

    /**
     * Calculates the cost for the next upgrade of a specific skill category.
     *
     * @param p the player data
     * @param category the skill category
     * @return the level/cost required for the next upgrade
     */
    public static double getCost(PlayerData p, String category) {
        UpgradeInfo info = UPGRADE_INFO.get(category);
        if (info == null) return 0;

        double current = switch (category) {
            case "damage" -> p.getSPShop().extraDamage();
            case "itemchance" -> p.getSPShop().doubleItemChance();
            case "extramoney" -> p.getSPShop().extraMoney();
            case "slots" -> p.getSPShop().extraSlots();
            case "skillpointchance" -> p.getSPShop().skillpointChance();
            case "moneypouchchance" -> p.getSPShop().moneypouchChance();
            case "instastealchance" -> p.getSPShop().instastealChance();
            default -> 0.0;
        };

        int currentLevel = (int) Math.floor(current / info.increment);

        return currentLevel + 1;
    }
}
