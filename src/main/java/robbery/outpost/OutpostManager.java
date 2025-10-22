package robbery.outpost;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import robbery.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.clip.placeholderapi.util.Msg.broadcast;

public class OutpostManager {
    private final Robbery plugin;
    private final OutpostRegion region;
    private final int totalTime;
    private UUID currentIsland;
    private double progress;

    private UUID ownerIsland = null;
    private UUID lastBoosterIsland;
    private BukkitTask boostExpireTask;
    private long ownershipExpiryMillis = 0L;
    private boolean captureAnnounced = false;
    private boolean neutralizeAnnounced = false;


    private double moneyMultiplier = 1.0;
    private String perk2Type = "None";
    private int perk2Value = 0;


    public OutpostManager(Robbery plugin, OutpostRegion region, int captureTimeSeconds) {
        this.plugin = plugin;
        this.region = region;
        this.totalTime = captureTimeSeconds;
        this.currentIsland = null;
        this.progress = 0;
        this.lastBoosterIsland = null;
        this.boostExpireTask = null;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            public void run() {
                tickCapture();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        new BukkitRunnable() {
            public void run() {
                rollOutpostPerks();
            }
        }.runTaskTimer(plugin, 0L, 4 * 60 * 60 * 20);
    }

    private void tickCapture() {
        // Keep the previous progress so we can detect threshold crossings (e.g. from 9 -> 10).
        double oldProgress = progress;

        if (ownerIsland != null && System.currentTimeMillis() >= ownershipExpiryMillis) {
            expireBoost();
        }

        Map<UUID, Island> present = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(p.getLocation())) continue;
            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(p);
            Island isle = sp.getIsland();
            if (isle == null) {
                // If a player inside the region doesn't have an island, show actionbar and stop processing this tick.
                Messages.sendActionBar(p, "events.outpost.need_island");
                return;
            }
            present.putIfAbsent(isle.getOwner().getUniqueId(), isle);
        }

