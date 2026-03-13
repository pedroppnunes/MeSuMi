package robbery.robberyLevel_XP;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;

public class XPManager {

    private final Robbery main;

    private final int SOFTCAP = 120;
    private final double BASE_MULT = 1.4;
    private final double EXPONENT = 3.3;
    private final double SOFTCAP_MULT = 1.15;

    public XPManager(Robbery plugin) {
        this.main = plugin;
    }

    public long xpNext(int level) {
        if (level <= 0) level = 1;

        double rawXp;
        if (level <= SOFTCAP) {
            rawXp = BASE_MULT * Math.pow(level, EXPONENT);
        } else {
            double baseAtCap = BASE_MULT * Math.pow(SOFTCAP, EXPONENT);
            rawXp = baseAtCap * Math.pow(SOFTCAP_MULT, (level - SOFTCAP));
        }

        if (rawXp > Long.MAX_VALUE || rawXp < 0) return Long.MAX_VALUE / 2;

        return Math.max(100L, (long) rawXp);
    }

    public long getCumulativeForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0L;

        long total = 0L;
        for (int i = 1; i < targetLevel; i++) {
            total += xpNext(i);
        }
        return total;
    }

    public int getLevelFromXp(long xp) {
        if (xp <= 0) return 1;

        int level = 1;
        long runningTotal = 0;

        while (level < 2000) {
            long nextReq = xpNext(level);
            if (xp < runningTotal + nextReq) {
                return level;
            }
            runningTotal += nextReq;
            level++;
        }
        return level;
    }

    public void addXP(Player player, long amount) {
        if (player == null || amount <= 0) return;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        synchronized (player.getUniqueId().toString().intern()) {
            long oldXp = pd.getXp();
            long newXp = oldXp + amount;
            pd.setXp(newXp);

            int oldLevel = pd.getLevel();
            int newLevel = getLevelFromXp(newXp);

            if (newLevel > oldLevel) {
                int spGained = 0;
                for (int l = oldLevel + 1; l <= newLevel; l++) {
                    spGained += skillPointsForLevel(l);
                }

                pd.setLevel(newLevel);
                if (spGained > 0) pd.addSkillPoints(spGained);

                Bukkit.getScheduler().runTask(main, () -> {
                    main.getServer().getPluginManager().callEvent(
                            new RobberyLevelUpEvent(player, oldLevel, newLevel));
                });
            }
        }
    }

    public void setXP(Player player, long amount) {
        if (player == null || amount < 0)
            return;
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null)
            return;

        synchronized (player.getUniqueId().toString().intern()) {
            pd.setXp(amount);
            pd.setLevel(getLevelFromXp(amount));
        }
    }

    public void setLevel(Player player, int level) {
        if (player == null || level < 1)
            return;
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null)
            return;

        synchronized (player.getUniqueId().toString().intern()) {
            long newXp = getCumulativeForLevel(level);
            pd.setXp(newXp);
            pd.setLevel(level);
        }
    }

    private int skillPointsForLevel(int level) {
        if (level <= 20)
            return 1;
        if (level <= 50)
            return 2;
        if (level <= 80)
            return 3;
        if (level <= 100)
            return 4;
        if (level <= 120)
            return 5;
        return 10;
    }

    public String colorizeLevel(int level) {
        return ChatColor.translateAlternateColorCodes('&', getLevelColorCode(level)) + level;
    }

    public String getLevelColorCode(int level) {
        if (level < 20)
            return "&7";
        if (level < 40)
            return "&f";
        if (level < 60)
            return "&a";
        if (level < 80)
            return "&e";
        if (level < 100)
            return "&6";
        if (level < 120)
            return "&c";
        return "&4";
    }

    public NamedTextColor getLevelColor(int level) {
        if (level >= 120)
            return NamedTextColor.DARK_RED;
        if (level >= 100)
            return NamedTextColor.RED;
        if (level >= 80)
            return NamedTextColor.GOLD;
        if (level >= 60)
            return NamedTextColor.YELLOW;
        if (level >= 40)
            return NamedTextColor.GREEN;
        if (level >= 20)
            return NamedTextColor.WHITE;
        return NamedTextColor.GRAY;
    }

    public long xpToNextLevel(int currentLevel) {
        return xpNext(currentLevel);
    }

    public String getLevelHexColor(int level) {
        return getLevelColor(level).asHexString();
    }

    public long xpRemainingForNextLevel(long currentXp, int currentLevel) {
        long nextLevelXpRequirement = getCumulativeForLevel(currentLevel + 1);
        return Math.max(0L, nextLevelXpRequirement - currentXp);
    }
}