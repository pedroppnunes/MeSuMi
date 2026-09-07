package robbery.ranks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.List;
import java.util.Map;

/**
 * Manages the different ranks available in the robbery system.
 * <p>
 * Handles rank normalization, comparison, instant rank awarding,
 * duplicate conversion to virtual ranks, and GUI item rendering.
 */
public class RankManager {

    /** The highest rank available. */
    public static final Rank MAFIA_BOSS = new Rank("Mafia Boss", 1.5, 8, 0.4, 0.25, "rank7");

    /** Second-highest rank. */
    public static final Rank KINGPIN = new Rank("Kingpin", 1.20, 7, 0.3, 0.20, "rank6");

    /** High-level rank for experienced players. */
    public static final Rank HEISTER = new Rank("Heister", 1, 6, 0.25, 0.15, "rank5");

    /** Mid-level rank representing an outlaw. */
    public static final Rank OUTLAW = new Rank("Outlaw", 0.8, 5, 0.20, 0.10, "rank4");

    /** Mid-low rank representing a bandit. */
    public static final Rank BANDIT = new Rank("Bandit", 0.65, 4, 0.175, 0.075, "rank3");

    /** Low-level rank representing a basic robber. */
    public static final Rank ROBBER = new Rank("Robber", 0.5, 3, 0.15, 0.05, "rank2");

    /** Entry-level rank representing a burglar. */
    public static final Rank BURGLAR = new Rank("Burglar", 0.25, 2, 0.1, 0.025, "rank1");

    /** Default empty rank for players without a rank. */
    public static final Rank NONE = new Rank("Member", 0.0, 0, 0, 0, "rank0");

    public static final Map<String, String> DISPLAY_NAMES = Map.of(
            "rank1", "Burglar",
            "rank2", "Robber",
            "rank3", "Bandit",
            "rank4", "Outlaw",
            "rank5", "Heister",
            "rank6", "Kingpin",
            "rank7", "Mafia Boss"
    );

    /**
     * Returns the {@link Rank} corresponding to the given identifier string.
     *
     * @param rank the rank identifier (e.g., "rank0", "rank1", ..., "rank7" or name)
     * @return the corresponding {@link Rank} object, or {@link #NONE} if the identifier is invalid
     */
    public static Rank getRank(String rank) {
        if (rank == null) return NONE;
        return switch (rank.toLowerCase().trim()) {
            case "rank0", "none", "" -> NONE;
            case "rank1", "burglar" -> BURGLAR;
            case "rank2", "robber" -> ROBBER;
            case "rank3", "bandit" -> BANDIT;
            case "rank4", "outlaw" -> OUTLAW;
            case "rank5", "heister" -> HEISTER;
            case "rank6", "kingpin" -> KINGPIN;
            case "rank7", "mafiaboss", "mafia_boss", "mafia boss" -> MAFIA_BOSS;
            default -> NONE;
        };
    }

    public static String normalizeRankKey(String input) {
        if (input == null) return "rank0";
        String s = input.toLowerCase().trim();
        return switch (s) {
            case "rank1", "burglar" -> "rank1";
            case "rank2", "robber" -> "rank2";
            case "rank3", "bandit" -> "rank3";
            case "rank4", "outlaw" -> "rank4";
            case "rank5", "heister" -> "rank5";
            case "rank6", "kingpin" -> "rank6";
            case "rank7", "mafiaboss", "mafia_boss", "mafia boss" -> "rank7";
            default -> s.startsWith("rank") ? s : "rank0";
        };
    }

    public static int getRankOrder(String rankKey) {
        if (rankKey == null) return 0;
        String k = normalizeRankKey(rankKey);
        try {
            if (k.startsWith("rank")) {
                return Integer.parseInt(k.replace("rank", ""));
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    public static boolean isBetterOrEqual(String rankA, String rankB) {
        return getRankOrder(rankA) >= getRankOrder(rankB);
    }

    public static boolean isBetter(String rankA, String rankB) {
        return getRankOrder(rankA) > getRankOrder(rankB);
    }

    public static String getDisplayName(String rankKey) {
        String normalized = normalizeRankKey(rankKey);
        return DISPLAY_NAMES.getOrDefault(normalized, getRank(normalized).name());
    }

    /**
     * Awards a rank to an online player.
     * <p>
     * If the target player does NOT have this rank yet (or has a lower rank),
     * it instantly sets their rank and updates permissions.
     * If the target player ALREADY has equal or higher rank,
     * it converts the awarded rank into a Virtual Rank stored in their account.
     */
    public static void awardRank(Player target, String rawRankKey) {
        if (target == null || !target.isOnline()) return;
        String rankKey = normalizeRankKey(rawRankKey);
        if (rankKey.equals("rank0")) return;

        PlayerData data = PlayerDataManager.getPlayerData(target);
        if (data == null) return;

        String currentRankKey = normalizeRankKey(data.getRank());
        int currentOrder = getRankOrder(currentRankKey);
        int newOrder = getRankOrder(rankKey);
        String rankName = getDisplayName(rankKey);

        if (newOrder > currentOrder) {
            data.setRank(rankKey);

            String lpGroup = RankUpdate.rankMap.get(rankKey);
            if (lpGroup != null) {
                for (String group : RankUpdate.rankMap.values()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent remove " + group);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent add " + lpGroup);
            }

            target.playSound(target.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            target.sendMessage("§a§lRANK UNLOCKED! §fYou received Rank §e" + rankName + "§f!");
        } else {
            data.addVirtualRank(rankKey);
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            target.sendMessage("§a§lVIRTUAL RANK RECEIVED! §fYou already own Rank §e" + getDisplayName(currentRankKey) + "§f.");
            target.sendMessage("§f1x §e" + rankName + " §fhas been added to your virtual inventory. Use §b/giftrank §fto gift it to a player!");
        }
    }

    private static NamespacedKey getClaimKey() {
        return new NamespacedKey(Robbery.getInstance(), "rank_claim_reward");
    }

    public static ItemStack createDisplayItem(String rankKey, int amount) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nice = getDisplayName(rankKey);
            meta.getPersistentDataContainer().set(getClaimKey(), PersistentDataType.STRING, normalizeRankKey(rankKey));
            meta.setDisplayName("§6§lRank Reward: §e" + nice);
            meta.setLore(List.of(
                    "§7Amount: §ex" + amount,
                    "§7Click to claim or convert into a virtual rank!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getClaimedRankKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(getClaimKey(), PersistentDataType.STRING);
    }
}
