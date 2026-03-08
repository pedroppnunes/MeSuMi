package robbery.robberyLevel_XP;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.Map;

public class AdminXPCommand implements CommandExecutor {

    private final Robbery plugin;

    public AdminXPCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        XPManager xpManager = plugin.getXpManager();

        try {
            switch (action) {
                case "calculate":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /adminxp calculate <itemID>");
                        return true;
                    }
                    String itemId = args[1].toLowerCase();
                    // Reads directly from your additems.yml
                    double hp = plugin.getItemConfig().getDouble("items." + itemId + ".hp", -1.0);

                    if (hp == -1.0) {
                        sender.sendMessage("§cItem ID '" + itemId + "' not found in additems.yml!");
                        return true;
                    }

                    double xpPerItem = getXpPerItem(itemId, hp);
                    long invXP = (long) (xpPerItem * 20);

                    sender.sendMessage("§8§m---------------------------------");
                    sender.sendMessage("§aCalculation for: §f" + itemId);
                    sender.sendMessage("§2HP: §f" + hp);
                    sender.sendMessage("§2XP per Item: §f" + String.format("%.2f", xpPerItem));
                    sender.sendMessage("§2XP per Inventory (20 slots): §a" + invXP);
                    sender.sendMessage("§2XP per Hour (20 trips): §6" + (invXP * 20));
                    sender.sendMessage("§8§m---------------------------------");
                    break;

                case "give":
                case "set":
                case "setlevel":
                case "reset":
                    // These all require a target player
                    if (args.length < 2) {
                        sendHelp(sender);
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null || !target.isOnline()) {
                        Messages.send(sender, "global.player-not-found");
                        return true;
                    }

                    handlePlayerActions(sender, action, target, args, xpManager);
                    break;

                default:
                    sendHelp(sender);
                    break;
            }
        } catch (NumberFormatException e) {
            Messages.send(sender, "command.adminxp.invalid_number");
        }

        return true;
    }

    private void handlePlayerActions(CommandSender sender, String action, Player target, String[] args, XPManager xpManager) {
        switch (action) {
            case "give":
                if (args.length < 3) { sendHelp(sender); return; }
                long giveAmount = Long.parseLong(args[2]);
                xpManager.addXP(target, giveAmount);
                Messages.sendFormatted(sender, "command.adminxp.give", Map.of("player", target.getName(), "amount", String.valueOf(giveAmount)));
                break;
            case "set":
                if (args.length < 3) { sendHelp(sender); return; }
                long setAmount = Long.parseLong(args[2]);
                xpManager.setXP(target, setAmount);
                Messages.sendFormatted(sender, "command.adminxp.set_xp", Map.of("player", target.getName(), "amount", String.valueOf(setAmount)));
                break;
            case "setlevel":
                if (args.length < 3) { sendHelp(sender); return; }
                int setLevel = Integer.parseInt(args[2]);
                xpManager.setLevel(target, setLevel);
                Messages.sendFormatted(sender, "command.adminxp.set_level", Map.of("player", target.getName(), "level", String.valueOf(setLevel)));
                break;
            case "reset":
                xpManager.setXP(target, 0);
                Messages.sendFormatted(sender, "command.adminxp.reset", Map.of("player", target.getName()));
                break;
        }
    }

    private void sendHelp(CommandSender sender) {
        Messages.send(sender, "command.adminxp.help_header");
        Messages.send(sender, "command.adminxp.help_commands");
    }

    private double getXpPerItem(String itemId, double hp) {
        double storeMultiplier = 1.0;
        int underscoreIndex = itemId.indexOf("_");
        if (itemId.startsWith("s") && underscoreIndex > 1) {
            try {
                int storeNumber = Integer.parseInt(itemId.substring(1, underscoreIndex));
                storeMultiplier = 1.0 + (storeNumber * 0.10);
            } catch (NumberFormatException ignored) {}
        }
        return Math.pow(hp, 0.85) * 1.4 * storeMultiplier;
    }
}