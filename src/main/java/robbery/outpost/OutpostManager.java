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

public class OutpostManager {
    private final Robbery plugin;
    private final OutpostRegion region;

    // Timing constants (seconds)
    private static final int CAPTURE_TIME_SECONDS = 60; // time to capture (100%)
    private static final int MESSAGE_THRESHOLD = 15; // percent at which we announce

    // Outpost state
    private UUID currentIsland; // island currently attempting to capture/neutralize
    private double progress; // 0..100 percent
    private UUID ownerIsland = null; // current owner

    // Boost management
    private UUID lastBoosterIsland;
    private BukkitTask boostExpireTask;
    private long ownershipExpiryMillis = 0L;

    // Message cooldowns
    private boolean captureAnnounced = false;
    private boolean neutralizeAnnounced = false;
    private long lastMessageTime = 0L;
    private static final long MESSAGE_COOLDOWN_MS = 5000; // 10 seconds between messages

    // Perks
    private double moneyMultiplier = 0.0;
    private String perk2Type = "None";
    private int perk2Value = 0;

    // Derived rates
    private final double INCREMENT_PER_PLAYER; // percent per second per player
    private final double DECAY_PER_SECOND; // percent per second when no players

    public OutpostManager(Robbery plugin, OutpostRegion region) {
        this.plugin = plugin;
        this.region = region;
        this.currentIsland = null;
        this.progress = 0;
        this.lastBoosterIsland = null;
        this.boostExpireTask = null;

        INCREMENT_PER_PLAYER = 100.0 / (double) CAPTURE_TIME_SECONDS;
        DECAY_PER_SECOND = INCREMENT_PER_PLAYER; // symmetric decay/capture speed as requested

        startTasks();
    }

