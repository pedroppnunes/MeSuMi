package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreCatalogCommand implements CommandExecutor, TabCompleter {

    private final Robbery plugin;

    public StoreCatalogCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        // Handle /stats [player] command
        if (cmdName.equals("stats")) {
            if (args.length == 0) {
                plugin.getPlayerStatsGUI().openGUI(player, player);
                return true;
            }

            String targetName = args[0];
            Player specified = Bukkit.getPlayer(targetName);
            if (specified != null && specified.isOnline()) {
                plugin.getPlayerStatsGUI().openGUI(player, specified);
            } else {
                org.bukkit.OfflinePlayer offTarget = Bukkit.getOfflinePlayer(targetName);
                if (offTarget != null) {
                    plugin.getPlayerStatsGUI().openGUIForOfflinePlayer(player, offTarget);
                } else {
                    Messages.send(sender, "global.player-not-found");
                }
            }
            return true;
        }

        // Handle /catalog [store] command
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return true;

        if (args.length == 0) {
            String currentStore = plugin.getStorePlaytimeTask().detectStore(player);
            if (currentStore != null) {
                plugin.getStoreCatalogGUI().openGUI(player, currentStore, 1);
            } else {
                plugin.getStoreCatalogGUI().openStoreSelectorGUI(player);
            }
            return true;
        }

        String arg = args[0].toLowerCase();
        String targetStore = null;

        if (arg.startsWith("store")) {
            targetStore = arg;
        } else {
            try {
                int storeNum = Integer.parseInt(arg);
                if (storeNum >= 1 && storeNum <= 12) {
                    targetStore = "store" + storeNum;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (targetStore == null) {
            for (int i = 1; i <= 12; i++) {
                String sId = "store" + i;
                String storeName = KeyManager.getStoreN(sId);
                if (storeName != null && storeName.toLowerCase().startsWith(arg)) {
                    targetStore = sId;
                    break;
                }
            }
        }

        if (targetStore == null) {
            Messages.send(player, "storeMastery.catalog.unknown-store");
            return true;
        }

        plugin.getStoreCatalogGUI().openGUI(player, targetStore, 1);
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String arg = args[0].toLowerCase();

            if (command.getName().equalsIgnoreCase("stats")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(arg)) {
                        suggestions.add(p.getName());
                    }
                }
                return suggestions;
            }

            for (int i = 1; i <= 12; i++) {
                String sId = "store" + i;
                if (sId.toLowerCase().startsWith(arg)) suggestions.add(sId);
                String name = KeyManager.getStoreN(sId);
                if (name != null && name.toLowerCase().startsWith(arg)) suggestions.add(name);
            }
            return suggestions;
        }
        return Collections.emptyList();
    }
}
