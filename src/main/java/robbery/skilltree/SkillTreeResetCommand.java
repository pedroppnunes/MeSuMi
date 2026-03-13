package robbery.skilltree;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;

public class SkillTreeResetCommand implements CommandExecutor {
    private final Robbery plugin;
    private final SkillTreeConfig config;

    public SkillTreeResetCommand(Robbery plugin, SkillTreeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("robbery.op")) {
            Messages.send(sender,"global.no-permission");
            return true;
        }

        String targetName = args[0];

        Player target = Bukkit.getPlayerExact(targetName);
        PlayerData pd = null;
        if (target != null) {
            pd = PlayerDataManager.getPlayerData(target);
        }

        if (pd == null) {
            return true;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("add")) {

            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number.");
                return true;
            }

            pd.addResetSkillTreePoints(amount);

            sender.sendMessage("§aAdded §e" + amount + "§a reset points to §e" + target.getName());
            Messages.sendFormatted(target, "command.skilltree.resetpoints-added", Map.of("amount",String.valueOf(amount)));

            return true;
        }

        if (pd.getResetSkillTreePoints() <= 0) {
            Messages.send(target,"command.skilltree.no-reset");
            return true;
        }

        int totalRefund = 0;
        Map<String, Integer> levels = pd.getAllSkillTreeLevels();

        for (Map.Entry<String, Integer> e : new HashMap<>(levels).entrySet()) {
            String perkId = e.getKey();
            int currentLevel = e.getValue();
            if (currentLevel <= 0) continue;

            SkillPerk perk = config.getTier(perkId);
            if (perk == null) continue;

            for (int lvl = 0; lvl < currentLevel; lvl++) {
                try {
                    int cost = perk.costForNext(lvl);
                    totalRefund += cost;
                } catch (Exception ex) {
                    plugin.getLogger().warning("Could not compute cost for perk " + perkId + " level " + lvl + ": " + ex.getMessage());
                }
            }

            // now safe to modify
            pd.setSkillTreeLevel(perkId, 0);
            try {
                pd.setPerkValue(perkId, perk.valueForLevel(0));
            } catch (Exception ignored) {
                pd.setPerkValue(perkId, 0.0);
            }
        }

        boolean consumed = pd.consumeResetSkillTreePoint();
        if (!consumed) {
            sender.sendMessage("§cFailed to consume reset token (concurrent modification?).");
            return true;
        }

        if (totalRefund > 0) {
            pd.addSkillPoints(totalRefund);
        }

        sender.sendMessage("§aSkilltree reset for " + targetName + ". Refunded skillpoints: §e" + totalRefund + "§a. Reset tokens left: §e" + pd.getResetSkillTreePoints());

        if (target.isOnline()) {
            Messages.send(target, "comamnd.skilltree.success");
        }

        return true;
    }
}