package robbery.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HideoutAliasCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            String cmd = "hideout" + (args.length > 0 ? " " + String.join(" ", args) : "");
            p.performCommand(cmd);
        }
        return true;
    }
}
