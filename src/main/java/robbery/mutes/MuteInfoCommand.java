package robbery.mutes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;
import robbery.mutes.MuteManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /muteinfo command, which allows a player to view
 * information about their current mute if they are muted.
 */
public class MuteInfoCommand implements CommandExecutor {

    /** The mute manager responsible for storing and retrieving mute data. */
    private final MuteManager muteManager;

    /**
     * Constructs a new MuteInfoCommand.
     *
     * @param muteManager the MuteManager instance used to check and retrieve mute information
     */
    public MuteInfoCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    /**
     * Executes the /muteinfo command.
     *
     * Command format: /muteinfo
     *
     * @param sender the command sender (must be a player)
     * @param cmd the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.only_players");
            return true;
        }

        // Check if the player is currently muted
        if (!muteManager.isMuted(player.getUniqueId())) {
            Messages.send(player, "command.muteinfo.not_muted");
            return true;
        }

        // Retrieve mute information
        String info = muteManager.getMuteInfo(player.getUniqueId());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("info", info);

        // Display mute information to the player
        Messages.sendFormatted(player, "command.muteinfo.display", placeholders);
        return true;
    }
}
