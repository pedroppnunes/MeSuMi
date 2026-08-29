package robbery.database;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;

import java.io.File;
import java.util.UUID;

public class MigrateToSQLCommand implements CommandExecutor {

    private final Robbery plugin;

    public MigrateToSQLCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting migration of player data to SQL...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File playersFolder = new File(plugin.getDataFolder(), "player");
            if (!playersFolder.exists() || !playersFolder.isDirectory()) {
                sender.sendMessage(ChatColor.RED + "No player data folder found.");
                return;
            }

            File[] playerFolders = playersFolder.listFiles();
            if (playerFolders == null) {
                sender.sendMessage(ChatColor.RED + "Player data folder is empty.");
                return;
            }

            int count = 0;
            for (File folder : playerFolders) {
                if (folder.isDirectory()) {
                    File generalFile = new File(folder, "general.yml");
                    if (generalFile.exists()) {
                        try {
                            UUID uuid = UUID.fromString(folder.getName());
                            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(generalFile);
                            
                            int prestige = cfg.getInt("stats.prestige", 0);
                            String rank = cfg.getString("stats.rank", "NONE");
                            int skillpoints = cfg.getInt("stats.skillpoints", 0);
                            int itemsStolen = cfg.getInt("stats.itemsStolen", 0);
                            
                            plugin.getPlayerDataDao().savePlayerData(uuid, "Unknown", prestige, rank, skillpoints, itemsStolen, cfg);
                            count++;
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to migrate data for " + folder.getName());
                            e.printStackTrace();
                        }
                    }
                }
            }

            int finalCount = count;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Migration complete! " + finalCount + " players migrated to SQL.");
            });
        });

        return true;
    }
}
