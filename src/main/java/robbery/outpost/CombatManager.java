package robbery.outpost;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages combat tagging for players in the Outpost world.
 * <p>
 * This class prevents combat logging, blocks commands while in combat,
 * and ensures players from the same island cannot damage each other.
 * Tagged players are automatically untagged after a set duration.
 */
public class CombatManager implements Listener {

    private final Map<UUID, Long> taggedPlayers = new HashMap<>();
    private final Robbery main;
    private final String[] allowedCommands = {"/r", "/ho top", "/msg", "/tell","/reply","message"};

    /**
     * Constructs a new CombatManager instance and starts the combat timer task.
     *
     * @param main The main Robbery plugin instance.
     */
    public CombatManager(Robbery main){
        this.main = main;
        startCombatTimer();
    }

    /**
     * Tags a player as being in combat.
     * Records the current system time as the last tag time.
     *
     * @param player The player to tag.
     */
    public void tag(Player player) {
        taggedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Checks if a player is currently tagged (in combat).
     *
     * @param player The player to check.
     * @return True if the player is tagged, false otherwise.
     */
    public boolean isTagged(Player player) {
        Long lastHit = taggedPlayers.get(player.getUniqueId());
        long tagDuration = 30 * 1000; // 30 seconds
        return lastHit != null && (System.currentTimeMillis() - lastHit) < tagDuration;
    }

    /**
     * Untags a player, removing them from the combat state.
     *
     * @param player The player to untag.
     */
    public void untag(Player player) {
        taggedPlayers.remove(player.getUniqueId());
    }

    /**
     * Handles players quitting while in combat.
     * If the player is tagged, they die and receive a warning for combat logging.
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if(main.getIsBackup()){
            return;
        }
        if (isTagged(player)) {
            player.setHealth(0);
            main.getWarningManager().addWarning(player.getUniqueId(), "Combat logging", "Server","3h");
        }
        untag(player);
    }

    /**
     * Handles damage between players.
     * Prevents players from the same island from hurting each other.
     * Tags both attacker and victim if damage occurs in the Outpost world.
     *
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        // Direct hit
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        }
        // Projectile hit (arrow, trident, etc.)
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
            }
        }

        if (attacker == null) return;

        if (!attacker.getWorld().getName().equalsIgnoreCase("outpost")) return;

        if (hasBypass(attacker) || hasBypass(victim)) return;

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(attacker);
        Island atkIsland = superiorPlayer.getIsland();
        SuperiorPlayer superiorPlayer2 = SuperiorSkyblockAPI.getPlayer(victim);
        Island vicIsland = superiorPlayer2.getIsland();

        if (atkIsland != null && vicIsland != null &&
                atkIsland.getOwner().getUniqueId().equals(vicIsland.getOwner().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        tag(attacker);
        tag(victim);
    }

    /**
     * Blocks commands for players currently tagged in combat.
     *
     * @param event PlayerCommandPreprocessEvent
     */
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().getName().equalsIgnoreCase("outpost")) return;

        if (!isTagged(player) || hasBypass(player)) return;

        String message = event.getMessage().toLowerCase();

        for (String cmd : allowedCommands) {
            if (message.equals(cmd)) {
                return;
            }
        }

        Messages.send(player, "events.combat.command_blocked");
        event.setCancelled(true);
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killed = event.getEntity();
        Player killer = killed.getKiller();
        event.deathMessage(null);
        if (!killed.getWorld().getName().equalsIgnoreCase("outpost")) {
            return;
        }

        if (killer == null) return;


        for (Player player : Bukkit.getOnlinePlayers()) {
                Messages.sendFormatted(player,"events.combat.death-message",Map.of("killed", killed.getName(), "killer", killer.getName()));
        }
    }

    /**
     * Starts a repeating task that updates combat timers.
     * Sends an action bar countdown to players still in combat.
     * Untags players automatically after the timer expires.
     */
    private void startCombatTimer() {
        Bukkit.getScheduler().runTaskTimer(main, () -> {
            long currentTime = System.currentTimeMillis();
            long tagDuration = 30 * 1000; // 10 seconds for countdown display

            taggedPlayers.entrySet().removeIf(entry -> {
                UUID uuid = entry.getKey();
                long lastTagTime = entry.getValue();
                long timeLeft = tagDuration - (currentTime - lastTagTime);

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && timeLeft > 0) {
                    long secondsLeft = timeLeft / 1000;
                    Messages.sendActionBarFormatted(player, "events.combat.tag_timer", "time", String.valueOf(secondsLeft));
                    return false;
                }

                return true;
            });
        }, 0L, 20L);
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission("robbery.op") || player.hasPermission("robbery.bypass");
    }
}
