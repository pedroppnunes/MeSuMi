package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;

/**
 * Handles the /load command, which reloads or loads the plugin's items configuration.
 * <p>
 * Only players with the "robbery.op" permission can execute this command.
 * </p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>/load - Loads or reloads plugin items.</li>
 * </ul>
 */
public class Load implements CommandExecutor {

    private final Robbery main;

    /**
     * Creates a new Load command executor.
     *
     * @param main the main plugin instance
     */
    public Load(Robbery main) {
        this.main = main;
    }

    /**
     * Executes the /load command.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the command label
     * @param args    command arguments (ignored)
     * @return true if the command was handled
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true; // Only players can execute this
        }

        if (!player.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        main.loadItems(); // Reload the items configuration
        Messages.send(player, "command.load.success"); // Optional feedback

        return true;
    }
}
