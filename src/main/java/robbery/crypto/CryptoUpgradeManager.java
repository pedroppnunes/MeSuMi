package robbery.crypto;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CryptoUpgradeManager {

    private static int maxLevel = 50;
    private static final Map<Integer, Long> upgradeCosts = new HashMap<>();
    private static final TreeMap<Integer, Integer> tierPrestige = new TreeMap<>();
    private static final Map<Integer, Long> storeRates = new HashMap<>();

    public static void loadConfig() {
        File file = new File(Robbery.getInstance().getDataFolder(), "crypto_config.yml");
        if (!file.exists()) {
            Robbery.getInstance().saveResource("crypto_config.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        if (cfg.contains("settings.max_level")) {
            maxLevel = cfg.getInt("settings.max_level", 50);
        } else {
            maxLevel = cfg.getInt("max_level", 50);
        }

        ConfigurationSection storeSec = cfg.getConfigurationSection("store_rates");
        if (storeSec != null) {
            storeRates.clear();
            for (String key : storeSec.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(key);
                    long val = storeSec.getLong(key);
                    storeRates.put(tier, val);
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection presSec = cfg.getConfigurationSection("prestige_requirements");
        if (presSec != null) {
            tierPrestige.clear();
            for (String key : presSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    int reqP = presSec.getInt(key);
                    tierPrestige.put(lvl, reqP);
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection secs = cfg.getConfigurationSection("costs");
        if (secs != null) {
            upgradeCosts.clear();
            for (String key : secs.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    long val = secs.getLong(key);
                    upgradeCosts.put(lvl, val);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static long getStoreBaseRate(int storeTier) {
        int clampedTier = Math.min(12, Math.max(1, storeTier));
        Long val = storeRates.get(clampedTier);
        if (val != null) {
            return val;
        }
        return switch (clampedTier) {
            case 1 -> 2L;
            case 2 -> 4L;
            case 3 -> 15L;
            case 4 -> 80L;
            case 5 -> 8L;
            case 6 -> 55L;
            case 7 -> 220L;
            case 8 -> 820L;
            case 9 -> 1350L;
            case 10 -> 2200L;
            case 11 -> 4650L;
            case 12 -> 6800L;
            default -> 2L;
        };
    }

    public static void reloadConfig() {
        loadConfig();
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    public static int getRequiredPrestige(int targetLevel) {
        if (tierPrestige.isEmpty()) {
            if (targetLevel >= 50) return 5;
            if (targetLevel >= 40) return 4;
            if (targetLevel >= 30) return 3;
            if (targetLevel >= 20) return 2;
            if (targetLevel >= 10) return 1;
            return 0;
        }

        Map.Entry<Integer, Integer> entry = tierPrestige.floorEntry(targetLevel);
        if (entry == null) {
            return 0;
        }
        return entry.getValue();
    }

    public static long getUpgradeCost(int currentLevel) {
        if (currentLevel >= getMaxLevel()) {
            return -1L; // Max level reached
        }

        int targetLevel = currentLevel + 1;
        // Try config-based cost first
        Long configCost = upgradeCosts.get(targetLevel);
        if (configCost != null) {
            return configCost;
        }

        // Fallback calculation if targetLevel key isn't explicitly defined in costs section
        if (targetLevel == 10) return 125_000_000L;
        if (targetLevel == 20) return 500_000_000L;
        if (targetLevel == 30) return 1_000_000_000L;
        if (targetLevel == 40) return 2_000_000_000L;
        if (targetLevel == 50) return 5_000_000_000L;

        if (targetLevel < 10) {
            return targetLevel * 250_000L;
        }
        if (targetLevel < 20) {
            return (targetLevel - 10) * 5_000_000L + 10_000_000L;
        }
        if (targetLevel < 30) {
            return (targetLevel - 20) * 10_000_000L + 20_000_000L;
        }
        if (targetLevel < 40) {
            return (targetLevel - 30) * 15_000_000L + 40_000_000L;
        }
        return (targetLevel - 40) * 30_000_000L + 150_000_000L;
    }

    public static int getTrackLevel(CryptoMachine machine, String track) {
        if (machine == null) return 0;
        String t = track.toLowerCase();
        if (t.contains("speed")) return machine.getSpeedLevel();
        if (t.contains("battery") || t.contains("fuel") || t.contains("time") || t.contains("duration")) return machine.getFuelTimeLevel();
        if (t.contains("reward") || t.contains("money")) return machine.getRewardLevel();
        return 0;
    }

    public static void setTrackLevel(CryptoMachine machine, String track, int level) {
        if (machine == null) return;
        int clamped = Math.max(0, Math.min(getMaxLevel(), level));
        String t = track.toLowerCase();
        if (t.contains("speed")) machine.setSpeedLevel(clamped);
        else if (t.contains("battery") || t.contains("fuel") || t.contains("time") || t.contains("duration")) machine.setFuelTimeLevel(clamped);
        else if (t.contains("reward") || t.contains("money")) machine.setRewardLevel(clamped);
    }

    public static String getTrackDisplayName(String track) {
        String t = track.toLowerCase();
        if (t.contains("speed")) return "Speed";
        if (t.contains("battery") || t.contains("fuel") || t.contains("time") || t.contains("duration")) return "Battery Duration";
        if (t.contains("reward") || t.contains("money")) return "Money Reward";
        return track;
    }

    public static boolean upgradeTrack(Player player, CryptoMachine machine, String track) {
        if (player == null || machine == null) return false;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return false;

        int currentLevel = getTrackLevel(machine, track);
        if (currentLevel >= getMaxLevel()) {
            Messages.send(player, "crypto.upgrade-max-level");
            return false;
        }

        int targetLevel = currentLevel + 1;
        int reqPrestige = getRequiredPrestige(targetLevel);
        if (pd.getPrestige() < reqPrestige) {
            Messages.sendFormatted(player, "crypto.upgrade-req-prestige", Map.of(
                    "prestige", String.valueOf(reqPrestige),
                    "level", String.valueOf(targetLevel),
                    "track", getTrackDisplayName(track)
            ));
            return false;
        }

        long cost = getUpgradeCost(currentLevel);
        Economy eco = Robbery.getEconomy();
        if (eco == null || eco.getBalance(player) < cost) {
            String costFormatted = NumberFormatter.formatDoubleNumber((double) cost);
            Messages.sendFormatted(player, "crypto.upgrade-req-money", Map.of(
                    "price", costFormatted,
                    "track", getTrackDisplayName(track)
            ));
            return false;
        }

        eco.withdrawPlayer(player, cost);
        setTrackLevel(machine, track, targetLevel);
        Robbery.getInstance().getCryptoManager().saveMachine(machine);

        String trackName = getTrackDisplayName(track);
        String costFormatted = NumberFormatter.formatDoubleNumber((double) cost);
        Messages.sendFormatted(player, "crypto.upgrade-success", Map.of(
                "track", trackName,
                "level", String.valueOf(targetLevel),
                "cost", costFormatted,
                "price", costFormatted
        ));

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }
}
