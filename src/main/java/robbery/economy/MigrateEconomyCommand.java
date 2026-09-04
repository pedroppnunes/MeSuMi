package robbery.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.core.Robbery;
import robbery.number.NumberFormatter;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MigrateEconomyCommand implements CommandExecutor {

    private final Robbery plugin;

    public MigrateEconomyCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        Economy econ = Robbery.getEconomy();
        if (econ == null) {
            sender.sendMessage(ChatColor.RED + "No Vault Economy provider found! Ensure Vault and MesumiEconomy are loaded.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting economy migration from Essentials userdata to MesumiEconomy...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File essFolder = new File(pluginsFolder, "Essentials/userdata");
            if (!essFolder.exists() || !essFolder.isDirectory()) {
                essFolder = new File(pluginsFolder, "EssentialsX/userdata");
            }

            if (!essFolder.exists() || !essFolder.isDirectory()) {
                sender.sendMessage(ChatColor.RED + "Essentials userdata folder not found at plugins/Essentials/userdata!");
                return;
            }

            File[] userFiles = essFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (userFiles == null || userFiles.length == 0) {
                sender.sendMessage(ChatColor.RED + "No Essentials userdata (.yml) files found to migrate.");
                return;
            }

            AtomicInteger migratedCount = new AtomicInteger(0);
            AtomicInteger skippedCount = new AtomicInteger(0);
            AtomicReference<Double> totalMigratedMoney = new AtomicReference<>(0.0);

            for (File userFile : userFiles) {
                String fileName = userFile.getName();
                String uuidStr = fileName.substring(0, fileName.lastIndexOf('.'));
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (Exception e) {
                    skippedCount.incrementAndGet();
                    continue;
                }

                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(userFile);
                if (!cfg.contains("money")) {
                    skippedCount.incrementAndGet();
                    continue;
                }

                double money = 0.0;
                if (cfg.isDouble("money") || cfg.isInt("money") || cfg.isLong("money")) {
                    money = cfg.getDouble("money");
                } else if (cfg.isString("money")) {
                    try {
                        money = Double.parseDouble(cfg.getString("money"));
                    } catch (Exception ignored) {}
                }

                if (money <= 0.0) {
                    skippedCount.incrementAndGet();
                    continue;
                }

                double finalMoney = money;
                OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(uuid);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        double currentBal = econ.getBalance(offPlayer);
                        if (currentBal > 0) {
                            econ.withdrawPlayer(offPlayer, currentBal);
                        }
                        econ.depositPlayer(offPlayer, finalMoney);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed migrating economy balance for " + offPlayer.getName() + " (" + uuid + "): " + ex.getMessage());
                    }
                });

                migratedCount.incrementAndGet();
                totalMigratedMoney.updateAndGet(v -> v + finalMoney);
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Economy Migration Completed!");
                sender.sendMessage(ChatColor.GRAY + "Players Migrated: " + ChatColor.YELLOW + migratedCount.get());
                sender.sendMessage(ChatColor.GRAY + "Skipped (0 bal / invalid): " + ChatColor.YELLOW + skippedCount.get());
                sender.sendMessage(ChatColor.GRAY + "Total Currency Transferred: " + ChatColor.GOLD + "$" + NumberFormatter.formatDoubleNumber(totalMigratedMoney.get()));
                sender.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
            });
        });

        return true;
    }
}
