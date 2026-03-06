package robbery.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.mechanics.InventoryManager;
import robbery.messages.Messages;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the /hp (Hide Players) command and manages player visibility.
 * <p>
 * Players can toggle whether other non-operator players are visible to them.
 * This class also listens for world changes to maintain proper visibility and update the
 * corresponding inventory indicator (dye).
 * </p>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>/hp - Toggles the visibility of other players (non-OPs) for the sender.</li>
 * </ul>
 */
public class HidePlayers implements CommandExecutor, Listener {

    private final Robbery main;
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    /**
     * Creates a new HidePlayers manager.
     *
     * @param main the main plugin instance
     */
    public HidePlayers(Robbery main) {
        this.main = main;
    }

    /**
     * Checks if a player is currently hiding other players.
     *
     * @param uuid the UUID of the player
     * @return true if the player has hidden others, false otherwise
     */
    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    /**
     * Sets the hidden state for a player and updates their inventory dye accordingly.
     *
     * @param player the player to update
     * @param hidden true to hide other players, false to show
     */
    public void setHidden(Player player, boolean hidden) {
        UUID uuid = player.getUniqueId();
        if (hidden) hiddenPlayers.add(uuid);
        else hiddenPlayers.remove(uuid);
        InventoryManager.updateHideDye(player, hidden);
    }

    /**
     * Executes the /hp command.
     * Toggles the visibility of all non-OP players for the sender.
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the command label used
     * @param args    command arguments
     * @return true if executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player senderPlayer)) return true;

        UUID uuid = senderPlayer.getUniqueId();
        boolean nowHidden = !hiddenPlayers.contains(uuid);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(senderPlayer)) {
                if (nowHidden) {
                    if (!target.isOp()) senderPlayer.hidePlayer(main, target);
                } else {
                    senderPlayer.showPlayer(main, target);
                }
            }
        }

        setHidden(senderPlayer, nowHidden);
        if (nowHidden) {
            Messages.send(senderPlayer, "command.hideplayers.hidden");
        } else {
            Messages.send(senderPlayer, "command.hideplayers.visible");
        }

        return true;
    }

    /**
     * Handles world changes for a player to maintain proper visibility and inventory state.
     *
     * @param player the player who changed world
     */
    public void handleWorldChange(Player player) {
        UUID uuid = player.getUniqueId();
        if (player.getWorld().getName().equals("world")) {
            InventoryManager.updateHideDye(player, hiddenPlayers.contains(uuid));
        }
    }

    /**
     * Listens for PlayerChangedWorldEvent and updates hidden status if needed.
     *
     * @param event the world change event
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        handleWorldChange(player);
    }
}
