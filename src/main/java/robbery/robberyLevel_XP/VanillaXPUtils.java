package robbery.robberyLevel_XP;

import org.bukkit.entity.Player;

public class VanillaXPUtils {

    public static int getTotalExperience(Player player) {
        if (player == null) return 0;
        int level = player.getLevel();
        int exp = Math.round(getExpToLevel(level) * player.getExp());
        return getLevelExp(level) + exp;
    }

    public static int getLevelExp(int level) {
        if (level <= 0) return 0;
        if (level <= 15) {
            return level * level + 6 * level;
        }
        if (level <= 30) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int getExpToLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    public static void setTotalExperience(Player player, int totalExp) {
        if (player == null) return;
        if (totalExp < 0) totalExp = 0;
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        int amount = totalExp;
        while (amount > 0) {
            int expToNext = getExpToLevel(player.getLevel());
            if (amount >= expToNext) {
                amount -= expToNext;
                player.setLevel(player.getLevel() + 1);
            } else {
                player.setExp((float) amount / (float) expToNext);
                amount = 0;
            }
        }
        player.setTotalExperience(totalExp);
    }

    public static void giveXP(Player player, int amount) {
        int current = getTotalExperience(player);
        setTotalExperience(player, Math.max(0, current + amount));
    }

    public static void removeXP(Player player, int amount) {
        giveXP(player, -amount);
    }

    public static void setXP(Player player, int amount) {
        setTotalExperience(player, Math.max(0, amount));
    }

    public static void setLevel(Player player, int level) {
        if (player == null) return;
        if (level < 0) level = 0;
        player.setLevel(level);
        player.setExp(0);
        player.setTotalExperience(getLevelExp(level));
    }

    public static void resetXP(Player player) {
        setTotalExperience(player, 0);
    }
}
