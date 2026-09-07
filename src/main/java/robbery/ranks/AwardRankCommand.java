package robbery.ranks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.UUID;

/**
 * Command allowing Console or Admins to award a rank to a player (/awardrank <player> <rank>).
 * Works seamlessly for both online and offline players.
 */
public class AwardRankCommand implements CommandExecutor {

    private final Robbery plugin;

    public AwardRankCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /awardrank <player> <rank>");
            return true;
        }

        String targetName = args[0];
        String rawRankKey = args[1];
        String rankKey = RankManager.normalizeRankKey(rawRankKey);

        if (rankKey.equals("rank0")) {
            sender.sendMessage("§cInvalid rank: " + rawRankKey);
            return true;
        }

        String rankDisplayName = RankManager.getDisplayName(rankKey);
        Player onlineTarget = Bukkit.getPlayerExact(targetName);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            RankManager.awardRank(onlineTarget, rankKey);
            sender.sendMessage("§aAwarded §e" + rankDisplayName + " §arank to online player §f" + onlineTarget.getName() + "§a.");
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();
            YamlConfiguration cfg = plugin.getPlayerDataDao().loadPlayerData(uuid);

            if (cfg == null) {
                sender.sendMessage("§cPlayer §f" + targetName + " §cwas not found in the database.");
                return true;
            }

            String currentRank = cfg.getString("stats.rank", "rank0");
            int currentOrder = RankManager.getRankOrder(currentRank);
            int newOrder = RankManager.getRankOrder(rankKey);

            if (newOrder > currentOrder) {
                cfg.set("stats.rank", rankKey);

                String lpGroup = RankUpdate.rankMap.get(rankKey);
                if (lpGroup != null) {
                    for (String group : RankUpdate.rankMap.values()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetName + " parent remove " + group);
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetName + " parent add " + lpGroup);
                }
            } else {
                int currentVirt = cfg.getInt("stats.virtualRanks." + rankKey, 0);
                cfg.set("stats.virtualRanks." + rankKey, currentVirt + 1);
            }

            plugin.getPlayerDataDao().savePlayerData(
                    uuid,
                    targetName,
                    cfg.getInt("stats.prestige", 0),
                    cfg.getString("stats.rank", "rank0"),
                    cfg.getInt("stats.skillpoints", 0),
                    cfg.getInt("stats.itemsStolen", 0),
                    cfg
            );

            sender.sendMessage("§aAwarded §e" + rankDisplayName + " §arank to offline player §f" + targetName + "§a.");
        }

        return true;
    }
}
