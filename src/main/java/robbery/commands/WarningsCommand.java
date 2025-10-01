package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;
import robbery.warnings.WarningManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the /warnings command, which lists all active warnings for the executing player.
 * <p>
 * Only players can execute this command. It will mark expired warnings as expired,
 * retrieve active warnings from the WarningManager, and display them formatted
 * with date, reason, issuer, and expiry information.
 * </p>
 */
public class WarningsCommand implements CommandExecutor {

    private final WarningManager warningManager;

    /**
     * Constructs a new WarningsCommand.
     *
     * @param warningManager The WarningManager responsible for storing player warnings.
     */
    public WarningsCommand(WarningManager warningManager) {
        this.warningManager = warningManager;
    }

    /**
     * Executes the /warnings command.
     *
     * @param sender  The sender of the command (must be a Player).
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    Command arguments (none expected for this command).
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        UUID uuid = p.getUniqueId();
        // Remove expired warnings and save changes
        warningManager.markExpiredAndSave(uuid);

        // Retrieve active warnings
        List<Map<String, String>> activeWarnings = warningManager.getActiveWarnings(uuid);

        if (activeWarnings.isEmpty()) {
            Messages.send(p, "command.warnings.none");
            return true;
        }

        Messages.send(p, "command.warnings.header");

        // Display each active warning with formatted details
        for (Map<String, String> warning : activeWarnings) {
            Messages.sendFormatted(p, "command.warnings.entry_start", Map.of("date", warning.get("start_date")));
            Messages.sendFormatted(p, "command.warnings.entry_reason", Map.of("reason", warning.get("reason")));
            Messages.sendFormatted(p, "command.warnings.entry_issuer", Map.of("issuer", warning.get("issuer")));
            Messages.sendFormatted(p, "command.warnings.entry_expires", Map.of("expire", warning.get("expires_at")));
            p.sendMessage("");
        }

        return true;
    }
}
