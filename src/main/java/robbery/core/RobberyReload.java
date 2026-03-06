package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.util.ItemsReloader;

import java.util.Map;

/**
 * Handles the /robbery reload command, allowing OP players or the console
 * to reload plugin data such as items and messages.
 * <p>
 * Usage: <code>/robbery reload</code>
 * </p>
 * <p>
 * When executed, this command reloads all items in the plugin and reloads
 * the messages configuration.
 * </p>
 */
public class RobberyReload implements CommandExecutor {

    /** Reference to the main Robbery plugin instance. */
    private final Robbery main;

    /**
     * Constructs a new RobberyReload command executor.
     *
     * @param main the main plugin instance
     */
    public RobberyReload(Robbery main){
        this.main = main;
    }

    /**
     * Executes the /robbery reload command.
     * <p>
     * Only players with operator permissions or the console can execute this command.
     * It reloads the plugin's items and messages configuration.
     * </p>
     *
     * @param sender the command sender
     * @param command the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command was executed (even if usage was incorrect)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("robbery")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!(sender instanceof Player) || sender.isOp()) {
                    reloadAddItems();
                    Messages.reload();
                    Messages.send(sender, "command.reload.success");
                } else {
                    Messages.send(sender, "global.no-permission");
                }
                return true;
            }
            Messages.send(sender, "command.reload.usage");
            return true;
        }
        return false;
    }

    /**
     * Reloads all items into the plugin's internal map.
     * <p>
     * This method is called internally when executing the reload command.
     * </p>
     */
    public void reloadAddItems() {
        int updated = ItemsReloader.reloadAndSync(main);
        Messages.sendFormatted(Bukkit.getConsoleSender(), "command.reload.updated-items", Map.of("count", String.valueOf(updated)));
    }
}
