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

import java.util.Calendar;
import java.util.TimeZone;

public class QuestsCommand implements CommandExecutor {

    private final Robbery main;

    public QuestsCommand(Robbery main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player player && !player.hasPermission("robbery.op")) {
            Messages.send(sender, "global.no-permission");
            return true;
        }

        if (args.length < 2) {
            Messages.send(sender, "command.quests.usage");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        String source = args[1].toLowerCase();

        if (target == null) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return true;

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        if (pd.getLastResetDay() != currentDayOfYear) {
            pd.setTalkedToQuestNPC(false);
        }

        if (source.equals("npc")) {
            pd.setTalkedToQuestNPC(true);
        }
        else if (source.equals("menu")) {
            if (!pd.hasTalkedToQuestNPC()) {
                Messages.send(target, "command.quests.must-talk-to-npc");
                return true;
            }
        }

        main.getQuestService().pick3DailyQuestsFor(pd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open quests_menu " + target.getName());

        return true;
    }
}