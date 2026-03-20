package robbery.quest;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.quest.QuestService;

public class AcceptAllDailyQuestsCommand implements CommandExecutor {

    private final Robbery plugin;
    private final QuestService questService;

    public AcceptAllDailyQuestsCommand(Robbery plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender.hasPermission("robbery.op") || !(sender instanceof Player))) {
            Messages.send(sender,"global.no-permission");
            return true;
        }

        if (args.length != 1) {
            Messages.send(sender,"command.acceptalldaily.usage");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) {
            return true;
        }

        if (!pd.getAcceptedDailyQuests().isEmpty()) {
            Messages.send(sender, "command.acceptalldaily.already-accepted");
            return true;
        }

        questService.pick3DailyQuestsFor(pd);

        for (String questId : pd.getOfferedDailyQuests()) {
            if (!pd.getAcceptedDailyQuests().contains(questId)) {
                questService.playerAcceptQuest(pd, questId);
            }
        }

        Messages.send(target, "command.acceptalldaily.confirm");

        return true;
    }
}
