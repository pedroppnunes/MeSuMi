package robbery.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;

/**
 * Handles the /nv or /nightvision command, which toggles
 * the Night Vision effect for the executing player.
 */
public class NightVision implements CommandExecutor {

    /**
     * Constructs a new NightVision command.
     *
     * @param main the main plugin instance
     */
    public NightVision(Robbery main) {
    }

    /**
     * Executes the /nv command.
     *
     * Toggles the Night Vision effect:
     * <ul>
     *     <li>If the player already has Night Vision, it is removed.</li>
     *     <li>If the player does not have Night Vision, it is applied indefinitely.</li>
     * </ul>
     *
     * @param sender the command sender (must be a player)
     * @param command the command object
     * @param label the command label
     * @param args command arguments
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            Messages.send(player, "command.nightvision.disabled");
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            Messages.send(player, "command.nightvision.enabled");
        }

        return true;
    }
}
