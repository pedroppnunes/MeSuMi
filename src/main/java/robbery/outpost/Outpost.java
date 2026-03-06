package robbery.outpost;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

/**
 * Handles the /outpost command, which teleports a player
 * to the server's outpost location.
 *
 * <p>
 * Players must have an empty backpack to use this command.
 * </p>
 */
public class Outpost implements CommandExecutor {

    /**
     * Executes the /outpost command.
     *
     * <ul>
     *     <li>If the sender is not a player, a message is sent and nothing happens.</li>
     *     <li>If the player's backpack is not empty, a message is sent and teleportation is canceled.</li>
     *     <li>If conditions are met, the player is teleported to the outpost using a console command.</li>
     * </ul>
     *
     * @param sender the command sender
     * @param command the command object
     * @param label the alias of the command used
     * @param args the command arguments
     * @return true always, as command processing is complete
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (!player.isOnline()) return true;

        if (pd.getBackpack() != null && pd.getBackpack().getSize() != 0) {
            Messages.send(player, "command.outpost.backpack-not-empty");
            return true;
        }

        // Teleport player to outpost using Multiverse command
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " outpost");
        return true;
    }
}
