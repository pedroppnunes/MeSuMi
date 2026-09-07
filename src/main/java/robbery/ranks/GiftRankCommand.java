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
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;
import java.util.UUID;

/**
 * Command allowing players to view their stored virtual ranks (/giftrank list)
 * or gift a virtual rank to another player (/giftrank <player> <rank>).
 */
public class GiftRankCommand implements CommandExecutor {

    private final Robbery plugin;

    public GiftRankCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.only-players");
            return true;
        }

        PlayerData senderData = PlayerDataManager.getPlayerData(p);
        if (senderData == null) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Map<String, Integer> virtuals = senderData.getVirtualRanksMap();
            p.sendMessage("§8§m--------------------------------------------------");
            p.sendMessage("§a§lYOUR VIRTUAL RANKS:");
            if (virtuals.isEmpty()) {
                p.sendMessage("§7You have no virtual ranks stored in your account.");
            } else {
                for (Map.Entry<String, Integer> entry : virtuals.entrySet()) {
                    if (entry.getValue() > 0) {
                        String name = RankManager.getDisplayName(entry.getKey());
                        p.sendMessage("§7- §e" + name + " §7(" + entry.getKey() + "): §f" + entry.getValue() + "x");
                    }
                }
                p.sendMessage("§7Use §b/giftrank <player> <rank> §7to gift a rank to a friend!");
            }
            p.sendMessage("§8§m--------------------------------------------------");
            return true;
        }

        if (args.length < 2) {
            p.sendMessage("§cUsage: /giftrank <player> <rank> or /giftrank list");
            return true;
        }

        String targetName = args[0];
        String rawRankKey = args[1];
        String rankKey = RankManager.normalizeRankKey(rawRankKey);

        if (rankKey.equals("rank0")) {
            p.sendMessage("§cInvalid rank specified!");
            return true;
        }

        String rankDisplayName = RankManager.getDisplayName(rankKey);
        int available = senderData.getVirtualRankCount(rankKey);
        if (available <= 0) {
            p.sendMessage("§cYou do not have any virtual §e" + rankDisplayName + " §cranks to gift!");
            return true;
        }

        if (p.getName().equalsIgnoreCase(targetName)) {
            p.sendMessage("§cYou cannot gift a rank to yourself!");
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            senderData.removeVirtualRank(rankKey, 1);
            RankManager.awardRank(onlineTarget, rankKey);

            p.sendMessage("§aSuccessfully gifted 1x §e" + rankDisplayName + " §arank to §f" + onlineTarget.getName() + "§a!");
            onlineTarget.sendMessage("§a§lRANK GIFT RECEIVED! §f" + p.getName() + " §agifted you a §e" + rankDisplayName + " §arank!");
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = offlineTarget.getUniqueId();
            YamlConfiguration cfg = plugin.getPlayerDataDao().loadPlayerData(uuid);

            if (cfg == null) {
                p.sendMessage("§cPlayer §f" + targetName + " §chas never joined the server!");
                return true;
            }

            senderData.removeVirtualRank(rankKey, 1);
            String targetCurrentRank = cfg.getString("stats.rank", "rank0");
            int targetOrder = RankManager.getRankOrder(targetCurrentRank);
            int newOrder = RankManager.getRankOrder(rankKey);

            if (newOrder > targetOrder) {
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

            p.sendMessage("§aSuccessfully gifted 1x §e" + rankDisplayName + " §arank to §f" + targetName + " §7(Offline)§a!");
        }

        return true;
    }
}
