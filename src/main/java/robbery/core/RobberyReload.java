package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.robberyLevel_XP.XPManager;
import robbery.util.ItemsReloader;

import java.util.*;

/**
 * Handles /robbery command and subcommands:
 *  - /robbery reload
 *  - /robbery admin xp <give|set|remove|setlevel|reset|calculate> <player> [amount]
 *  - /robbery admin sp <give|set|remove|reset> <player> [amount]
 *  - /robbery admin itemsstolen <give|set|remove|reset> <player> [amount]
 *  - /robbery admin additems <itemName>
 *  - /robbery admin reload
 */
public class RobberyReload implements CommandExecutor, TabCompleter {

    private final Robbery main;

    public RobberyReload(Robbery main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
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
                    case "xp" -> handleXp(sender, subArgs);
                    case "sp", "skillpoints" -> handleSp(sender, subArgs);
                    case "itemsstolen", "items_stolen", "stolenitems", "stolen" -> handleItemsStolen(sender, subArgs);
                    default -> sendHelp(sender);
                }
                return true;
            }

            String category = first;
            String[] subArgs = Arrays.copyOfRange(args, 2, args.length);

            switch (category) {
                case "xp" -> handleXp(sender, subArgs);
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
        reloadAddItems();
        Messages.reload();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aConfiguration and items reloaded successfully!"));
    }

    // --- XP Category ---
    private void handleXp(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp <give|set|remove|setlevel|reset|calculate> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        XPManager xpManager = main.getXpManager();

        if (action.equals("calculate")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp calculate <itemID>");
                return;
            }
            String itemId = args[1].toLowerCase();
            double hp = main.getItemConfig().getDouble("items." + itemId + ".hp", -1.0);
            if (hp == -1.0) {
                sender.sendMessage(ChatColor.RED + "Item ID '" + itemId + "' not found in additems.yml!");
                return;
            }
            double xpPerItem = Math.pow(hp, 0.85) * 1.4;
            sender.sendMessage("§8§m---------------------------------");
            sender.sendMessage("§aCalculation for: §f" + itemId);
            sender.sendMessage("§2HP: §f" + hp);
            sender.sendMessage("§2Base XP per Item: §f" + String.format("%.2f", xpPerItem));
            sender.sendMessage("§8§m---------------------------------");
            return;
        }

        String targetName;
        String amountStr = null;

        // Support /robbery admin xp <player> <action> [amount]
        Player directPlayer = Bukkit.getPlayer(args[0]);
        if (directPlayer != null && directPlayer.isOnline() && args.length >= 2) {
            targetName = args[0];
            action = args[1].toLowerCase();
            if (args.length >= 3) amountStr = args[2];
        } else {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp " + action + " <player> [amount]");
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
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp give <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    xpManager.addXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aXP to &f" + target.getName() + "&a (Total XP: &e" + pd.getXp() + "&a, Level: &e" + pd.getLevel() + "&a)."));
                }
                case "set" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp set <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    xpManager.setXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aXP to &e" + amount + "&a (Level: &e" + pd.getLevel() + "&a)."));
                }
                case "remove" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp remove <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    long newXp = Math.max(0L, pd.getXp() - amount);
                    xpManager.setXP(target, newXp);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aXP from &f" + target.getName() + "&a (New XP: &e" + newXp + "&a, Level: &e" + pd.getLevel() + "&a)."));
                }
                case "setlevel" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp setlevel <player> <level>");
                        return;
                    }
                    int level = Integer.parseInt(amountStr);
                    xpManager.setLevel(target, level);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aLevel to &e" + level + "&a (XP: &e" + pd.getXp() + "&a)."));
                }
                case "reset" -> {
                    xpManager.setXP(target, 0L);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aXP and level to 1 (0 XP)."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown xp action: " + action + " (Available: give, set, remove, setlevel, reset, calculate)");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
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
        sender.sendMessage("§8=== §5§lRobbery Admin Commands §8===");
        sender.sendMessage("§d/robbery reload §7- Reload plugin data & items");
        sender.sendMessage("§d/robbery admin xp <give|set|remove|setlevel|reset> <player> [amount] §7- Manage player XP");
        sender.sendMessage("§d/robbery admin sp <give|set|remove|reset> <player> [amount] §7- Manage Skill Points");
        sender.sendMessage("§d/robbery admin itemsstolen <give|set|remove|reset> <store> <player> [amount] §7- Manage Items Stolen");
        sender.sendMessage("§d/robbery admin reload §7- Reload plugin data & items");
        sender.sendMessage("§8=================================");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPerm(sender)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("admin", "reload"));
            return filter(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            completions.addAll(List.of("xp", "sp", "itemsstolen", "reload"));
            return filter(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            switch (cat) {
                case "xp" -> completions.addAll(List.of("give", "set", "remove", "setlevel", "reset", "calculate"));
                case "sp", "itemsstolen" -> completions.addAll(List.of("give", "set", "remove", "reset"));
            }
            return filter(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("itemsstolen")) {
                for (int i = 1; i <= 12; i++) {
                    completions.add("store" + i);
                }
            } else if (cat.equals("xp") || cat.equals("sp")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filter(completions, args[3]);
        }
        
        if (args.length == 5 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("itemsstolen")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filter(completions, args[4]);
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

