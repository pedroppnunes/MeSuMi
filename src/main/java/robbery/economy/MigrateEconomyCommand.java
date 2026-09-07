package robbery.economy;

import net.milkbowl.vault.economy.Economy;
import org.MSM.mesumiEconomy.economy.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;

import java.util.UUID;

public class MigrateEconomyCommand implements CommandExecutor {

    private final Robbery plugin;

    public MigrateEconomyCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        MoneyManager moneyManager = Robbery.getMoneyManager();
        if (moneyManager == null) {
            sender.sendMessage(ChatColor.RED + "MesumiEconomy is not hooked or enabled!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Starting economy migration to MesumiEconomy...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int count = 0;

            // Try to find Vault provider to migrate balances from Vault if active
            Economy vaultEco = null;
            try {
                RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    vaultEco = rsp.getProvider();
                }
            } catch (Throwable ignored) {}

            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() == null) continue;
                UUID uuid = op.getUniqueId();
                String name = op.getName();

                double balance = 0.0;
                if (vaultEco != null && vaultEco.hasAccount(op)) {
                    balance = vaultEco.getBalance(op);
                }

                try {
                    moneyManager.createAccount(uuid, name);
                    if (balance > 0.0) {
                        moneyManager.setMoney(uuid, balance);
                    }
                    count++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed migrating economy balance for player " + name + " (" + uuid + "): " + e.getMessage());
                }
            }

            try {
                moneyManager.save(true);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save MesumiEconomy money manager after migration: " + e.getMessage());
            }

            final int finalCount = count;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "Economy migration complete! Migrated " + finalCount + " player accounts to MesumiEconomy.");
            });
        });

        return true;
    }
}
