package robbery.crypto;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

public class CryptoUpgradeManager {

    public static final int MAX_LEVEL = 39;

    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    public static int getRequiredPrestige(int targetLevel) {
        if (targetLevel >= 30) return 3;
        if (targetLevel >= 20) return 2;
        if (targetLevel >= 10) return 1;
        return 0;
    }

    public static long getUpgradeCost(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return -1L; // Max level reached
        }

        int targetLevel = currentLevel + 1;

        // Big milestone requirements
        if (targetLevel == 10) return 125_000_000L;       // 125M (was 250M)
        if (targetLevel == 20) return 500_000_000L;       // 500M (was 750M)
        if (targetLevel == 30) return 1_000_000_000L;     // 1B (was 1.5B)

        // Tier 0 (0 to 9)
        if (targetLevel < 10) {
            return switch (targetLevel) {
                case 1 -> 250_000L;
                case 2 -> 500_000L;
                case 3 -> 750_000L;
                case 4 -> 1_000_000L;
                case 5 -> 1_500_000L;
                case 6 -> 2_000_000L;
                case 7 -> 3_000_000L;
                case 8 -> 4_000_000L;
                case 9 -> 5_000_000L;
                default -> 250_000L;
            };
        }

        // Tier 1 (11 to 19)
        if (targetLevel < 20) {
            return switch (targetLevel) {
                case 11 -> 10_000_000L;
                case 12 -> 12_000_000L;
                case 13 -> 15_000_000L;
                case 14 -> 18_000_000L;
                case 15 -> 20_000_000L;
                case 16 -> 25_000_000L;
                case 17 -> 30_000_000L;
                case 18 -> 35_000_000L;
                case 19 -> 45_000_000L;
                default -> 10_000_000L;
            };
        }

        // Tier 2 (21 to 29)
        if (targetLevel < 30) {
            return switch (targetLevel) {
                case 21 -> 20_000_000L;
                case 22 -> 25_000_000L;
                case 23 -> 30_000_000L;
                case 24 -> 35_000_000L;
                case 25 -> 40_000_000L;
                case 26 -> 50_000_000L;
                case 27 -> 60_000_000L;
                case 28 -> 70_000_000L;
                case 29 -> 80_000_000L;
                default -> 20_000_000L;
            };
        }

        // Tier 3 (31 to 39)
        return switch (targetLevel) {
            case 31 -> 40_000_000L;
            case 32 -> 50_000_000L;
            case 33 -> 60_000_000L;
            case 34 -> 70_000_000L;
            case 35 -> 80_000_000L;
            case 36 -> 100_000_000L;
            case 37 -> 110_000_000L;
            case 38 -> 120_000_000L;
            case 39 -> 130_000_000L;
            default -> 40_000_000L;
        };
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
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
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
        if (currentLevel >= MAX_LEVEL) {
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
