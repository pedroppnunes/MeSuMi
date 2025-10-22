package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;

public class LoadBackup implements CommandExecutor {

    private final Robbery main;

    public LoadBackup(Robbery main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        main.loadBackupItems();
        Messages.send(player, "command.load.success");

        return true;
    }
}
