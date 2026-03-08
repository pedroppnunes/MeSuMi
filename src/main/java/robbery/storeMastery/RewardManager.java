package robbery.storeMastery;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import java.io.File;
import java.util.List;
import java.util.Map;

public class RewardManager {
    private final Robbery plugin;
    private final File rewardFile;
    private FileConfiguration rewardsCfg;

    public RewardManager(Robbery plugin) {
        this.plugin = plugin;
        this.rewardFile = new File(plugin.getDataFolder(), "mastery-rewards.yml");
        if (!rewardFile.exists()) {
            plugin.saveResource("mastery-rewards.yml", false);
        }
        reload();
    }

    public void reload() {
        this.rewardsCfg = YamlConfiguration.loadConfiguration(rewardFile);
    }

    /**
     * Apply rewards configured for storeId at a specific level.
     */
    public void applyRewards(Player player, String storeId, int level) {
        String node = "rewards." + storeId + "." + level;
        if (!rewardsCfg.contains(node)) return;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        int itemsInStore = pd != null ? pd.getStoreMastery(storeId) : 0;
        int totalItems = pd != null ? pd.getStoreMastery(storeId) : itemsInStore;

        // Money
        if (rewardsCfg.isDouble(node + ".money") || rewardsCfg.isInt(node + ".money")) {
            double money = rewardsCfg.getDouble(node + ".money", 0.0); //TODO
            giveMoney(player, money);
        }

        // Robbery XP
        if (rewardsCfg.isInt(node + ".robbery-xp")) {
            int xp = rewardsCfg.getInt(node + ".robbery-xp", 0);
            plugin.getXpManager().addXP(player, xp); //TODO
        }


        if (rewardsCfg.isList(node + ".commands")) {
            List<String> commands = rewardsCfg.getStringList(node + ".commands");
            Map<String, String> placeholders = Map.of(
                    "player", player.getName(),
                    "store", storeId,
                    "level", String.valueOf(level),
                    "items_in_store", String.valueOf(itemsInStore),
                    "total_items", String.valueOf(totalItems)
            );

            for (String cmd : commands) {
                String replaced = replacePlaceholders(cmd, placeholders);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced);
            }
        }

        if (rewardsCfg.isString(node + ".broadcast")) {
            String raw = rewardsCfg.getString(node + ".broadcast");
            String processed = raw
                    .replace("%player%", player.getName())
                    .replace("%store%", storeId)
                    .replace("%level%", String.valueOf(level));
            Bukkit.getServer().broadcastMessage(Messages.colorize(processed));
        }

        if (rewardsCfg.isString(node + ".give-message")) {
            String msg = rewardsCfg.getString(node + ".give-message");
            msg = msg
                    .replace("%items_in_store%", String.valueOf(itemsInStore))
                    .replace("%total_items%", String.valueOf(totalItems))
                    .replace("%store%", storeId)
                    .replace("%level%", String.valueOf(level));
            Messages.send(player, "events.mastery.reward-given");
            Messages.sendFormatted(player, "events.mastery.reward-given-detailed",
                    Map.of("rewards", msg, "store", storeId, "level", String.valueOf(level)));
        }
    }

    private String replacePlaceholders(String input, Map<String,String> placeholders) {
        String out = input;
        for (Map.Entry<String,String> e : placeholders.entrySet()) {
            out = out.replace("%" + e.getKey() + "%", e.getValue());
        }
        return out;
    }


    private void giveMoney(Player player, double amount) {
        if (amount <= 0) return;
        try {
            var econ = Robbery.getEconomy();
            if (econ != null) {
                econ.depositPlayer(player, amount);
            } else {
                Messages.sendFormatted(player, "events.mastery.reward-money", Map.of("amount", String.valueOf(amount)));
            }
        } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
            Messages.sendFormatted(player, "events.mastery.reward-money", Map.of("amount", String.valueOf(amount)));
        }
    }

    public String getRewardDisplay(String storeId, int level) {
        String path = "rewards." + storeId + "." + level + ".give-message";
        return rewardsCfg.getString(path, "None");
    }
}
