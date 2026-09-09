package robbery.robberyLevel_XP;

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
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminXPCommand implements CommandExecutor, TabCompleter {

    private final Robbery plugin;

    public AdminXPCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.adminxp") && !sender.hasPermission("robbery.op") && !sender.hasPermission("robbery.admin")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String firstArg = args[0].toLowerCase();

        if (firstArg.equals("robbery") || firstArg.equals("robberyxp") || firstArg.equals("robbery_xp") || firstArg.equals("r")) {
            handleRobberyXP(sender, Arrays.copyOfRange(args, 1, args.length));
        } else if (firstArg.equals("vanilla") || firstArg.equals("mc") || firstArg.equals("minecraft") || firstArg.equals("v") || firstArg.equals("vanillaxp")) {
            handleVanillaXP(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            handleRobberyXP(sender, args);
        }

        return true;
    }

    public void handleRobberyXP(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendRobberyXpHelp(sender);
            return;
        }

        String action = args[0].toLowerCase();
        XPManager xpManager = plugin.getXpManager();

        if (action.equals("calculate")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /adminxp calculate <itemID>");
                return;
            }
            String itemId = args[1].toLowerCase();
            double hp = plugin.getItemConfig().getDouble("items." + itemId + ".hp", -1.0);
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

        Player directPlayer = Bukkit.getPlayer(args[0]);
        if (directPlayer != null && directPlayer.isOnline() && args.length >= 2) {
            targetName = args[0];
            action = args[1].toLowerCase();
            if (args.length >= 3) amountStr = args[2];
        } else {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /adminxp [robbery] <give|set|remove|setlevel|reset> <player> [amount]");
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
                case "give", "add" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp [robbery] give <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    xpManager.addXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aRobbery XP to &f" + target.getName() + " &7(Total XP: &e" + pd.getXp() + "&7, Robbery Level: &e" + pd.getLevel() + "&7)."));
                }
                case "set" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp [robbery] set <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    xpManager.setXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aRobbery XP to &e" + amount + " &7(Robbery Level: &e" + pd.getLevel() + "&7)."));
                }
                case "remove", "take" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp [robbery] remove <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(amountStr);
                    long newXp = Math.max(0L, pd.getXp() - amount);
                    xpManager.setXP(target, newXp);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aRobbery XP from &f" + target.getName() + " &7(New XP: &e" + newXp + "&7, Robbery Level: &e" + pd.getLevel() + "&7)."));
                }
                case "setlevel" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp [robbery] setlevel <player> <level>");
                        return;
                    }
                    int level = Integer.parseInt(amountStr);
                    xpManager.setLevel(target, level);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aRobbery Level to &e" + level + " &7(Robbery XP: &e" + pd.getXp() + "&7)."));
                }
                case "reset" -> {
                    xpManager.setXP(target, 0L);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aRobbery XP and level to 1 (0 XP)."));
                }
                default -> sendRobberyXpHelp(sender);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    public void handleVanillaXP(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendVanillaXpHelp(sender);
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
                sender.sendMessage(ChatColor.RED + "Usage: /adminxp vanilla <give|set|remove|setlevel|reset> <player> [amount]");
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

        try {
            switch (action) {
                case "give", "add" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp vanilla give <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    VanillaXPUtils.giveXP(target, amount);
                    int newTotal = VanillaXPUtils.getTotalExperience(target);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aVanilla XP points to &f" + target.getName() + " &7(Total XP: &e" + newTotal + "&7, Level: &e" + target.getLevel() + "&7)."));
                }
                case "set" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp vanilla set <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    VanillaXPUtils.setXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aVanilla XP points to &e" + amount + " &7(Level: &e" + target.getLevel() + "&7)."));
                }
                case "remove", "take" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp vanilla remove <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(amountStr);
                    VanillaXPUtils.removeXP(target, amount);
                    int newTotal = VanillaXPUtils.getTotalExperience(target);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aVanilla XP points from &f" + target.getName() + " &7(New XP: &e" + newTotal + "&7, Level: &e" + target.getLevel() + "&7)."));
                }
                case "setlevel" -> {
                    if (amountStr == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /adminxp vanilla setlevel <player> <level>");
                        return;
                    }
                    int level = Integer.parseInt(amountStr);
                    VanillaXPUtils.setLevel(target, level);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aVanilla XP Level to &e" + level + " &7(Total XP: &e" + VanillaXPUtils.getTotalExperience(target) + "&7)."));
                }
                case "reset" -> {
                    VanillaXPUtils.resetXP(target);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aVanilla XP and levels to 0."));
                }
                default -> sendVanillaXpHelp(sender);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8=== &5&lRobbery Admin XP Options &8==="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery <give|set|remove|setlevel|reset> <player> [amount] &7- Manage Robbery XP & Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp vanilla <give|set|remove|setlevel|reset> <player> [amount] &7- Manage Vanilla Minecraft XP & Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp calculate <itemID> &7- Test item XP values"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8================================="));
    }

    private void sendRobberyXpHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8=== &5&lRobbery XP Options &8==="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery give <player> <amount> &7- Gives Robbery XP"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery set <player> <amount> &7- Sets total Robbery XP"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery remove <player> <amount> &7- Removes Robbery XP"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery setlevel <player> <level> &7- Sets Robbery Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/adminxp robbery reset <player> &7- Resets Robbery XP to 0"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8================================="));
    }

    private void sendVanillaXpHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8=== &aVanilla XP Options &8==="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/adminxp vanilla give <player> <amount> &7- Gives Vanilla XP points"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/adminxp vanilla set <player> <amount> &7- Sets total Vanilla XP points"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/adminxp vanilla remove <player> <amount> &7- Removes Vanilla XP points"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/adminxp vanilla setlevel <player> <level> &7- Sets Vanilla XP Level"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/adminxp vanilla reset <player> &7- Resets Vanilla XP to 0"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8================================="));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("robbery.adminxp") && !sender.hasPermission("robbery.op") && !sender.hasPermission("robbery.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("robbery", "vanilla", "give", "set", "remove", "setlevel", "reset", "calculate"));
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("robbery") || first.equals("r")) {
                completions.addAll(List.of("give", "set", "remove", "setlevel", "reset", "calculate"));
            } else if (first.equals("vanilla") || first.equals("v") || first.equals("mc")) {
                completions.addAll(List.of("give", "set", "remove", "setlevel", "reset"));
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filter(completions, args[1]);
        }

        if (args.length == 3) {
            String first = args[0].toLowerCase();
            if (first.equals("robbery") || first.equals("r") || first.equals("vanilla") || first.equals("v") || first.equals("mc")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filter(completions, args[2]);
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