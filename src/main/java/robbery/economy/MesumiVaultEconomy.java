package robbery.economy;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.core.Robbery;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MesumiVaultEconomy extends AbstractEconomy {

    private final Robbery plugin;
    private final File moneyFile;
    private YamlConfiguration moneyConfig;

    public MesumiVaultEconomy(Robbery plugin) {
        this.plugin = plugin;
        File mesumiFolder = new File(plugin.getDataFolder().getParentFile(), "MesumiEconomy");
        if (!mesumiFolder.exists()) {
            mesumiFolder.mkdirs();
        }
        this.moneyFile = new File(mesumiFolder, "money.yml");
        loadMoneyConfig();
    }

    private void loadMoneyConfig() {
        if (!moneyFile.exists()) {
            try { moneyFile.createNewFile(); } catch (IOException ignored) {}
        }
        this.moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
    }

    private synchronized void saveMoneyConfig() {
        try {
            moneyConfig.save(moneyFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save money.yml for MesumiEconomy: " + e.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "MesumiEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        if (amount < 1000) {
            return String.format("$%.2f", amount);
        }
        return "$" + robbery.number.NumberFormatter.formatDoubleNumber(amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "Dollar";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalance(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) return 0.0;
        loadMoneyConfig();
        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        String[] possibleKeys = new String[] {
            uuid,
            "accounts." + uuid,
            "data." + uuid,
            "players." + uuid,
            name != null ? name : "",
            name != null ? "accounts." + name : "",
            name != null ? "data." + name : "",
            name != null ? "players." + name : ""
        };

        for (String k : possibleKeys) {
            if (!k.isEmpty() && moneyConfig.contains(k)) {
                return moneyConfig.getDouble(k);
            }
        }
        return 500.0; // Default starting balance
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        double current = getBalance(player);
        if (current < amount) {
            return new EconomyResponse(0, current, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        double newBal = current - amount;
        setBalance(player, newBal);
        return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        double current = getBalance(player);
        double newBal = current + amount;
        setBalance(player, newBal);
        return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    private synchronized void setBalance(OfflinePlayer player, double newBal) {
        if (player == null || player.getUniqueId() == null) return;
        loadMoneyConfig();
        String uuid = player.getUniqueId().toString();
        String name = player.getName();

        String targetKey = uuid;
        String[] checkKeys = new String[] {
            uuid,
            "accounts." + uuid,
            "data." + uuid,
            "players." + uuid,
            name != null ? name : "",
            name != null ? "accounts." + name : "",
            name != null ? "data." + name : "",
            name != null ? "players." + name : ""
        };

        for (String k : checkKeys) {
            if (!k.isEmpty() && moneyConfig.contains(k)) {
                targetKey = k;
                break;
            }
        }

        moneyConfig.set(targetKey, newBal);
        saveMoneyConfig();
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }
}
