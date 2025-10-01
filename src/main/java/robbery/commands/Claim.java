package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;

/**
 * Handles the /claim command, allowing players to open their rewards GUI.
 * <p>
 * Players can only use this command in the "SuperiorWorld". When executed,
 * it opens the reward GUI for the player via {@link Rcrate#openRewardGUI(Player)}.
 * </p>
 * <p>
 * Command usage: /claim
 * </p>
 */
public class Claim implements CommandExecutor {

    /**
     * Executes the /claim command.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the command label used
     * @param args    command arguments (ignored)
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        if (!p.getWorld().getName().equalsIgnoreCase("SuperiorWorld")) {
            Messages.send(p, "command.claim.only-in-hideout");
            return true;
        }

        Rcrate.openRewardGUI(p);
        return true;
    }
}
