package robbery.notifications;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.Collections;
import java.util.List;

public class NotificationsCommand implements CommandExecutor, TabCompleter {

    private final Robbery plugin;
    private final NotificationsGUI notificationsGUI;

    public NotificationsCommand(Robbery plugin, NotificationsGUI notificationsGUI) {
        this.plugin = plugin;
        this.notificationsGUI = notificationsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        notificationsGUI.openGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
