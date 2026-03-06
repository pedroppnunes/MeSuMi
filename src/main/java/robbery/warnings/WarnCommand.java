package robbery.warnings;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Handles the /warn command, which issues a warning to a player.
 * <p>
 * Usage: /warn &lt;player&gt; &lt;duration&gt; &lt;reason&gt;
 * </p>
 * <p>
 * The command records the warning in the plugin's WarningManager and optionally
 * displays a GUI to the warned player. Duration must include a suffix:
 * s (seconds), m (minutes), h (hours), d (days), w (weeks), y (years).
 * </p>
 */
public class WarnCommand implements CommandExecutor {

    private final Robbery plugin;

    /**
     * Constructs a new WarnCommand.
     *
     * @param plugin The main plugin instance.
     */
    public WarnCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the /warn command.
     *
     * @param sender The sender of the command (Player or console).
     * @param cmd    The command object.
     * @param label  The alias of the command used.
     * @param args   Command arguments: [playerName, duration, reason...].
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {

        if (args.length < 3) {
            Messages.send(sender, "command.warn.usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        String durationStr = args[1].toLowerCase();
        LocalDateTime expiry = parseDuration(durationStr);
        if (expiry == null) {
            Messages.send(sender, "command.warn.invalid-duration");
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String issuer = (sender instanceof Player) ? sender.getName() : "Server";

        plugin.getWarningManager().addWarning(target.getUniqueId(), reason, issuer, durationStr);

        Messages.sendFormatted(sender, "command.warn.success", java.util.Map.of(
                "player", target.getName(),
                "duration", durationStr,
                "reason", reason
        ));

        plugin.getWarningManager().sendWarningGUI(target, reason, issuer, durationStr);
        return true;
    }

    /**
     * Parses a duration string with a suffix and returns the expiry time.
     *
     * @param input Duration string, e.g., "5m" or "2h".
     * @return LocalDateTime representing the expiry, or null if invalid.
     */
    private LocalDateTime parseDuration(String input) {
        if (input.length() < 2) return null;

        char unit = input.charAt(input.length() - 1);
        String numberPart = input.substring(0, input.length() - 1);

        int amount;
        try {
            amount = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            return null;
        }

        return switch (unit) {
            case 's' -> LocalDateTime.now().plusSeconds(amount);
            case 'm' -> LocalDateTime.now().plusMinutes(amount);
            case 'h' -> LocalDateTime.now().plusHours(amount);
            case 'd' -> LocalDateTime.now().plusDays(amount);
            case 'w' -> LocalDateTime.now().plusWeeks(amount);
            case 'y' -> LocalDateTime.now().plusYears(amount);
            default -> null;
        };
    }

}
