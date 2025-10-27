package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

public class StopBoosterCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            Messages.send(sender, "command.stopbooster.usage");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData data = PlayerDataManager.getPlayerData(player);
        if (data == null) {
            return true;
        }

        if (data.isBoostersPaused()) {
            data.resumeBoosters(player);
            Messages.send(player, "boosters.resumed");
        } else {
            data.stopBoosters(player);
            Messages.send(player, "boosters.paused");
        }

        return true;
    }
}
