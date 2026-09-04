package robbery.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HideoutAliasCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (args.length > 0 && (args[0].equalsIgnoreCase("contrib") || args[0].equalsIgnoreCase("contributors") ||
                    args[0].equalsIgnoreCase("topcontrib") || args[0].equalsIgnoreCase("topcontributors"))) {
                HideoutTopCommand.sendHideoutTop(p);
                return true;
            }
            String cmd = "hideout" + (args.length > 0 ? " " + String.join(" ", args) : "");
            p.performCommand(cmd);
        }
        return true;
    }
}
