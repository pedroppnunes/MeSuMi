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

    // Timing constants
    private static final int CAPTURE_TIME_SECONDS = 60;
    private static final int NEUTRALIZE_TIME_SECONDS = 120;
    private static final int MESSAGE_THRESHOLD = 10;

    // Outpost state
    private UUID currentIsland;
    private double progress;
    private UUID ownerIsland = null;
    private boolean isNeutralized = false;

    // Boost management
    private UUID lastBoosterIsland;
    private BukkitTask boostExpireTask;
    private long ownershipExpiryMillis = 0L;

    // Message cooldowns
    private boolean captureAnnounced = false;
    private boolean neutralizeAnnounced = false;
    private long lastMessageTime = 0L;
    private static final long MESSAGE_COOLDOWN_MS = 10000; // 10 seconds between messages

    // Perks
    private double moneyMultiplier = 0.0;
    private String perk2Type = "None";
    private int perk2Value = 0;

    public OutpostManager(Robbery plugin, OutpostRegion region) {
        this.plugin = plugin;
        this.region = region;
        this.currentIsland = null;
        this.progress = 0;
        this.lastBoosterIsland = null;
        this.boostExpireTask = null;
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

        // Check if boost expired
        if (ownerIsland != null && System.currentTimeMillis() >= ownershipExpiryMillis) {
            expireBoost();
            return;
        }

        // Get islands present in the outpost
        Map<UUID, Integer> islandPlayerCounts = getIslandPlayerCounts();

        if (islandPlayerCounts.isEmpty()) {
            handleNoPlayers();
            return;
        }

        if (islandPlayerCounts.size() == 1) {
            handleSingleIsland(islandPlayerCounts, oldProgress);
        } else {
            handleMultipleIslands();
        }

        updateProgressDisplay();
        checkCaptureCompletion();
    }

    private Map<UUID, Integer> getIslandPlayerCounts() {
        Map<UUID, Integer> islandCounts = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;

            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
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
            progress = Math.max(0, progress - 1);
        } else {
            currentIsland = null;
            resetMessageFlags();
        }
    }

    private void handleSingleIsland(Map<UUID, Integer> islandCounts, double oldProgress) {
        UUID capturingIsland = islandCounts.keySet().iterator().next();
        int playerCount = islandCounts.get(capturingIsland);

        // If owner is present in their own outpost, show time left
        if (ownerIsland != null && ownerIsland.equals(capturingIsland)) {
            showTimeLeftToOwner();
            return;
        }

        // Handle capture progress
        if (currentIsland == null) {
            currentIsland = capturingIsland;
            progress = 1.0;
        } else if (!currentIsland.equals(capturingIsland)) {
            // Different island - decrease progress
            progress = Math.max(0, progress - 1);
            if (progress == 0) {
                currentIsland = capturingIsland;
                progress = 1.0;
            }
        } else {
            // Same island - increase progress
            double increment = playerCount;
            double maxProgress = ownerIsland == null ? CAPTURE_TIME_SECONDS : NEUTRALIZE_TIME_SECONDS;
            progress = Math.min(progress + increment, maxProgress);
        }

        // Check for announcement thresholds with cooldown
        checkAnnouncements(oldProgress, capturingIsland);
    }

    private void handleMultipleIslands() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (region.isInside(player.getLocation())) {
                Messages.sendActionBar(player, "events.outpost.contested");
            }
        }

        // Slightly decay progress when contested
        if (progress > 0) {
            progress = Math.max(0, progress - 0.5);
        }
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

    private void checkCaptureCompletion() {
        if (currentIsland == null) return;

        if (ownerIsland == null) {
            // Capture unowned outpost
            if (progress >= CAPTURE_TIME_SECONDS) {
                completeCapture(currentIsland);
            }
        } else {
            // Handle owned outpost
            if (progress >= CAPTURE_TIME_SECONDS && progress < NEUTRALIZE_TIME_SECONDS) {
                if (!isNeutralized) {
                    neutralizeOutpost();
                }
            } else if (progress >= NEUTRALIZE_TIME_SECONDS) {
                completeCapture(currentIsland);
            }
        }
    }

    private void completeCapture(UUID islandOwner) {
        isNeutralized = false;
        removePerksFromIsland();

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(islandOwner);
        Island island = superiorPlayer != null ? superiorPlayer.getIsland() : null;

        if (island == null) {
            Bukkit.getLogger().warning("Could not find island for UUID: " + islandOwner);
            return;
        }

        // Broadcast capture
        broadcastMessage("events.outpost.capture_broadcast",
                Map.of("island_name", island.getName()));

        // Apply perks and set ownership
        applyPerksToIsland(islandOwner);
        lastBoosterIsland = islandOwner;
        ownerIsland = islandOwner;

        // Set expiry (4 hours)
        long delayMillis = 4L * 60 * 60 * 1000;
        ownershipExpiryMillis = System.currentTimeMillis() + delayMillis;

        // Reset capture state
        currentIsland = null;
        progress = 0;
        resetMessageFlags();

        // Schedule boost expiry
        if (boostExpireTask != null) {
            boostExpireTask.cancel();
        }
        boostExpireTask = new BukkitRunnable() {
            @Override
            public void run() {
                expireBoost();
            }
        }.runTaskLater(plugin, delayMillis / 50);
    }

    private void neutralizeOutpost() {
        removePerksFromIsland();

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(currentIsland);
        Island capturingIsle = sp != null ? sp.getIsland() : null;
        String islandName = capturingIsle != null ? capturingIsle.getName() : "Unknown";

        broadcastMessage("events.outpost.neutralized_broadcast",
                Map.of("island_name", islandName));

        isNeutralized = true;
        neutralizeAnnounced = true;
        progress = 0;

        broadcastMessage("events.outpost.capturing_broadcast",
                Map.of("island_name", islandName));
    }

    private void showTimeLeftToOwner() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!region.isInside(player.getLocation())) continue;

            SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
            Island playerIsland = sp.getIsland();
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

            double displayProgress;

            if (ownerIsland == null || isNeutralized) {
                displayProgress = progress;
            } else {
                if (progress < CAPTURE_TIME_SECONDS) {
                    displayProgress = progress;
                } else {
                    displayProgress = progress - CAPTURE_TIME_SECONDS;
                }
            }

            sendProgressBar(player, displayProgress, CAPTURE_TIME_SECONDS);
        }
    }

    private void resetMessageFlags() {
        captureAnnounced = false;
        neutralizeAnnounced = false;
        isNeutralized = false;
    }

    private void sendProgressBar(Player player, double progress, double max) {
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

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_border"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_title"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_desc"));
            Bukkit.broadcastMessage(Messages.get("events.outpost.boost_expired_border") + "\u200B");
            Bukkit.broadcastMessage("");
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
                Map.of("perk", perk2Type, "value", String.valueOf(perk2Value))));
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
        return isNeutralized;
    }
}