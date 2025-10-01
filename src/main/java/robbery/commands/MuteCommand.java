package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;
import robbery.mutes.MuteManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /mute command which allows a sender to mute a player
 * for a specified duration with an optional reason.
 */
public class MuteCommand implements CommandExecutor {

    /** The mute manager responsible for applying mutes. */
    private final MuteManager muteManager;

    /**
     * Constructs a new MuteCommand.
     *
     * @param muteManager the MuteManager instance to handle mutes
     */
    public MuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    /**
     * Executes the /mute command.
     * Command format: /mute <player> <duration> [reason]
     *
     * @param sender the command sender
     * @param cmd the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length < 2) {
            Messages.send(sender, "command.mute.usage");
            return true;
        }

        // Get the target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String duration = args[1];
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;

        // Apply the mute
        muteManager.mutePlayer(target.getUniqueId(), sender.getName(), duration, reason);

        // Prepare placeholders for the confirmation message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("duration", duration);
        placeholders.put("reason", reason != null ? reason : Messages.get("command.mute.no_reason"));

        // Send confirmation to sender
        Messages.sendFormatted(sender, "command.mute.success", placeholders);
        return true;
    }
}
