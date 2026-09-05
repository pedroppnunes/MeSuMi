package robbery.core;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.number.NumberFormatter;

import java.math.BigDecimal;
import java.util.*;

public class HideoutAdminCommand implements CommandExecutor {

    private final Robbery plugin;

    public HideoutAdminCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String cmd = label.toLowerCase();

        // 1. Reset Hideout Worth Command
        if (cmd.equals("resethideoutworth") || cmd.equals("resethideout") || cmd.equals("resetworth")) {
            return handleResetWorth(sender, args);
        }

        // 2. Disqualify Player Command
        if (cmd.equals("dqplayer") || cmd.equals("disqualifyplayer")) {
            return handleDQPlayer(sender, args, true);
        }

        // 3. Undisqualify Player Command
        if (cmd.equals("undqplayer") || cmd.equals("undisqualifyplayer")) {
            return handleDQPlayer(sender, args, false);
        }

        // 4. Disqualify Hideout Command
        if (cmd.equals("dqhideout") || cmd.equals("disqualifyhideout")) {
            return handleDQHideout(sender, args, true);
        }

        // 5. Undisqualify Hideout Command
        if (cmd.equals("undqhideout") || cmd.equals("undisqualifyhideout")) {
            return handleDQHideout(sender, args, false);
        }

        // 6. List Disqualifications Command
        if (cmd.equals("dqlist") || cmd.equals("disqualified")) {
            return handleDQList(sender);
        }

        return true;
    }

    /* ---------------- Reset Worth Handlers ---------------- */

    private boolean handleResetWorth(CommandSender sender, String[] args) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            sender.sendMessage(ChatColor.RED + "SuperiorSkyblock2 is not enabled on this server.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /resethideoutworth <all|island_name|player_name>");
            return true;
        }

        String target = args[0];

        if (target.equalsIgnoreCase("all")) {
            sender.sendMessage(ChatColor.YELLOW + "Resetting week hideout worth for ALL Hideouts...");
            int count = plugin.getWeeklyLeaderboardTask().resetAllHideoutValues();
            sender.sendMessage(ChatColor.GREEN + "★ Successfully reset hideout worth for " + count + " Hideout(s)!");
            return true;
        }

        // Reset single island or player's island
        Island targetIsland = findIsland(target);
        if (targetIsland == null) {
            sender.sendMessage(ChatColor.RED + "Could not find a Hideout or player named '" + target + "'.");
            return true;
        }

        try {
            targetIsland.setBonusWorth(BigDecimal.ZERO);
            targetIsland.setBonusLevel(BigDecimal.ZERO);
            String islandName = targetIsland.getName() != null ? targetIsland.getName() : targetIsland.getOwner().getName();
            sender.sendMessage(ChatColor.GREEN + "★ Successfully reset weekly hideout worth for Hideout '" + islandName + "'.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reset hideout worth: " + e.getMessage());
        }

        return true;
    }

    /* ---------------- Player DQ Handlers ---------------- */

    private boolean handleDQPlayer(CommandSender sender, String[] args, boolean disqualify) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + (disqualify ? "dqplayer" : "undqplayer") + " <player_name>");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = off.getUniqueId();
        String name = off.getName() != null ? off.getName() : targetName;

        DisqualificationManager dqManager = plugin.getDisqualificationManager();

        if (disqualify) {
            dqManager.disqualifyPlayer(uuid, name);
            sender.sendMessage(ChatColor.GREEN + "★ Player '" + name + "' has been DISQUALIFIED from contributing hideout value.");
        } else {
            boolean removed = dqManager.undisqualifyPlayer(uuid);
            if (removed) {
                sender.sendMessage(ChatColor.GREEN + "★ Player '" + name + "' is no longer disqualified.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Player '" + name + "' was not disqualified.");
            }
        }

        return true;
    }

    /* ---------------- Hideout DQ Handlers ---------------- */

    private boolean handleDQHideout(CommandSender sender, String[] args, boolean disqualify) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            sender.sendMessage(ChatColor.RED + "SuperiorSkyblock2 is not enabled on this server.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + (disqualify ? "dqhideout" : "undqhideout") + " <island_name|player_name>");
            return true;
        }

        String target = args[0];
        Island targetIsland = findIsland(target);
        if (targetIsland == null) {
            sender.sendMessage(ChatColor.RED + "Could not find a Hideout or player named '" + target + "'.");
            return true;
        }

        UUID islandUuid = targetIsland.getUniqueId();
        String islandName = targetIsland.getName() != null ? targetIsland.getName() : (targetIsland.getOwner() != null ? targetIsland.getOwner().getName() : target);

        DisqualificationManager dqManager = plugin.getDisqualificationManager();

        if (disqualify) {
            dqManager.disqualifyIsland(islandUuid, islandName);
            sender.sendMessage(ChatColor.GREEN + "★ Hideout '" + islandName + "' has been DISQUALIFIED from receiving hideout value.");
        } else {
            boolean removed = dqManager.undisqualifyIsland(islandUuid);
            if (removed) {
                sender.sendMessage(ChatColor.GREEN + "★ Hideout '" + islandName + "' is no longer disqualified.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Hideout '" + islandName + "' was not disqualified.");
            }
        }

        return true;
    }

    /* ---------------- List DQ Handler ---------------- */

    private boolean handleDQList(CommandSender sender) {
        DisqualificationManager dqManager = plugin.getDisqualificationManager();
        Map<UUID, String> players = dqManager.getDisqualifiedPlayers();
        Map<UUID, String> islands = dqManager.getDisqualifiedIslands();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "       &c&lDisqualified Players & Hideouts"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));

        sender.sendMessage(ChatColor.YELLOW + "Disqualified Players (" + players.size() + "):");
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  None");
        } else {
            for (String pName : players.values()) {
                sender.sendMessage(ChatColor.RED + "  - " + pName);
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Disqualified Hideouts (" + islands.size() + "):");
        if (islands.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  None");
        } else {
            for (String iName : islands.values()) {
                sender.sendMessage(ChatColor.RED + "  - " + iName);
            }
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m----------------------------------------"));
        return true;
    }

    /* ---------------- Utility: Find Island by Name or Player ---------------- */

    private Island findIsland(String input) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) return null;

        // 1. Try finding player's island first
        OfflinePlayer offP = Bukkit.getOfflinePlayer(input);
        if (offP != null) {
            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(offP.getUniqueId());
            if (sp != null && sp.getIsland() != null) {
                return sp.getIsland();
            }
        }

        // 2. Try matching island by exact or partial name
        for (Island island : SuperiorSkyblockAPI.getGrid().getIslands()) {
            if (island.getName() != null && island.getName().equalsIgnoreCase(input)) {
                return island;
            }
        }

        // 3. Try matching island by owner name
        for (Island island : SuperiorSkyblockAPI.getGrid().getIslands()) {
            if (island.getOwner() != null && island.getOwner().getName() != null && island.getOwner().getName().equalsIgnoreCase(input)) {
                return island;
            }
        }

        return null;
    }
}
