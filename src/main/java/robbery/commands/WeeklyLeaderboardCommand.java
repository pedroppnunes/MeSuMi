package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;

/**
 * Handles the /weeklyleaderboard command, which generates and displays
 * the weekly leaderboard to the executing player.
 * <p>
 * Only players with the "robbery.op" permission can execute this command.
 * The command triggers the WeeklyLeaderboardTask to generate and send
 * the leaderboard.
 * </p>
 */
public class WeeklyLeaderboardCommand implements CommandExecutor {

    private final Robbery main;

    /**
     * Constructs a new WeeklyLeaderboardCommand.
     *
     * @param main The main Robbery plugin instance.
     */
    public WeeklyLeaderboardCommand(Robbery main) {
        this.main = main;
    }

    /**
     * Executes the /weeklyleaderboard command.
     *
     * @param sender  The sender of the command (must be a Player).
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    Command arguments (not used in this command).
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.player-only");
            return true;
        }
        if (!p.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        main.getWeeklyLeaderboardTask().generateAndSendLeaderboard();

        return true;
    }
}
