package robbery.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;

/**
 * Handles the /spawn command which teleports a player to a predefined spawn location.
 * <p>
 * The spawn location is hardcoded to coordinates (20025.5, 100, 20015.5) in the world named "world"
 * with a yaw of -90 and pitch of 0.
 * </p>
 * <p>
 * Only players can use this command. If the world is not found, a message is sent to the player.
 * </p>
 */
public class SpawnCommand implements CommandExecutor {

    /**
     * Executes the /spawn command.
     *
     * @param sender the sender of the command (must be a player)
     * @param cmd the command object
     * @param label the command label
     * @param args command arguments (ignored for this command)
     * @return true if the command was executed or a message was sent to the sender
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        World world = Bukkit.getWorld("world");

        if (world == null) {
            Messages.send(player, "command.spawn.world-not-found");
            return true;
        }

        // Predefined spawn location
        Location spawn = new Location(world, 20025.5, 100, 20015.5, -90.0f, 0.0f);

        player.teleport(spawn);

        Messages.send(player, "command.spawn.teleporting");
        return true;
    }
}
