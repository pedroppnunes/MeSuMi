package robbery.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;

import static robbery.attribute.Attribute.PERK_AVOID_CAUGHT1;

/**
 * Handles the /busted command, which teleports a player to a random
 * "busted" location when caught during a robbery or heist attempt.
 * <p>
 * Players with certain ranks have a small chance to avoid being teleported.
 * Teleportation locations are predefined and stored internally.
 * A short cooldown prevents multiple rapid activations of the command.
 * </p>
 */
public class Busted implements CommandExecutor {

    private static final Random random = new Random();
    private final List<int[]> busted = new ArrayList<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Robbery main;

    /**
     * Mapping of ranks to their chance (percentage) to avoid being busted.
     * Ranks not listed here have no chance to avoid teleportation.
     */
    private final Map<String, Integer> rankSaveChances = Map.of(
            "rank3", 0,
            "rank4", 10,
            "rank5", 15,
            "rank6", 20,
            "rank7", 25
    );

    /** Cooldown in milliseconds between consecutive uses of the command per player. */
    private static final long COOLDOWN_MS = 2000;

    /**
     * Initializes the Busted command, including predefined teleport locations.
     *
     * @param main The main plugin instance (not used in this class, but included for consistency).
     */
    public Busted(Robbery main) {
        busted.add(new int[]{20054, 102, 19965});
        busted.add(new int[]{20054, 102, 19977});
        busted.add(new int[]{20054, 102, 19971});
        busted.add(new int[]{20059, 102, 19965});
        busted.add(new int[]{20059, 102, 19971});
        busted.add(new int[]{20059, 102, 19977});
        busted.add(new int[]{20069, 102, 19965});
        busted.add(new int[]{20069, 102, 19971});
        busted.add(new int[]{20069, 102, 19977});
        busted.add(new int[]{20074, 102, 19977});
        busted.add(new int[]{20074, 102, 19971});
        busted.add(new int[]{20074, 102, 19965});
        busted.add(new int[]{20074, 106, 19971});
        busted.add(new int[]{20056, 106, 19976});
        busted.add(new int[]{20064, 111, 19960});
        this.main = main;
    }

    /**
     * Executes the /busted command.
     * <p>
     * Usage: /busted [player]
     * If a player name is specified, that player is targeted; otherwise, the command defaults to the sender (if allowed).
     * Teleports the player to a random predefined location or a store-specific location depending on rank and chance.
     * </p>
     *
     * @param sender  the command sender
     * @param command the command object
     * @param label   the command alias used
     * @param args    optional arguments; args[0] can be the target player name
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;

        if (args.length > 0) {
            player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline()) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }
            if (sender instanceof Player && !((Player) sender).getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }

        assert player != null;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);
            if ((now - lastUsed) < COOLDOWN_MS) {
                return true;
            }
        }

        cooldowns.put(uuid, now);

        PlayerData p = PlayerDataManager.getPlayerData(player);

        if (p.getBackpack().getSize() == 0)
            return true;

        String rank = p.getRank();
        double attribute = p.getPerkValue(PERK_AVOID_CAUGHT1);
        int baseChance = rankSaveChances.getOrDefault(rank, 0);
        if (baseChance > 0 || attribute > 0) {
            int chance = Math.min(100, (int)(baseChance + attribute));
            int roll = random.nextInt(100) + 1;
            if (roll <= chance) {
                Messages.sendTitle(player, "command.busted.lucky.title", "command.busted.lucky.subtitle", 10, 60, 10);
                return true;
            }
            String storeName = KeyManager.getStoreNameR(p.getKey().getName());
            Location target = null;
            switch (Objects.requireNonNull(storeName).toLowerCase()) {
                case "store1":
                case "store2":
                    target = new Location(player.getWorld(), 20116.500, 101, 20027.500);
                    break;
                case "store3":
                case "store4":
                    target = new Location(player.getWorld(), 20213.500, 101, 20007.500);
                    break;
                case "store5":
                case "store6":
                case "store7":
                    target = new Location(player.getWorld(), 20295.500, 101, 20095.500);
                    break;
                case "store8":
                case "store9":
                case "store10":
                case "store11":
                    target = new Location(player.getWorld(), 20181.500, 101, 20187.500);
                    break;
                case "store12":
                    target = new Location(player.getWorld(), 20090.500,101,20231.500);
                    break;
                default:
                    break;
            }
            if(target != null){
                player.teleport(target);
                Messages.sendTitle(player, "command.busted.busted-title", "", 10, 60, 10);
                p.busted();
                main.getQuestService().onPlayerBusted(p);
                return true;
            }
        }

        p.busted();
        main.getQuestService().onPlayerBusted(p);
        int[] coords = busted.get(getRandomLocation());
        player.teleport(new Location(player.getWorld(), coords[0], coords[1], coords[2]));
        Messages.sendTitle(player, "command.busted.busted-title", "", 10, 60, 10);

        return true;
    }

    /**
     * Returns a random index from the predefined busted locations list.
     *
     * @return random index of a busted location
     */
    private int getRandomLocation() {
        return random.nextInt(busted.size());
    }
}
