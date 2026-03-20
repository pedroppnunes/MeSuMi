package robbery.skilltree;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

public class SkillTreeResetCommand implements CommandExecutor {

    public SkillTreeResetCommand(Robbery plugin) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        if (args.length < 1) return false;

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return true;

        if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {
            try {
                int amount = Integer.parseInt(args[2]);
                pd.addResetSkillTreePoints(amount);
                sender.sendMessage("§aAdded §e" + amount + "§a reset points to §e" + target.getName());
                Messages.sendFormatted(target, "command.skilltree.resetpoints-added", Map.of("amount", String.valueOf(amount)));
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
            }
            return true;
        }

        if (pd.getResetSkillTreePoints() <= 0) {
            Messages.send(target, "command.skilltree.no-reset");
            return true;
        }

        int totalRefund = Robbery.getSkillTreeConfig().calculateTotalRefund(pd);

        Robbery.getSkillTreeConfig().performReset(pd);

        if (pd.consumeResetSkillTreePoint()) {
            if (totalRefund > 0) {
                pd.addSkillPoints(totalRefund);
            }

            sender.sendMessage("§aSkilltree reset for " + targetName + ". Refunded: §e" + totalRefund + "§a. Tokens left: §e" + pd.getResetSkillTreePoints());
            Messages.send(target, "command.skilltree.success");
        }

        return true;
    }


}