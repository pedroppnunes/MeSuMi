package robbery.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

/**
 * Handles the /store command which allows a player to teleport to a store location
 * based on their current rank/key.
 * <p>
 * Only players in the required world ("world") with the appropriate permissions
 * or OP status can use this command. The player must also have an empty backpack.
 * </p>
 * <p>
 * Store locations are predefined in the {@link #LOCS} map. Teleportation is done
 * according to the player's current key from {@link PlayerData#getKey()}.
 * </p>
 */
public class StoreTeleport implements CommandExecutor {

    /** Required world name for using this command */
    private static final String REQUIRED_WORLD = "world";

    /**
     * Predefined store locations mapped by store identifiers.
     * Uses hardcoded coordinates and yaw/pitch values.
     */
    private static final Map<String, Location> LOCS = Map.ofEntries(
            Map.entry("store1",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20124,101,20037,0,0)),
            Map.entry("store2",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20129,102,19984,180,0)),
            Map.entry("store3",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20175,101,20038,0,0)),
            Map.entry("store4",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20205,101,19989,180,0)),
            Map.entry("store5",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20259,100,20098,90,0)),
            Map.entry("store6",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20320,101,20021,-140,0)),
            Map.entry("store7",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20328,101,20133,-90,0)),
            Map.entry("store8",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20230,101,20201,0,0)),
            Map.entry("store9",  new Location(Bukkit.getWorld(REQUIRED_WORLD), 20188,101,20159,180,0)),
            Map.entry("store10", new Location(Bukkit.getWorld(REQUIRED_WORLD), 20180,101,20204,0,0)),
            Map.entry("store11", new Location(Bukkit.getWorld(REQUIRED_WORLD), 20130,101,20152,140,0)),
            Map.entry("store12", new Location(Bukkit.getWorld(REQUIRED_WORLD), 20090,101,20231,0,-10))
    );

    /** Permissions required to use the /store command */
    private static final String[] ALLOWED_PERMS = {
            "robbery.rank5", "robbery.rank6", "robbery.rank7"
    };

    /**
     * Executes the /store command to teleport a player to their store location.
     *
     * @param sender the command sender (must be a player)
     * @param cmd the command object
     * @param lbl the command label
     * @param args command arguments (ignored)
     * @return true if the command executed or a message was sent to the player
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String lbl, String @NotNull [] args) {

        if (!(sender instanceof Player p)) {
            Messages.send(sender, "global.player-only");
            return true;
        }

        // Check permissions
        boolean permitted = false;
        for (String perm : ALLOWED_PERMS) {
            if (p.hasPermission(perm)) {
                permitted = true;
                break;
            }
        }
        if (!permitted && !p.isOp()) {
            Messages.send(p, "command.store.no-permission");
            return true;
        }

        // Check world
        if (!p.getWorld().getName().equalsIgnoreCase(REQUIRED_WORLD)) {
            Messages.send(p, "command.store.wrong-world");
            return true;
        }

        // Check empty backpack
        PlayerData data = PlayerDataManager.getPlayerData(p);
        if (data.getBackpack() != null && data.getBackpack().getSize() != 0) {
            Messages.send(p, "command.store.no-backpack");
            return true;
        }

        // Get store location from current key
        Keys current = data.getKey();
        String keyId = robbery.keys.KeyManager.getStoreNameR(current.getName());

        Location loc = LOCS.get(keyId);
        if (loc == null) {
            Messages.send(p, "command.store.unknown-store");
            return true;
        }

        // Teleport player
        p.teleport(loc);
        Messages.sendFormatted(p, "command.store.teleported", "store", current.getName());
        return true;
    }
}
