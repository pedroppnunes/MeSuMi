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
import org.jetbrains.annotations.NotNull;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class MigrateHideoutWorthCommand implements CommandExecutor {

    private final Robbery plugin;

    public MigrateHideoutWorthCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            sender.sendMessage(ChatColor.RED + "SuperiorSkyblock2 plugin is not active on this server.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting Hideout Worth migration for all player contributed values...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, Double> contribMap = new HashMap<>();

            // 1. Gather online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData pd = PlayerDataManager.getPlayerData(p);
                if (pd != null && pd.getHideoutValueContributed() > 0) {
                    contribMap.put(p.getUniqueId(), pd.getHideoutValueContributed());
                }
            }

            // 2. Gather offline players from player data folder
            File playersFolder = new File(plugin.getDataFolder(), "player");
            if (playersFolder.exists() && playersFolder.isDirectory()) {
                File[] playerFolders = playersFolder.listFiles();
                if (playerFolders != null) {
                    for (File folder : playerFolders) {
                        if (folder.isDirectory()) {
                            try {
                                UUID uuid = UUID.fromString(folder.getName());
                                if (contribMap.containsKey(uuid)) continue; // already loaded from online player

                                File generalFile = new File(folder, "general.yml");
                                if (generalFile.exists()) {
                                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
                                    double contrib = cfg.getDouble("stats.hideoutValueContributed", 0.0);
                                    if (contrib > 0) {
                                        contribMap.put(uuid, contrib);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            if (contribMap.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "No player contributed hideout values found to migrate.");
                });
                return;
            }

            // 3. Apply contributions to SuperiorSkyblock2 islands on the main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int playersMigrated = 0;
                Set<UUID> updatedIslands = new HashSet<>();
                double totalWorthAdded = 0.0;

                for (Map.Entry<UUID, Double> entry : contribMap.entrySet()) {
                    UUID uuid = entry.getKey();
                    double contrib = entry.getValue();

                    try {
                        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(uuid);
                        if (sp != null && sp.getIsland() != null) {
                            Island hideout = sp.getIsland();
                            BigDecimal valBD = BigDecimal.valueOf(contrib);

                            try {
                                hideout.setBonusWorth(hideout.getBonusWorth().add(valBD));
                            } catch (Throwable ignored) {}

                            try {
                                hideout.setBonusLevel(hideout.getBonusLevel().add(valBD));
                            } catch (Throwable ignored) {}

                            updatedIslands.add(hideout.getUniqueId());
                            playersMigrated++;
                            totalWorthAdded += contrib;
                        }
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to migrate hideout worth for UUID " + uuid + ": " + t.getMessage());
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "★ Migration Complete!");
                sender.sendMessage(ChatColor.GREEN + "Migrated " + playersMigrated + " player contributions across " + updatedIslands.size() + " Hideout(s).");
                sender.sendMessage(ChatColor.GREEN + "Total Hideout Worth added: $" + NumberFormatter.formatDoubleNumber(totalWorthAdded));
            });
        });

        return true;
    }
}