    private void startTasks() {
        // Capture tick task - runs every second
        new BukkitRunnable() {
            public void run() {
                tickCapture();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // Perk rotation task - runs every 4 hours
        new BukkitRunnable() {
            public void run() {
                rollOutpostPerks();
            }
        }.runTaskTimer(plugin, 0L, 4 * 60 * 60 * 20L);
    }

    private void tickCapture() {
        double oldProgress = progress;

        // If ownership expired, clear ownership and perks
        if (ownerIsland != null && System.currentTimeMillis() >= ownershipExpiryMillis) {
            expireBoost();
            return;
        }

        Map<UUID, Integer> islandPlayerCounts = getIslandPlayerCounts();

        if (islandPlayerCounts.isEmpty()) {
            handleNoPlayers();
            updateProgressDisplay();
            return;
        }

        // If more than one different island is present -> contested. Stop progression.
        if (islandPlayerCounts.size() > 1) {
            handleMultipleIslandsContested();
            return; // no progress changes while contested
        }

        // Exactly one island present
        UUID capturingIsland = islandPlayerCounts.keySet().iterator().next();
        int playerCount = islandPlayerCounts.get(capturingIsland);

        // If owner is present (defenders) and the capturing island is the same as owner, show owner time left
        if (ownerIsland != null && ownerIsland.equals(capturingIsland)) {
            // Show owner remaining time to their members
            showTimeLeftToOwner();

            // If there is leftover progress started by another island (neutralization in progress),
            // owner members should actively reduce that progress while they are present.
            if (progress > 0 && currentIsland != null && !currentIsland.equals(ownerIsland)) {
                double ownerDecay = DECAY_PER_SECOND * (double) playerCount;
                progress = Math.max(0.0, progress - ownerDecay);

                if (progress == 0.0) {
                    currentIsland = null;
                    resetMessageFlags();
                }

                updateProgressDisplay();
                return;
            }

            // Nothing else to do when owner is present
            updateProgressDisplay();
            return;
        }

        // At this point, either outpost is unclaimed (ownerIsland == null) and someone is capturing
        // Or outpost is claimed by another island and this island is attempting to neutralize it.

        // If switching capturing island, reset progress to start fresh (new attempt)
        if (currentIsland == null || !currentIsland.equals(capturingIsland)) {
            currentIsland = capturingIsland;
            progress = 0.0;
        }

        double increment = INCREMENT_PER_PLAYER * (double) playerCount;

        if (ownerIsland == null) {
            // Normal capture when outpost is unclaimed
            progress = Math.min(100.0, progress + increment);
            checkAnnouncements(oldProgress, capturingIsland);

            if (progress >= 100.0) {
                completeCapture(capturingIsland);
            }
        } else {
            // Owner exists -> this progress is neutralizing the outpost
            progress = Math.min(100.0, progress + increment);
            checkAnnouncements(oldProgress, capturingIsland);

            if (progress >= 100.0) {
                neutralizeOutpost(capturingIsland);
            }
        }

        updateProgressDisplay();
    }

    private Map<UUID, Integer> getIslandPlayerCounts() {
        Map<UUID, Integer> islandCounts = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;

            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
            if (sp == null) continue;
            Island island = sp.getIsland();
            if (island == null) {
                Messages.sendActionBar(player, "events.outpost.need_island");
                continue;
            }

            UUID islandOwner = island.getOwner().getUniqueId();
            islandCounts.put(islandOwner, islandCounts.getOrDefault(islandOwner, 0) + 1);
        }

        return islandCounts;
    }

    private void handleNoPlayers() {
        if (progress > 0) {
            progress = Math.max(0.0, progress - DECAY_PER_SECOND);
            if (progress == 0.0) {
                currentIsland = null;
                resetMessageFlags();
            }
        } else {
            currentIsland = null;
            resetMessageFlags();
        }
    }

    private void handleMultipleIslandsContested() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;
            Messages.sendActionBar(player, "events.outpost.contested");
        }
        // No progress changes while contested
    }

    private void checkAnnouncements(double oldProgress, UUID capturingIsland) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime < MESSAGE_COOLDOWN_MS) {
            return;
        }

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(capturingIsland);
        Island capturingIsle = sp != null ? sp.getIsland() : null;
        if (capturingIsle == null) return;

        if (oldProgress < MESSAGE_THRESHOLD && progress >= MESSAGE_THRESHOLD) {
            if (ownerIsland == null && !captureAnnounced) {
                broadcastMessage("events.outpost.capturing_broadcast",
                        Map.of("island_name", capturingIsle.getName()));
                captureAnnounced = true;
                lastMessageTime = currentTime;
            } else if (ownerIsland != null && !neutralizeAnnounced) {
                broadcastMessage("events.outpost.neutralizing_broadcast",
                        Map.of("island_name", capturingIsle.getName()));
                neutralizeAnnounced = true;
                lastMessageTime = currentTime;
            }
        }
    }

    private void completeCapture(UUID capturingIsland) {
        // Only complete capture if outpost is currently unclaimed
        if (ownerIsland != null) return;

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(capturingIsland);
        Island island = superiorPlayer != null ? superiorPlayer.getIsland() : null;

        if (island == null) {
            Bukkit.getLogger().warning("Could not find island for UUID: " + capturingIsland);
            return;
        }

        broadcastMessage("events.outpost.capture_broadcast",
                Map.of("island_name", island.getName()));

        applyPerksToIsland(capturingIsland);
        lastBoosterIsland = capturingIsland;
        ownerIsland = capturingIsland;

        long delayMillis = 4L * 60 * 60 * 1000; // 4 hours
        ownershipExpiryMillis = System.currentTimeMillis() + delayMillis;

        // reset state so new captures start fresh
        currentIsland = null;
        progress = 0.0;
        resetMessageFlags();

        if (boostExpireTask != null) {
            boostExpireTask.cancel();
        }
        long ticks = Math.max(1L, delayMillis / 50L);
        boostExpireTask = new BukkitRunnable() {
            @Override
            public void run() {
                expireBoost();
            }
        }.runTaskLater(plugin, ticks);
    }

    private void neutralizeOutpost(UUID neutralizingIsland) {
        // neutralize means removing current owner's perks and making outpost unclaimed.
        if (ownerIsland == null) return; // nothing to neutralize

        SuperiorPlayer spNeutralizer = SuperiorSkyblockAPI.getPlayer(neutralizingIsland);
        Island neutralizerIsland = spNeutralizer != null ? spNeutralizer.getIsland() : null;
        String neutralizerName = neutralizerIsland != null ? neutralizerIsland.getName() : "Unknown";

        // remove perks and announce neutralization
        removePerksFromIsland();

        broadcastMessage("events.outpost.neutralized_broadcast",
                Map.of("island_name", neutralizerName));

        // set to unclaimed state
        ownerIsland = null;
        lastBoosterIsland = null;
        ownershipExpiryMillis = 0L;
        if (boostExpireTask != null) {
            boostExpireTask.cancel();
            boostExpireTask = null;
        }

        // After neutralization, capture must start again normally, so reset capture progress/state
        currentIsland = null;
        progress = 0.0;
        resetMessageFlags();
    }

    private void showTimeLeftToOwner() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;

            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
            Island playerIsland = sp != null ? sp.getIsland() : null;
            if (playerIsland != null && playerIsland.getOwner().getUniqueId().equals(ownerIsland)) {
                long timeLeft = ownershipExpiryMillis - System.currentTimeMillis();
                String formattedTime = formatMillis(Math.max(0, timeLeft));
                Messages.sendActionBarFormatted(player, "events.outpost.owned_time_left",
                        "time", formattedTime);
            }
        }
    }

    private void updateProgressDisplay() {
        if (currentIsland == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;

            // display progress as simple percent 0..100
            sendProgressBar(player, progress, 100.0);
        }
    }

    private void resetMessageFlags() {
        captureAnnounced = false;
        neutralizeAnnounced = false;
    }

    private void sendProgressBar(Player player, double progress, double max) {
        int totalBars = 10;
        double fraction = Math.max(0.0, Math.min(progress / max, 1.0));
        int filledBars = (int) (fraction * totalBars);
        int percentage = (int) Math.round(fraction * 100.0);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            bar.append(i < filledBars ? "§a█" : "§c█");
        }

        Messages.sendActionBarFormatted(player, "events.outpost.progress_bar", Map.of(
                "bar", bar.toString(),
                "percent", String.valueOf(percentage)
        ));
    }

    private String formatMillis(long ms) {
        long totalSec = ms / 1000;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private void broadcastMessage(String messageKey, Map<String, String> placeholders) {
        Bukkit.broadcastMessage(Messages.getFormatted(messageKey, placeholders));
    }

    private void expireBoost() {
        if (lastBoosterIsland != null) {
            removePerksFromIsland();
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_title"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_desc"));
        }

        lastBoosterIsland = null;
        ownerIsland = null;
        ownershipExpiryMillis = 0L;
        boostExpireTask = null;
        resetMessageFlags();
    }

    private void rollOutpostPerks() {
        double roll = Math.random();
        if (roll < 0.33) {
            moneyMultiplier = 0.25;
        } else if (roll < 0.66) {
            moneyMultiplier = 0.5;
        } else {
            moneyMultiplier = 0.75;
        }

        int typeRoll = (int) (Math.random() * 3);
        String[] types = {"Skillpoint Chance", "Booster Chance", "Speed"};
        perk2Type = types[typeRoll];

        double perkRoll = Math.random();
        if (perkRoll < 0.33) {
            perk2Value = perk2Type.equals("Speed") ? 10 : 5;
        } else if (perkRoll < 0.66) {
            perk2Value = perk2Type.equals("Speed") ? 15 : 10;
        } else {
            perk2Value = perk2Type.equals("Speed") ? 20 : 15;
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(Messages.get("events.outpost.perk_announcement_border"));
        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.perk_announcement_money",
                Map.of("amount", String.valueOf(moneyMultiplier))));
        Bukkit.broadcastMessage(Messages.getFormatted("events.outpost.perk_announcement_perk2",
                Map.of(
                        "perk", perk2Type,
                        "value", String.valueOf(perk2Value)
                )));
        Bukkit.broadcastMessage(Messages.get("events.outpost.perk_announcement_border") + "\u200B");
        Bukkit.broadcastMessage("");

        applyPerksToIsland(ownerIsland);
    }


    public void applyPerksToIsland(UUID islandUUID) {
        if (islandUUID == null) return;

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(islandUUID);
        if (sp == null) return;
        Island currentIsle = sp.getIsland();
        if (currentIsle == null) return;
        for (SuperiorPlayer coop : currentIsle.getIslandMembers(true)) {
            Player player = coop.asPlayer();
            if (player != null) {
                PlayerData pd = PlayerDataManager.getPlayerData(player);
                pd.setOutpostBoost(moneyMultiplier);
                switch (perk2Type) {
                    case "Skillpoint Chance":
                        pd.setSkillpointChance(perk2Value);
                        break;
                    case "Booster Chance":
                        pd.setBoosterChance(perk2Value);
                        break;
                    case "Speed":
                        pd.setSpeedBonus(perk2Value);
                        break;
                }
            }
        }
    }

    public void removePerksFromIsland() {
        if (lastBoosterIsland == null) return;

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(lastBoosterIsland);
        if (sp == null) return;
        Island lastIsle = sp.getIsland();
        if (lastIsle != null) {
            for (SuperiorPlayer coop : lastIsle.getIslandMembers(true)) {
                Player player = coop.asPlayer();
                if (player != null) {
                    PlayerData pd = PlayerDataManager.getPlayerData(player);
                    pd.setOutpostBoost(0.0);
                    pd.setSkillpointChance(0);
                    pd.setBoosterChance(0);
                    pd.setSpeedBonus(0);
                }
            }
        }
    }

    // Getters
    public UUID getCurrentIsland() {
        return ownerIsland;
    }

    public String getStatusTitle() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.status-title-unclaimed");

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(ownerIsland);
        Island island = sp != null ? sp.getIsland() : null;
        String islandName = island != null ? island.getName() : Messages.get("events.outpost.holder-island-name-none");

        String titleTemplate = Messages.get("events.outpost.status-title-controlled");
        return titleTemplate.replace("%island%", islandName);
    }

    public String getStatusLoreLine1() {
        if (ownerIsland == null)
            return Messages.get("events.outpost.status-lore-time-left")
                    .replace("%time%", Messages.get("events.outpost.holder-island-name-none"));

        long timeLeft = ownershipExpiryMillis - System.currentTimeMillis();
        String formattedTime = formatMillis(Math.max(0, timeLeft));

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
        return sp != null ? sp.getName() : Messages.get("events.outpost.holder-leader-name-none");
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

    public String getPerk2() {
        return perk2Value + "% " + perk2Type;
    }

    public double getPerk1() {
        return moneyMultiplier;
    }

    public boolean isNeutralized() {
        return ownerIsland == null;
    }
}
