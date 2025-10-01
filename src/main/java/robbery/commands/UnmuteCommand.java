package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import robbery.mutes.MuteManager;

import java.util.UUID;

/**
 * Command to unmute a muted player.
 * <p>
 * Usage: /unmute &lt;player&gt;
 * </p>
 * <p>
 * This command checks if the target player is muted using {@link MuteManager}.
 * If muted, it removes the mute and informs the sender. If the player is not muted,
 * it notifies the sender accordingly.
 * </p>
 */
public class UnmuteCommand implements CommandExecutor {

    private final MuteManager muteManager;

    /**
     * Constructs a new UnmuteCommand instance.
     *
     * @param muteManager The MuteManager instance used to check and remove mutes.
     */
    public UnmuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    /**
     * Executes the /unmute command.
     *
     * @param sender  The sender of the command.
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    Command arguments. Expected: 1 argument (player name).
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        if (!muteManager.isMuted(uuid)) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not muted.");
            return true;
        }

        muteManager.unmutePlayer(uuid);
        sender.sendMessage(ChatColor.GREEN + "Successfully unmuted " + target.getName() + ".");
        return true;
    }
}
