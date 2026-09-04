package robbery.skilltree;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.storeMastery.PlayerSkillTreeGUI;

public class SkillTreeCommand implements CommandExecutor {

    private final Robbery plugin;

    public SkillTreeCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        int page = label.equalsIgnoreCase("skilltreeup") ? 2 : 1;
        String targetName = viewer.getName();

        if (args.length == 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                targetName = args[0];
            }
        } else if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[0]);
                targetName = args[1];
            } catch (NumberFormatException e) {
                try {
                    page = Integer.parseInt(args[1]);
                    targetName = args[0];
                } catch (NumberFormatException ex) {
                    targetName = args[0];
                }
            }
        }

        if (page < 1) page = 1;

        boolean isSelf = targetName.equalsIgnoreCase(viewer.getName());
        if (!isSelf) {
            if (!viewer.hasPermission("robbery.op") && !viewer.isOp()) {
                viewer.sendMessage(Component.text("You do not have permission to view other players' skill trees.").color(NamedTextColor.RED));
                return true;
            }
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        PlayerData targetData = PlayerSkillTreeGUI.getOrLoadPlayerData(plugin, targetPlayer);

        if (targetData == null) {
            viewer.sendMessage(Component.text("Player data not found for " + targetName).color(NamedTextColor.RED));
            return true;
        }

        String displayName = (targetPlayer != null && targetPlayer.getName() != null) ? targetPlayer.getName() : targetName;
        plugin.getPlayerSkillTreeGUI().openGUI(viewer, targetData, displayName, targetPlayer != null ? targetPlayer.getUniqueId() : null, page);
        return true;
    }
}
