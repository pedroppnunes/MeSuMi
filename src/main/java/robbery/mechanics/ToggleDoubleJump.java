package robbery.mechanics;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

/**
 * Handles the /doublejump command which allows eligible players to toggle
 * their double jump ability on or off.
 * <p>
 * Requirements:
 * <ul>
 *     <li>Player must have permission "robbery.rank7"</li>
 *     <li>Player must be in the world named "world"</li>
 * </ul>
 *
 * <p>
 * Toggling this command will also disable the standard flight mode.
 * </p>
 */
public class ToggleDoubleJump implements CommandExecutor {

    /**
     * Constructs a ToggleDoubleJump command instance.
     *
     * @param main the main plugin instance
     */
    public ToggleDoubleJump(Robbery main) {
    }

    /**
     * Executes the /doublejump command to toggle double jump for the player.
     *
     * @param sender the command sender (must be a player)
     * @param command the command object
     * @param label the command label
     * @param args command arguments (ignored)
     * @return true if the command was handled
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        PlayerData p = PlayerDataManager.getPlayerData(player);

        // Permission check
        if (!player.hasPermission("robbery.rank7")) {
            Messages.send(player, "command.doublejump.no-permission");
            return true;
        }

        // World check
        if (!player.getWorld().getName().equals("world")) {
            Messages.send(player, "command.doublejump.wrong-world");
            return true;
        }

        // Toggle double jump
        p.toggleDoubleJump();
        if (p.isDoubleJump()) {
            Messages.send(player, "command.doublejump.enabled");
        } else {
            Messages.send(player, "command.doublejump.disabled");
        }

        // Disable normal flight
        player.setAllowFlight(false);

        return true;
    }
}
