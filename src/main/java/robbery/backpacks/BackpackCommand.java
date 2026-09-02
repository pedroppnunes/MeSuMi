package robbery.backpacks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.core.Robbery;

public class BackpackCommand implements CommandExecutor {

    private final Robbery plugin;

    public BackpackCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            plugin.getBackpackGUI().openGUI(p, 1);
        }
        return true;
    }
}
