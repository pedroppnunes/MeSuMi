package robbery.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;

/**
 * Handles the /lobby command.
 * <p>
 * This command connects the player to the "lobby" server via BungeeCord.
 * Only players can execute this command.
 * </p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>/lobby - Connects the player to the lobby server.</li>
 * </ul>
 */
public class Lobby implements CommandExecutor {

    private final Robbery main;

    /**
     * Creates a new Lobby command executor.
     *
     * @param main the main plugin instance
     */
    public Lobby(Robbery main) {
        this.main = main;
    }

    /**
     * Executes the /lobby command.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the command label
     * @param args    command arguments (ignored)
     * @return true if the command was handled
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true; // Only players can execute this command
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");

        player.sendPluginMessage(main, "BungeeCord", out.toByteArray());

        return true;
    }
}