        if (present.size() == 1) {
            UUID capturingIsland = present.keySet().iterator().next();

            // If the owner island is the same as the capturing island, show remaining owned time to owners.
            if (ownerIsland != null && ownerIsland.equals(capturingIsland)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!region.isInside(p.getLocation())) continue;
                    SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(p);
                    Island pi = sp.getIsland();
                    if (pi != null && pi.getOwner().getUniqueId().equals(capturingIsland)) {
                        long left = ownershipExpiryMillis - System.currentTimeMillis();
                        String hhmmss = formatMillis(Math.max(0, left));
                        Messages.sendActionBarFormatted(p, "events.outpost.owned_time_left", "time", hhmmss);
                    }
                }
                return;
            }

            // Start or switch capturing island, or decrease progress if another island tries to take over.
            if (currentIsland == null) {
                currentIsland = capturingIsland;
                progress = 1.0;
            } else if (!currentIsland.equals(capturingIsland)) {
                if (progress > 0) {
                    progress--;
                } else {
                    currentIsland = capturingIsland;
                    progress = 1.0;
                }
            } else {
                // Same island continues capturing; count how many members from that island are inside
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!region.isInside(p.getLocation())) continue;
                    SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(p);
                    Island pi = sp.getIsland();
                    if (pi != null && pi.getOwner().getUniqueId().equals(capturingIsland)) count++;
                }

                if (count > 0) {
                    double increment = 1.0 * count;
                    if (ownerIsland == null) {
                        // Normal capture: max at totalTime
                        progress = Math.min(progress + increment, totalTime);
                    } else {
                        // Neutralizing an owned outpost: needs up to totalTime * 2 (first totalTime to neutralize, then totalTime to recapture)
                        progress = Math.min(progress + increment, totalTime * 2);
                    }
                }
            }

            // ---- Announcement logic: trigger once when crossing the threshold (oldProgress < 10 && progress >= 10)
            // Use currentIsland (UUID) to fetch island name for the message.
            if (oldProgress < 10 && progress >= 10 && currentIsland != null) {
                SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(currentIsland);
                Island capturingIsle = sp != null ? sp.getIsland() : null;
                if (capturingIsle != null) {
                    if (ownerIsland == null) {
                        // This is a capture attempt (outpost is unclaimed) -> announce capturing
                        if (!captureAnnounced) {
                            Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.capturing_broadcast",
                                    Map.of("island_name", capturingIsle.getName())));
                            captureAnnounced = true;
                        }
                    } else {
                        // This is a neutralizing attempt against an owned outpost -> announce neutralizing
                        if (!neutralizeAnnounced) {
                            Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.neutralizing_broadcast",
                                    Map.of("island_name", capturingIsle.getName())));
                            neutralizeAnnounced = true;
                        }
                    }
                }
            }
        }
        else if (present.size() > 1) {
            // Contested: show actionbar to players in region and stop processing capture this tick.
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (region.isInside(p.getLocation())) {
                    Messages.sendActionBar(p, "events.outpost.contested");
                }
            }
            return;
        }
        else {
            // No players present: decay progress; when it reaches 0, clear current island
            if (progress > 0) {
                progress--;
            } else {
                currentIsland = null;
            }
        }

        // Send progress bar to players inside the region (account for neutralize phase showing second half)
        if (currentIsland != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!region.isInside(p.getLocation())) continue;

                if (ownerIsland == null) {
                    sendProgressBar(p, progress, totalTime);
                } else {
                    if (progress < totalTime) {
                        // neutralizing phase (0..totalTime)
                        sendProgressBar(p, progress, totalTime);
                    } else {
                        // recapture phase (progress - totalTime .. totalTime)
                        sendProgressBar(p, progress - totalTime, totalTime);
                    }
                }
            }
        }

        // ---- Completion & neutralization handling
        if (currentIsland != null) {
            if (ownerIsland == null && progress >= totalTime) {
                // Capture completed on an unowned outpost
                completeCapture(currentIsland);
                ownerIsland = currentIsland;
                currentIsland = null;
                progress = 0;

                // Reset neutralize flag because this outpost is now owned (future neutralizes may happen)
                neutralizeAnnounced = false;
                // captureAnnounced is logically reset in next block (or can remain true until currentIsland==null clears)
            }
            else if (ownerIsland != null) {
                // We're dealing with an owned outpost being neutralized / recaptured
                if (progress >= totalTime && ownerIsland != null) {
                    // First stage completed: the outpost was neutralized (ownership lost).
                    // Announce neutralized by the capturing island (preferably include island name).
                    SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(currentIsland);
                    Island capturingIsle = sp != null ? sp.getIsland() : null;
                    if (capturingIsle != null) {
                        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.neutralized_broadcast",
                                Map.of("island_name", capturingIsle.getName())));
                    } else {
                        // Fallback generic neutralized message
                        Bukkit.broadcastMessage(Messages.get("events.outpost.neutralized_broadcast"));
                    }

                    // Clear owner so region becomes neutral; allow recapture to proceed.
                    ownerIsland = null;

                    // Reset capture announcement so that the next recapture crossing 10 can announce "capturing".
                    captureAnnounced = false;
                }

                if (progress >= totalTime * 2) {
                    // Second stage completed: the capturing island has fully captured the outpost (reclaimed).
                    completeCapture(currentIsland);
                    ownerIsland = currentIsland;
                    currentIsland = null;
                    progress = 0;

                    // Reset announcement flags because capture cycle completed.
                    neutralizeAnnounced = false;
                }
            }
        }

        // When there's no active capturing island, reset announcements so later attempts can trigger messages again.
        if (currentIsland == null) {
            captureAnnounced = false;
            neutralizeAnnounced = false;
        }
    }


    private void completeCapture(UUID islandOwner) {
        if (boostExpireTask != null) {
            boostExpireTask.cancel();
            boostExpireTask = null;
        }

        removePerksFromIsland();

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(islandOwner);
        Island is = superiorPlayer != null ? superiorPlayer.getIsland() : null;

        if (is == null) {
            Bukkit.getLogger().warning("Could not find island for UUID: " + islandOwner);
            return;
        }

        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.capture_broadcast", Map.of("island_name", is.getName())));

        applyPerksToIsland(islandOwner);

        lastBoosterIsland = islandOwner;
        long delayTicks = 4L * 60 * 60 * 20;
        long delayMillis = delayTicks * 50;

        ownershipExpiryMillis = System.currentTimeMillis() + delayMillis;
        ownerIsland = islandOwner;

        if (boostExpireTask != null) boostExpireTask.cancel();
        boostExpireTask = new BukkitRunnable() {
            @Override
            public void run() {
                expireBoost();
            }
        }.runTaskLater(plugin, delayTicks);
    }

    private String formatMillis(long ms) {
        long totalSec = ms / 1000;
        long hours    = totalSec / 3600;
        long minutes  = (totalSec % 3600) / 60;
        long seconds  = totalSec % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }


    private void expireBoost() {
        if (lastBoosterIsland != null) {
            removePerksFromIsland();

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_border"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_title"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_desc"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_border")+"\u200B");
            Bukkit.broadcastMessage("");
        }
        lastBoosterIsland = null;
        ownerIsland = null;
        ownershipExpiryMillis = 0L;
        boostExpireTask = null;
    }

    private void sendProgressBar(Player player, double progress, int max) {
        int totalBars = 10;
        double fraction = progress / max;
        int filledBars = (int) (fraction * totalBars);
        int percentage = (int) (fraction * 100);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            bar.append(i < filledBars ? "§a█" : "§c█");
        }

        Messages.sendActionBarFormatted(player, "events.outpost.progress_bar", Map.of(
                "bar", bar.toString(),
                "percent", String.valueOf(percentage)
        ));
    }
    
    public UUID getCurrentIsland() {
        return ownerIsland;
    }
    public String getStatusTitle() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.status-title-unclaimed");

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(ownerIsland);
        Island island = sp != null ? sp.getIsland() : null;

        String islandName = island != null ? island.getName() :
                Messages.get("events.outpost.holder-island-name-none");

        String titleTemplate = Messages.get("events.outpost.status-title-controlled");
        return titleTemplate.replace("%island%", islandName);
    }

    public String getStatusLoreLine1() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.status-lore-time-left")
                    .replace("%time%", Messages.get("events.outpost.holder-island-name-none"));

        long left = ownershipExpiryMillis - System.currentTimeMillis();
        String formattedTime = formatMillis(Math.max(0, left));

        String loreTemplate = Messages.get("events.outpost.status-lore-time-left");
        return loreTemplate.replace("%time%", formattedTime);
    }

    public String getHolderIslandName() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.holder-island-name-none");

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(ownerIsland);
        Island island = sp != null ? sp.getIsland() : null;

        return island != null ? island.getName() : Messages.get("events.outpost.holder-island-name-none");
    }

    public String getHolderLeaderName() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.holder-leader-name-none");

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(ownerIsland);
        if (sp == null)
            return Messages.get("events.outpost.holder-leader-name-none");

        return sp.getName();
    }

    public String getOutpostMaterial(Player player) {
        if (ownerIsland == null)
            return Messages.get("events.outpost.outpost-material-unclaimed");

        Island island = SuperiorSkyblockAPI.getPlayer(player).getIsland();
        if (island == null || !island.getOwner().getUniqueId().equals(ownerIsland)) {
            return Messages.get("events.outpost.outpost-material-not-owner");
        }

        return Messages.get("events.outpost.outpost-material-owner");
    }

    private void rollOutpostPerks() {
        double roll = Math.random();
        if (roll < 0.25) {
            moneyMultiplier = 0.75;
        } else if (roll < 0.75) {
            moneyMultiplier = 1.0;
        } else {
            moneyMultiplier = 1.25;
        }

        int typeRoll = (int) (Math.random() * 3);
        String[] types = {"Skillpoint Chance", "Booster Chance", "Speed"};
        perk2Type = types[typeRoll];

        double perkRoll = Math.random();
        if (perkRoll < 0.25) {
            perk2Value = perk2Type.equals("Speed") ? 10 : 5;
        } else if (perkRoll < 0.75) {
            perk2Value = perk2Type.equals("Speed") ? 15 : 10;
        } else {
            perk2Value = perk2Type.equals("Speed") ? 20 : 15;
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(Messages.get("events.outpost.perk_announcement_border"));
        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.perk_announcement_money", Map.of("amount", String.valueOf(moneyMultiplier))));
        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.perk_announcement_perk2", Map.of("perk", perk2Type, "value", String.valueOf(perk2Value))));
        Bukkit.broadcastMessage(Messages.get("events.outpost.perk_announcement_border")+"\u200B");
        Bukkit.broadcastMessage("");

        applyPerksToIsland(ownerIsland);
    }

    public void applyPerksToIsland(UUID islandUUID) {
        if(islandUUID != null){
        Island curIsle = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);
        if (curIsle != null) {
            for (SuperiorPlayer coop : curIsle.getIslandMembers(true)) {
                PlayerData pd = PlayerDataManager.getPlayerData(coop.asPlayer());
                pd.setOutpostBoost(moneyMultiplier);
                switch (perk2Type) {
                    case "Skillpoint":
                        pd.setSkillpointChance(perk2Value);
                        break;
                    case "Booster":
                        pd.setBoosterChance(perk2Value);
                        break;
                    case "Speed":
                        pd.setSpeedBonus(perk2Value);
                        break;
                }
            }
            }
        }
    }

    public void removePerksFromIsland() {
        if(lastBoosterIsland != null){
            Island lastIsle = SuperiorSkyblockAPI.getIslandByUUID(lastBoosterIsland);
            if (lastIsle != null) {
                for (SuperiorPlayer coop : lastIsle.getIslandMembers(true)) {
                    PlayerData pd = PlayerDataManager.getPlayerData(coop.asPlayer());
                    pd.setOutpostBoost(0.0);
                    pd.setSkillpointChance(0);
                    pd.setBoosterChance(0);
                    pd.setSpeedBonus(0);
                }
            }
        }
    }

    public String getPerk2(){
        return perk2Value + "%" + " " + perk2Type;
    }
    public double getPerk1(){
        return moneyMultiplier;
    }

}
