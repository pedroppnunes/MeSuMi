package robbery.ranks;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.Robbery;

import java.util.Map;

/**
 * Utility class for creating and handling rank vouchers in the form of paper items.
 * <p>
 * Each rank voucher represents a specific {@link Rank} and stores the rank key
 * using a persistent data container, allowing safe identification across server restarts.
 */
public final class RankPaper {

    /** Persistent data key used to store the rank key inside the paper item. */
    private static final NamespacedKey KEY = new NamespacedKey(Robbery.getInstance(), "rank_reward");

    /** Mapping of rank keys to their display names for player-friendly text. */
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
     * Creates a rank voucher ItemStack for the given rank key.
     *
     * @param rankKey the internal rank key (e.g., "rank1", "rank2", etc.)
     * @return a PAPER ItemStack representing the rank voucher, with display name and persistent data set
     */
    public static ItemStack create(String rankKey) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        String nice = DISPLAY_NAMES.get(rankKey);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, rankKey);
        meta.setDisplayName("§6§lRank Voucher: §e" + nice);
        paper.setItemMeta(meta);
        return paper;
    }

    /**
     * Retrieves the stored rank key from a given ItemStack.
     *
     * @param i the ItemStack to check
     * @return the rank key if present, or {@code null} if the item is invalid or has no rank data
     */
    public static String getRankKey(ItemStack i) {
        if (i == null || i.getType() != Material.PAPER || !i.hasItemMeta()) return null;
        return i.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    /**
     * Returns the display name corresponding to a given rank key.
     *
     * @param rankToken the rank key
     * @return the player-friendly rank name, or the key itself if not found
     */
    public static String getDisplayName(String rankToken) {
        return DISPLAY_NAMES.getOrDefault(rankToken, rankToken);
    }
}
