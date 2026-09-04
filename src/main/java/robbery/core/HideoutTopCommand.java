package robbery.core;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import robbery.formatters.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HideoutTopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            sendHideoutTop(p);
        } else {
            sender.sendMessage("This command can only be used by players.");
        }
        return true;
    }

    public static void sendHideoutTop(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            player.sendMessage(ChatColor.RED + "Hideouts system is not active.");
            return;
        }

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
        if (sp == null || sp.getIsland() == null) {
            player.sendMessage(ChatColor.RED + "You do not have a Hideout!");
            return;
        }

        Island hideout = sp.getIsland();
        List<SuperiorPlayer> members = hideout.getIslandMembers(true);
        if (members == null || members.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No members found in your Hideout.");
            return;
        }

        Robbery main = JavaPlugin.getPlugin(Robbery.class);
        List<MemberContribution> list = new ArrayList<>();

        for (SuperiorPlayer member : members) {
            UUID uuid = member.getUniqueId();
            String name = member.getName() != null ? member.getName() : "Unknown";
            double contrib = 0.0;

            Player onlineP = Bukkit.getPlayer(uuid);
            if (onlineP != null) {
                PlayerData pd = PlayerDataManager.getPlayerData(onlineP);
                if (pd != null) {
                    contrib = pd.getHideoutValueContributed();
                }
            } else {
                if (main != null && main.getPlayerDataDao() != null) {
                    YamlConfiguration cfg = main.getPlayerDataDao().loadPlayerData(uuid);
                    if (cfg != null) {
                        contrib = cfg.getDouble("stats.hideoutValueContributed", 0.0);
                    }
                }
            }
            list.add(new MemberContribution(name, contrib));
        }

        list.sort((a, b) -> Double.compare(b.contribution, a.contribution));

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "       &e&lHideout Top Contributors"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));

        int rank = 1;
        for (MemberContribution mc : list) {
            String color = (rank == 1) ? "&a" : (rank == 2 ? "&e" : (rank == 3 ? "&6" : "&7"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    " &8" + rank + ". " + color + mc.name + " &7- &e" + NumberFormatter.formatDoubleNumber(mc.contribution) + " &aValue"));
            rank++;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
    }

    private static class MemberContribution {
        final String name;
        final double contribution;

        MemberContribution(String name, double contribution) {
            this.name = name;
            this.contribution = contribution;
        }
    }
}
