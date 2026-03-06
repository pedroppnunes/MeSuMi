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
            Messages.send(sender,"global.no-permission");
            return true;
        }

        if (args.length < 2) {
            Messages.send(sender,"command.adminxp.help_header");
            Messages.send(sender,"command.adminxp.help_commands");
            return true;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null || !target.isOnline()) {
            Messages.send(sender,"global.player-not-found");
            return true;
        }

        XPManager xpManager = plugin.getXpManager();

        try {
            switch (action) {
                case "give":
                    if (args.length < 3) {
                        Messages.send(sender,"command.adminxp.help_header");
                        Messages.send(sender,"command.adminxp.help_commands");
                        return true;
                    }
                    long giveAmount = Long.parseLong(args[2]);
                    if (giveAmount <= 0) {
                        Messages.send(sender,"command.adminxp.greater_than_zero");
                        return true;
                    }
                    xpManager.addXP(target, giveAmount);
                    Messages.sendFormatted(sender,"command.adminxp.give",java.util.Map.of("player",target.getName(),"amount",String.valueOf(giveAmount)));
                    break;

                case "set":
                    if (args.length < 3) {
                        Messages.send(sender,"command.adminxp.help_header");
                        Messages.send(sender,"command.adminxp.help_commands");
                        return true;
                    }
                    long setAmount = Long.parseLong(args[2]);
                    if (setAmount < 0) {
                        Messages.send(sender,"command.adminxp.no_negative");
                        return true;
                    }
                    xpManager.setXP(target, setAmount);
                    Messages.sendFormatted(sender,"command.adminxp.set_xp",java.util.Map.of("player",target.getName(),"amount",String.valueOf(setAmount)));
                    break;

                case "setlevel":
                    if (args.length < 3) {
                        Messages.send(sender,"command.adminxp.help_header");
                        Messages.send(sender,"command.adminxp.help_commands");
                        return true;
                    }
                    int setLevel = Integer.parseInt(args[2]);
                    if (setLevel < 1) {
                        Messages.send(sender,"command.adminxp.min_level");
                        return true;
                    }
                    xpManager.setLevel(target, setLevel);
                    Messages.sendFormatted(sender,"command.adminxp.set_level",java.util.Map.of("player",target.getName(),"level",String.valueOf(setLevel)));
                    break;

                case "reset":
                    xpManager.setXP(target, 0);
                    Messages.sendFormatted(sender,"command.adminxp.reset",java.util.Map.of("player",target.getName()));
                    break;

                default:
                    Messages.send(sender,"command.adminxp.help_header");
                    Messages.send(sender,"command.adminxp.help_commands");
                    break;
            }
        } catch (NumberFormatException e) {
            Messages.send(sender,"command.adminxp.invalid_number");
        }

        return true;
    }
}
