package robbery.skilltree;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.messages.Messages;

public class SkillCommand implements CommandExecutor {
    private final SkillService service;
    private final SkillTreeGUI gui;
    private final SkillTreeConfig cfg;

    public SkillCommand(SkillService svc, SkillTreeGUI gui, SkillTreeConfig cfg) {
        this.service = svc;
        this.gui = gui;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            Messages.send(sender,"global.player-only");
            return true;
        }
        if (args.length == 0) {
            gui.open(p);
            return true;
        }
        return false;
    }
}
