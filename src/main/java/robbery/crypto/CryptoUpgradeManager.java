package robbery.crypto;

import net.milkbowl.vault.economy.Economy;
import org.MSM.mesumiEconomy.economy.MoneyManager;
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

    private static int maxLevel = 10;
    private static int xpPerCredit = 1_000_000;
    private static final Map<Integer, Integer> creditCosts = new HashMap<>();
    private static final TreeMap<Integer, Integer> tierPrestige = new TreeMap<>();

    public static void loadConfig() {
        File file = new File(Robbery.getInstance().getDataFolder(), "crypto_config.yml");
        if (!file.exists()) {
            Robbery.getInstance().saveResource("crypto_config.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        maxLevel = cfg.getInt("settings.max_level", 10);
        xpPerCredit = cfg.getInt("settings.xp_per_credit", 1_000_000);

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

        ConfigurationSection creditSec = cfg.getConfigurationSection("credit_costs");
        if (creditSec != null) {
            creditCosts.clear();
            for (String key : creditSec.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    int val = creditSec.getInt(key);
                    creditCosts.put(lvl, val);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static void reloadConfig() {
        loadConfig();
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    public static int getXpPerCredit() {
        return xpPerCredit;
    }

    public static int getRequiredPrestige(int targetLevel) {
        if (tierPrestige.containsKey(targetLevel)) {
            return tierPrestige.get(targetLevel);
        }
        if (targetLevel >= 10) return 5;
        if (targetLevel >= 9) return 4;
        if (targetLevel >= 7) return 3;
        if (targetLevel >= 5) return 2;
        if (targetLevel >= 3) return 1;
        return 0;
    }

    public static int getCreditCost(int targetLevel) {
        if (creditCosts.containsKey(targetLevel)) {
            return creditCosts.get(targetLevel);
        }
        return switch (targetLevel) {
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 5;
            case 6 -> 7;
            case 7 -> 10;
            case 8 -> 13;
            case 9 -> 16;
            case 10 -> 20;
            default -> 1;
        };
    }

    public static long getUpgradeCost(int currentLevel) {
        if (currentLevel >= getMaxLevel()) return -1L;
        return getCreditCost(currentLevel + 1);
    }

    public static String getTrackDisplayName(String track) {
        if (track == null) return "Upgrade";
        return switch (track.toLowerCase().trim()) {
            case "speed", "interval" -> "Steal Speed";
            case "capacity", "batch" -> "Batch Capacity";
            case "duration", "time", "battery" -> "Battery Duration";
            case "reward", "money" -> "Money Reward";
            default -> "Upgrade";
        };
    }

    public static boolean upgradeTrack(Player player, CryptoMachine machine, String track) {
        if (player == null || machine == null || track == null) return false;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return false;

        String normTrack = track.toLowerCase().trim();
        int currentLevel = switch (normTrack) {
            case "speed", "interval" -> machine.getSpeedLevel();
            case "capacity", "batch" -> machine.getCapacityLevel();
            case "duration", "time", "battery" -> machine.getFuelTimeLevel();
            case "reward", "money" -> machine.getRewardLevel();
            default -> -1;
        };

        if (currentLevel < 0) return false;

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
                    "track", getTrackDisplayName(normTrack)
            ));
            return false;
        }

        int creditCost = getCreditCost(targetLevel);
        if (pd.getCryptoCredits() < creditCost) {
            player.sendMessage(Messages.colorize("&cYou need &6&l⛁ " + creditCost + " Crypto Credits &cto upgrade " + getTrackDisplayName(normTrack) + " to Level " + targetLevel + "! (You have: &6⛁ " + pd.getCryptoCredits() + "&c)"));
            return false;
        }

        pd.removeCryptoCredits(creditCost);

        switch (normTrack) {
            case "speed", "interval" -> machine.setSpeedLevel(targetLevel);
            case "capacity", "batch" -> machine.setCapacityLevel(targetLevel);
            case "duration", "time", "battery" -> machine.setFuelTimeLevel(targetLevel);
            case "reward", "money" -> machine.setRewardLevel(targetLevel);
        }

        Robbery.getInstance().getCryptoManager().saveMachine(machine);

        player.sendMessage(Messages.colorize("&aSuccessfully upgraded &e" + getTrackDisplayName(normTrack) + " &ato &bLevel " + targetLevel + "&a for &6&l⛁ " + creditCost + " Crypto Credits&a!"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        return true;
    }

    public static boolean upgradeMachine(Player player, CryptoMachine machine) {
        return upgradeTrack(player, machine, "speed");
    }
}
