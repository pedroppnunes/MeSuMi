package robbery.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
 * Handles the /mall command.
 * <p>
 * Teleports eligible players to the mall location in the main world.
 * Only players with ranks 4 through 7 (Outlaw+) can use this command.
 * Players must have an empty backpack to enter.
 * </p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>/mall - Teleports the player to the mall if conditions are met.</li>
 * </ul>
 */
public class Mall implements CommandExecutor {

    public Mall(Robbery main) {
        // Constructor can be used for future plugin reference if needed
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (!player.isOnline()) return true;

        // Ensure backpack is empty
        if (pd.getBackpack() != null && pd.getBackpack().getSize() != 0) {
            Messages.send(player, "command.mall.backpack-not-empty");
            return true;
        }

        // Check rank permission: Outlaw+ (ranks 4, 5, 6, 7), robbery.mall, OP, or admin
        boolean hasAccess = player.hasPermission("robbery.mall")
                || player.hasPermission("robbery.op")
                || player.hasPermission("robbery.bypass")
                || player.isOp()
                || (pd != null && robbery.ranks.RankManager.getRankOrder(pd.getRank()) >= 4)
                || player.hasPermission("robbery.rank4")
                || player.hasPermission("robbery.rank5")
                || player.hasPermission("robbery.rank6")
                || player.hasPermission("robbery.rank7")
                || player.hasPermission("robbery.outlaw")
                || player.hasPermission("robbery.heister")
                || player.hasPermission("robbery.kingpin")
                || player.hasPermission("robbery.mafiaboss")
                || player.hasPermission("robbery.mafia_boss");

        if (hasAccess) {
            World targetWorld = Bukkit.getWorld("world");
            if (targetWorld == null) {
                targetWorld = player.getWorld();
            }
            Location mallLocation = new Location(targetWorld, 20064.5, 101, 20033.5);
            player.teleport(mallLocation);
        } else {
            Messages.send(player, "command.mall.no-permission");
        }

        return true;
    }
}
