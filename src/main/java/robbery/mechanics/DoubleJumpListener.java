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

import static robbery.attribute.Attribute.PERK_SPECIAL_DOUBLEJUMP;
import static robbery.attribute.Attribute.PERK_SPECIAL_FEATHERFLIGHT;

public class DoubleJumpListener implements Listener {

    private final Set<UUID> canDoubleJump = new HashSet<>();
    private final Map<UUID, Long> flyCooldowns = new HashMap<>();
    private final Set<UUID> temporaryFlightPlayers = new HashSet<>();
    private final Map<UUID, Long> doubleJumpCooldowns = new HashMap<>();
    private final Robbery main;

    public DoubleJumpListener(Robbery main) {
        this.main = main;
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData pd = PlayerDataManager.getPlayerData(player);

        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isFlightItem(item)) return;

        boolean hasRank7 = player.hasPermission("robbery.rank7");
        boolean hasPerk = pd != null && pd.getPerkValue(PERK_SPECIAL_FEATHERFLIGHT) == 1;

        if ((!hasRank7 && !hasPerk) || !player.getWorld().getName().equals("world")) {
            Messages.sendActionBar(player, "events.flight.no-permission");
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
        // 2 minutes for rank7, 5 minutes for perk
        long flightCooldown = hasRank7 ? 120_000L : 300_000L;

        temporaryFlightPlayers.add(uuid);
        flyCooldowns.put(uuid, currentTime + flightCooldown);

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

        if (temporaryFlightPlayers.contains(uuid)) {
            player.setAllowFlight(true);
        } else {
            if (player.isOnGround() && player.getWorld().getName().equalsIgnoreCase("world")) {
                if ((player.hasPermission("robbery.rank7") || p.getPerkValue(PERK_SPECIAL_DOUBLEJUMP) == 1) && p.isDoubleJump()) {
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

        PlayerData p = PlayerDataManager.getPlayerData(player);

        if (temporaryFlightPlayers.contains(uuid)) {
            event.setCancelled(false);
            return;
        }
        
        boolean hasRank7 = player.hasPermission("robbery.rank7");
        boolean hasPerk = p.getPerkValue(PERK_SPECIAL_DOUBLEJUMP) == 1; // Double jump uses DOUBLEJUMP perk
        
        if ((!hasRank7 && !hasPerk) || !player.getWorld().getName().equalsIgnoreCase("world")) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        if (canDoubleJump.contains(uuid)) {

            if (!player.hasPermission("robbery.rank7")) {
                long now = System.currentTimeMillis();
                long cooldownEnd = doubleJumpCooldowns.getOrDefault(uuid, 0L);

                if (now < cooldownEnd) {
                    long seconds = (cooldownEnd - now) / 1000;
                    Messages.sendActionBarFormatted(player, "events.doublejump.cooldown",
                            Map.of("seconds", String.valueOf(seconds)));
                    return;
                }

                doubleJumpCooldowns.put(uuid, now + 3000);
            }

            Vector direction = player.getLocation().getDirection().normalize();
            Vector jumpBoost = direction.multiply(1.2).setY(1.2);
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
        doubleJumpCooldowns.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        canDoubleJump.remove(event.getPlayer().getUniqueId());
        doubleJumpCooldowns.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        canDoubleJump.remove(event.getPlayer().getUniqueId());
        doubleJumpCooldowns.remove(event.getPlayer().getUniqueId());
        temporaryFlightPlayers.remove(event.getPlayer().getUniqueId());
    }
}