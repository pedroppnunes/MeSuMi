package robbery.booster;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

/**
 * Command to activate a booster for a player.
 * <p>
 * Usage: /usebooster &lt;boosterName&gt; &lt;playerName&gt;
 * </p>
 * <p>
 * The command sets the specified booster for the target player using
 * {@link PlayerData#setBoosters(String, Player)} and notifies the sender
 * of the successful activation.
 * </p>
 */
public class UseBooster implements CommandExecutor {

    /**
     * Constructs a new UseBooster command.
     *
     * @param main The main plugin instance (Robbery).
     */
    public UseBooster(Robbery main) {
    }

    /**
     * Executes the /usebooster command.
     *
     * @param sender  The sender of the command.
     * @param command The command object.
     * @param label   The alias of the command used.
     * @param args    Command arguments. Expected: 2 arguments (boosterName, playerName).
     * @return true if the command was handled, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 2) {
            Messages.send(sender, "command.usebooster.usage");
            return false;
        }

        Player player = Bukkit.getPlayer(args[1]);
        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        String boosterName = args[0].toLowerCase();
        PlayerData p = PlayerDataManager.getPlayerData(player);
        p.setBoosters(boosterName, player);

        Messages.sendFormatted(sender, "command.usebooster.success", Map.of(
                "boostername", boosterName,
                "player", player.getName()
        ));
        return true;
    }

}
