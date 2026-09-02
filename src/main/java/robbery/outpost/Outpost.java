package robbery.outpost;

import org.bukkit.Bukkit;
import org.bukkit.World;
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (!player.isOnline()) return true;

        if (pd != null && pd.getBackpack() != null && !pd.getBackpack().getItems().isEmpty()) {
            Messages.send(player, "command.outpost.backpack-not-empty");
            return true;
        }

        // Try direct world teleport first if world "outpost" exists
        World outpostWorld = Bukkit.getWorld("outpost");
        if (outpostWorld != null) {
            player.teleport(outpostWorld.getSpawnLocation());
            return true;
        }

        // Fallback to Multiverse or warp command
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv tp " + player.getName() + " outpost");
        return true;
    }
}
