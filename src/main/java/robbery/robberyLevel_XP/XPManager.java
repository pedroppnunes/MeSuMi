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

    // Cache cumulative XP required to reach level L
    // Index 0 = Level 1 (0 XP), Index 1 = Level 2, etc.
    private final List<Long> cumulativeCache = new ArrayList<>();
    private final int INITIAL_CACHE_SIZE = 2000;

    // Growth constants
    private final int SOFTCAP = 120;
    private final double BASE_MULT = 100.0;
    private final double EXPONENT = 2.2;
    private final double SOFTCAP_MULT = 1.15;

    public XPManager(Robbery plugin) {
        this.main = plugin;
        precomputeCumulative(INITIAL_CACHE_SIZE);
    }

    private void precomputeCumulative(int maxLevel) {
        synchronized (cumulativeCache) {
            cumulativeCache.clear();
            long runningTotal = 0L;
            cumulativeCache.add(0L); // Level 1 starts at 0 XP
            for (int level = 1; level < maxLevel; level++) {
                runningTotal += xpNext(level);
                cumulativeCache.add(runningTotal);
            }
        }
    }

    public long xpNext(int level) {
        if (level <= 0)
            level = 1;

        long rawXp;
        if (level <= SOFTCAP) {
            rawXp = (long) (BASE_MULT * Math.pow(level, EXPONENT));
        } else {
            long base = (long) (BASE_MULT * Math.pow(SOFTCAP, EXPONENT));
            double multiplier = Math.pow(SOFTCAP_MULT, (level - SOFTCAP));
            rawXp = (long) (base * multiplier);
        }

        return Math.max(100L, rawXp);
    }

    public long getCumulativeForLevel(int targetLevel) {
        if (targetLevel <= 1)
            return 0L;

        synchronized (cumulativeCache) {
            // If requested level is beyond cache, expand it
            while (cumulativeCache.size() < targetLevel) {
                int currentMaxLevel = cumulativeCache.size();
                long nextTotal = cumulativeCache.get(currentMaxLevel - 1) + xpNext(currentMaxLevel);
                cumulativeCache.add(nextTotal);
            }
            return cumulativeCache.get(targetLevel - 1);
        }
    }

    public int getLevelFromXp(long xp) {
        if (xp <= 0)
            return 1;

        synchronized (cumulativeCache) {
            // Ensure cache is large enough to potentially contain this XP
            // If XP is higher than our last cached level, grow cache until it fits
            while (xp >= cumulativeCache.get(cumulativeCache.size() - 1)) {
                int currentMaxLevel = cumulativeCache.size();
                long nextTotal = cumulativeCache.get(currentMaxLevel - 1) + xpNext(currentMaxLevel);
                cumulativeCache.add(nextTotal);

                // Safety cap to prevent infinite loops if math breaks
                if (currentMaxLevel > 1000000)
                    break;
            }

            // Binary Search for efficiency
            int low = 0;
            int high = cumulativeCache.size() - 1;
            int level = 1;

            while (low <= high) {
                int mid = (low + high) / 2;
                if (cumulativeCache.get(mid) <= xp) {
                    level = mid + 1;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return level;
        }
    }

    public void addXP(Player player, long amount) {
        if (player == null || amount <= 0)
            return;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null)
            return;

        // Use a consistent lock object from PlayerData if available,
        // otherwise stay with the internal string lock.
        synchronized (player.getUniqueId().toString().intern()) {
            long oldXp = pd.getXp();
            long newXp = oldXp + amount;
            pd.setXp(newXp);

            int oldLevel = pd.getLevel();
            int newLevel = getLevelFromXp(newXp);

            if (newLevel > oldLevel) {
                int spGained = 0;
                // Calculate SP for every level crossed
                for (int l = oldLevel + 1; l <= newLevel; l++) {
                    spGained += skillPointsForLevel(l);
                }

                pd.setLevel(newLevel);
                if (spGained > 0) {
                    pd.addSkillPoints(spGained);
                }

                // Fire event on main thread
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

    public String getLevelColorName(int level) {
        if (level < 20) return "&7Gray";
        if (level < 40) return "&fWhite";
        if (level < 60) return "&aGreen";
        if (level < 80) return "&eYellow";
        if (level < 100) return "&6Gold";
        if (level < 120) return "&cRed";
        return "&4Dark Red";
    }

    public String getLevelHexColor(int level) {
        return getLevelColor(level).asHexString();
    }

    public long xpToNextLevel(int currentLevel) {
        return xpNext(currentLevel);
    }

    public long xpRemainingForNextLevel(long currentXp, int currentLevel) {
        long nextLevelXpRequirement = getCumulativeForLevel(currentLevel + 1);
        return Math.max(0L, nextLevelXpRequirement - currentXp);
    }
}