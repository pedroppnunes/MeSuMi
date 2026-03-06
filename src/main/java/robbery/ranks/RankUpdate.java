package robbery.ranks;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /rankupdate command, which allows administrators to update
 * a player's rank directly.
 * <p>
 * This command updates both the player's stored rank in PlayerData and
 * the player's permissions using LuckPerms commands. All previous rank
 * groups are removed before adding the new rank.
 * </p>
 * <p>
 * Usage: /rankupdate &lt;rank&gt; &lt;player&gt;<br>
 * Example: /rankupdate rank3 Steve
 * </p>
 */
public class RankUpdate implements CommandExecutor {

    /** Mapping of rank keys to corresponding LuckPerms permission groups. */
    public static final Map<String, String> rankMap = new HashMap<>();

    static {
        rankMap.put("rank1", "robberyburglar");
        rankMap.put("rank2", "robberyrobber");
        rankMap.put("rank3", "robberybandit");
        rankMap.put("rank4", "robberyoutlaw");
        rankMap.put("rank5", "robberyheister");
        rankMap.put("rank6", "robberykingpin");
        rankMap.put("rank7", "robberymafiaboss");
    }

    /**
     * Constructs the RankUpdate command.
     *
     * @param main the instance of the Robbery plugin
     */
    public RankUpdate(Robbery main) {
    }

    /**
     * Executes the /rankupdate command.
     *
     * @param sender the sender of the command
     * @param command the command object
     * @param label the command label
     * @param args command arguments, expecting [rank, playerName]
     * @return true if the command executed (even if failed validation)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 2) {
            Messages.send(sender, "command.rankupdate.usage");
            return true;
        }

        String newRankKey = args[0].toLowerCase();
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        if (!rankMap.containsKey(newRankKey)) {
            Messages.send(sender, "command.rankupdate.invalid-rank");
            return true;
        }

        String newRankPermission = rankMap.get(newRankKey);

        // Update player data
        PlayerData data = PlayerDataManager.getPlayerData(target);
        data.setRank(newRankKey);

        // Remove old rank groups
        for (String group : rankMap.values()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent remove " + group);
        }

        // Add new rank group
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " parent add " + newRankPermission);

        Messages.sendFormatted(sender, "command.rankupdate.success", Map.of("player", target.getName(), "rank", newRankKey));

        return true;
    }
}
