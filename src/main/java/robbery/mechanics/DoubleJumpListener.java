package robbery.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;

public class DoubleJumpListener implements Listener {

    private final Set<UUID> canDoubleJump = new HashSet<>();
    private final Map<UUID, Long> flyCooldowns = new HashMap<>();
    private final Set<UUID> temporaryFlightPlayers = new HashSet<>();
    private final Robbery main;

    public DoubleJumpListener(Robbery main) {
        this.main = main;
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        if (player.hasPermission("robbery.bypass")) {
            player.setAllowFlight(true);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFlightItem(item)) return;

        if (!player.hasPermission("robbery.rank7") || !player.getWorld().getName().equals("world")) {
            Messages.sendActionBar(player,"events.flight.no-permission");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownEnd = flyCooldowns.getOrDefault(uuid, 0L);

        if (currentTime < cooldownEnd) {
            long timeLeft = (cooldownEnd - currentTime) / 1000;
            Messages.sendActionBarFormatted(player, "events.flight.cooldown", "seconds", String.valueOf(timeLeft));
            event.setCancelled(true);
            return;
        }

        int flightDuration = 5;

        temporaryFlightPlayers.add(uuid);
        flyCooldowns.put(uuid, currentTime + 5 * 60 * 1000);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setLevel(flightDuration);
        player.setExp(1.0f);
        Messages.sendActionBar(player, "events.flight.start");
        event.setCancelled(true);

        final BukkitTask[] flightTaskHolder = new BukkitTask[1];
        flightTaskHolder[0] = Bukkit.getScheduler().runTaskTimer(main, new Runnable() {
            int secondsLeft = flightDuration;

            @Override
            public void run() {
                if (!temporaryFlightPlayers.contains(uuid)) {
                    cancelFlight(player);
                    flightTaskHolder[0].cancel();
                    return;
                }

                if (secondsLeft <= 0) {
                    cancelFlight(player);
                    temporaryFlightPlayers.remove(uuid);
                    flightTaskHolder[0].cancel();
                    return;
                }

                player.setLevel(secondsLeft);
                player.setExp(secondsLeft / (float) flightDuration);
                secondsLeft--;
            }

            private void cancelFlight(Player player) {
                player.setFlying(false);
                player.setAllowFlight(false);
                player.setLevel(0);
                player.setExp(0.0f);
                Messages.sendActionBar(player, "events.flight.end");
            }
        }, 0L, 20L);
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData p = PlayerDataManager.getPlayerData(player);
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("robbery.bypass") || temporaryFlightPlayers.contains(uuid)) {
            player.setAllowFlight(true);
        } else {
            if (player.isOnGround() && player.getWorld().getName().equalsIgnoreCase("world")) {
                if (player.hasPermission("robbery.rank7") && p.isDoubleJump()) {
                    player.setAllowFlight(true);
                    canDoubleJump.add(uuid);
                } else {
                    player.setAllowFlight(false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("robbery.bypass") || temporaryFlightPlayers.contains(uuid)) {
            event.setCancelled(false);
            return;
        }
        if (!player.hasPermission("robbery.rank7") || !player.getWorld().getName().equalsIgnoreCase("world")) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        if (canDoubleJump.contains(uuid)) {
            Vector direction = player.getLocation().getDirection().normalize();
            Vector jumpBoost = direction.multiply(1.0).setY(1.0);
            player.setVelocity(jumpBoost);
            canDoubleJump.remove(uuid);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction().toString().contains("RIGHT")) {
            ItemStack item = event.getItem();
            if (item == null || item.getType() != Material.LIME_DYE && item.getType() != Material.RED_DYE)
                return;

            if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName())
                return;

            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains("Hide Players") || displayName.contains("Unhide Players")) {
                player.performCommand("hp");
                event.setCancelled(true);
            }
        }
    }




    private boolean isFlightItem(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§bFlight");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        canDoubleJump.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        canDoubleJump.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        canDoubleJump.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }
}