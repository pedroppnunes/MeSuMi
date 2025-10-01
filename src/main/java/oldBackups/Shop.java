/*package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.Robbery;

public class Shop implements CommandExecutor {

    private Robbery main;

    public Shop(Robbery main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        if(!player.getWorld().getName().equals("SuperiorWorld") || player.getWorld().getName().equals("world")){
            player.sendMessage("§cYou can't use that here.");
            return true;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open valueblocks_menu " + player.getName());

        return true;
    }
}


 */