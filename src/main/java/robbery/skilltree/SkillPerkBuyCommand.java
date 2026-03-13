package robbery.skilltree;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.messages.Messages;

import java.util.Map;

public class SkillPerkBuyCommand implements CommandExecutor {

    private final SkillService skillService;
    private final SkillTreeConfig config;

    public SkillPerkBuyCommand(SkillService skillService, SkillTreeConfig config) {
        this.skillService = skillService;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p && !p.hasPermission("robbery.op")) {
            Messages.send(p, "global.no-permission");
            return true;
        }

        if (args.length != 2) {
            Messages.send(sender, "events.skilltree.usage");
            return true;
        }

        String playerName = args[0];
        String tierId = args[1].toLowerCase();

        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        SkillPerk tier = config.getTier(tierId);
        if (tier == null) {
            Messages.sendFormatted(sender, "events.skilltree.invalid_tier", Map.of("tier", tierId));
            return true;
        }
        if (!skillService.canUpgrade(target, tierId)) {
            Messages.send(target, "events.skilltree.not_enough_skillpoints");
            return true;
        }

        skillService.upgrade(target, tierId);

        return true;
    }
}