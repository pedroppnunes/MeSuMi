package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.robberyLevel_XP.AdminXPCommand;
import robbery.util.ItemsReloader;

import java.util.*;

/**
 * Handles /robbery command and subcommands:
 *  - /robbery reload
 *  - /robbery admin xp [robbery|vanilla] <give|set|remove|setlevel|reset|calculate> <player> [amount]
 *  - /robbery admin sp <give|set|remove|reset> <player> [amount]
 *  - /robbery admin itemsstolen <give|set|remove|reset> <player> [amount]
 *  - /robbery admin reload
 */
public class RobberyReload implements CommandExecutor, TabCompleter {

    private final Robbery main;
    private final AdminXPCommand adminXPCommand;

    public RobberyReload(Robbery main) {
        this.main = main;
        this.adminXPCommand = new AdminXPCommand(main);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. Direct /robbery reload
        if (sub.equals("reload")) {
            if (!hasAdminPerm(sender)) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            handleReload(sender);
            return true;
        }

        // 2. /robbery admin ...
        if (sub.equals("admin")) {
            if (!hasAdminPerm(sender)) {
                Messages.send(sender, "global.no-permission");
                return true;
            }

            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }

            String first = args[1].toLowerCase();

            // Allow /robbery admin <give|set|remove|setlevel|reset> <xp|sp|itemsstolen> <player> <amount>
            if (first.equals("give") || first.equals("set") || first.equals("remove") || first.equals("setlevel") || first.equals("reset")) {
                if (args.length < 3) {
                    sendHelp(sender);
                    return true;
                }
                String cat = args[2].toLowerCase();
                List<String> subList = new ArrayList<>();
                subList.add(first);
                for (int i = 3; i < args.length; i++) {
                    subList.add(args[i]);
                }
                String[] subArgs = subList.toArray(new String[0]);
                switch (cat) {
                    case "xp" -> adminXPCommand.handleRobberyXP(sender, subArgs);
                    case "sp", "skillpoints" -> handleSp(sender, subArgs);
                    case "itemsstolen", "items_stolen", "stolenitems", "stolen" -> handleItemsStolen(sender, subArgs);
                    default -> sendHelp(sender);
                }
                return true;
            }

            String category = first;
            String[] subArgs = Arrays.copyOfRange(args, 2, args.length);

            switch (category) {
                case "xp" -> adminXPCommand.onCommand(sender, command, "adminxp", subArgs);
                case "sp", "skillpoints" -> handleSp(sender, subArgs);
                case "itemsstolen", "items_stolen", "stolenitems", "stolen" -> handleItemsStolen(sender, subArgs);
                case "reload" -> handleReload(sender);
                default -> sendHelp(sender);
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private boolean hasAdminPerm(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        return sender.isOp() || sender.hasPermission("robbery.op") || sender.hasPermission("robbery.admin");
    }

    private void handleReload(CommandSender sender) {
        main.reloadConfig();
        reloadAddItems();
        Messages.reload();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aConfiguration and items reloaded successfully!"));
    }

    // --- SP Category ---
    private void handleSp(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp <give|set|remove|reset> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        String targetName;
        String amountStr = null;

        Player directPlayer = Bukkit.getPlayer(args[0]);
        if (directPlayer != null && directPlayer.isOnline() && args.length >= 2) {
            targetName = args[0];
            action = args[1].toLowerCase();
            if (args.length >= 3) amountStr = args[2];
        } else {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp " + action + " <player> [amount]");
                return;
            }
            targetName = args[1];
            if (args.length >= 3) amountStr = args[2];
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        try {
            switch (action) {
                case "give" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp give <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    pd.addSkillPoints(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aSkill Points to &f" + target.getName() + " &7(Total: &e" + pd.getSkillPoints() + "&7)."));
                }
                case "set" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp set <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    pd.setSkillPoints(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aSkill Points to &e" + pd.getSkillPoints() + "&a."));
                }
                case "remove" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp remove <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    int newSp = Math.max(0, pd.getSkillPoints() - amount);
                    pd.setSkillPoints(newSp);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aSkill Points from &f" + target.getName() + " &7(New: &e" + newSp + "&7)."));
                }
                case "reset" -> {
                    pd.setSkillPoints(0);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aSkill Points to 0."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown sp action: " + action + " (Available: give, set, remove, reset)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    // --- ItemsStolen Category ---
    private void handleItemsStolen(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen <give|set|remove|reset> <store> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        String storeId = args[1].toLowerCase();
        String targetName = args[2];
        String amountStr = (args.length >= 4) ? args[3] : null;

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        try {
            switch (action) {
                case "give" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen give <store> <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    int oldLevel = main.getMasteryManager().getLevelFromItems(storeId, pd.getStoreItems(storeId));
                    pd.addStoreItems(storeId, amount);
                    pd.addItemsStolen(amount);
                    int newLevel = main.getMasteryManager().getLevelFromItems(storeId, pd.getStoreItems(storeId));

                    if (newLevel > oldLevel) {
                        pd.setStoreMilestone(storeId, newLevel);
                        robbery.keys.Keys store = robbery.keys.KeyManager.getStoreName(storeId);
                        if (store != null) {
                            for (int l = oldLevel + 1; l <= newLevel; l++) {
                                main.getMasteryManager().handleLevelUp(target, store, l);
                            }
                        }
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aAdded &e" + amount + " &aItems Stolen to &f" + target.getName() + " &afor &e" + storeId + " &7(Store Total: &e" + pd.getStoreItems(storeId) + "&7, Global: &e" + pd.getItemsStolen() + "&7, Mastery: &dM" + pd.getStoreMasteryLevel(storeId) + "&7)."));
                }
                case "set" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen set <store> <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    int oldLevel = pd.getStoreMasteryLevel(storeId);
                    pd.getStoreItemsMap().put(storeId, amount);
                    int newLevel = main.getMasteryManager().getLevelFromItems(storeId, amount);
                    pd.setStoreMilestone(storeId, newLevel);
                    if (newLevel > oldLevel) {
                        robbery.keys.Keys store = robbery.keys.KeyManager.getStoreName(storeId);
                        if (store != null) {
                            for (int l = oldLevel + 1; l <= newLevel; l++) {
                                main.getMasteryManager().handleLevelUp(target, store, l);
                            }
                        }
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aItems Stolen for &e" + storeId + " &ato &e" + amount + " &7(Mastery: &dM" + newLevel + "&7)."));
                }
                case "remove" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen remove <store> <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    int newStolen = Math.max(0, pd.getStoreItems(storeId) - amount);
                    pd.getStoreItemsMap().put(storeId, newStolen);
                    pd.setItemsStolen(Math.max(0, pd.getItemsStolen() - amount));
                    int newLevel = main.getMasteryManager().getLevelFromItems(storeId, newStolen);
                    pd.setStoreMilestone(storeId, newLevel);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aItems Stolen from &f" + target.getName() + " &afor &e" + storeId + " &7(New Store Total: &e" + newStolen + "&7, Mastery: &dM" + newLevel + "&7)."));
                }
                case "reset" -> {
                    pd.getStoreItemsMap().put(storeId, 0);
                    pd.setStoreMilestone(storeId, 0);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aItems Stolen for &e" + storeId + " &ato 0."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown itemsstolen action: " + action + " (Available: give, set, remove, reset)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    public void reloadAddItems() {
        int updated = ItemsReloader.reloadAndSync(main);
        Messages.sendFormatted(Bukkit.getConsoleSender(), "command.reload.updated-items", Map.of("count", String.valueOf(updated)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8=== &5&lRobbery Admin Commands &8==="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/robbery reload &7- Reload plugin data & items"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/robbery admin xp robbery <give|set|remove|setlevel|reset> <player> [amount] &7- Manage Robbery XP & Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/robbery admin xp vanilla <give|set|remove|setlevel|reset> <player> [amount] &7- Manage Vanilla XP & Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/robbery admin sp <give|set|remove|reset> <player> [amount] &7- Manage Skill Points"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/robbery admin itemsstolen <give|set|remove|reset> <store> <player> [amount] &7- Manage Items Stolen"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8================================="));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPerm(sender)) return Collections.emptyList();

        if (args.length == 1) {
            return filter(List.of("admin", "reload"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(List.of("xp", "sp", "itemsstolen", "reload"), args[1]);
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("xp")) {
            String[] subArgs = Arrays.copyOfRange(args, 2, args.length);
            return adminXPCommand.onTabComplete(sender, command, "adminxp", subArgs);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("sp") || cat.equals("itemsstolen")) {
                return filter(List.of("give", "set", "remove", "reset"), args[2]);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("itemsstolen")) {
                List<String> stores = new ArrayList<>();
                for (int i = 1; i <= 12; i++) stores.add("store" + i);
                return filter(stores, args[3]);
            } else if (cat.equals("sp")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[3]);
            }
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("itemsstolen")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
                return filter(players, args[4]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
