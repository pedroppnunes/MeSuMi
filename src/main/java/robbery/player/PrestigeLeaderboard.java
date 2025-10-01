package robbery.player;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Manages a leaderboard tracking player prestige levels.
 * <p>
 * Provides methods to update a player's prestige in the leaderboard
 * and retrieve the top prestige players and their prestige values.
 */
public class PrestigeLeaderboard {

    /** TreeMap storing player names as keys and their prestige as values. */
    private static final Map<String, Integer> leaderboard = new TreeMap<>();

    /**
     * Updates the leaderboard with the given player's current prestige.
     * If the player is not already in the leaderboard, they are added.
     *
     * @param player the player whose prestige should be updated
     */
    public static void updateLeaderboard(Player player) {
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        leaderboard.put(player.getName(), playerData.getPrestige());
    }

    /**
     * Retrieves the name of the player at the given position in the leaderboard.
     * The leaderboard is sorted in descending order of prestige.
     *
     * @param position the 1-based position in the leaderboard
     * @return the player's name at the given position, or "N/A" if the position is invalid
     */
    public static String getTopPrestigePlayer(int position) {
        List<String> sortedPlayers = leaderboard.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (position <= sortedPlayers.size()) {
            return sortedPlayers.get(position - 1);
        }
        return "N/A";
    }

    /**
     * Retrieves the prestige value of the player at the given position in the leaderboard.
     * The leaderboard is sorted in descending order of prestige.
     *
     * @param position the 1-based position in the leaderboard
     * @return the prestige value as a String, or "N/A" if the position is invalid
     */
    public static String getTopPrestige(int position) {
        List<Map.Entry<String, Integer>> sortedPlayers = leaderboard.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()))
                .toList();

        if (position <= sortedPlayers.size()) {
            return String.valueOf(sortedPlayers.get(position - 1).getValue());
        }
        return "N/A";
    }
}
