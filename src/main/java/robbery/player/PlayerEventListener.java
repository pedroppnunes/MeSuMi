package robbery.player;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.core.Robbery;
import robbery.backpacks.BackpackManager;
import robbery.backpacks.PvCommand;
import robbery.keys.Rcrate;
import robbery.messages.Messages;
import robbery.quest.QuestProgress;
import robbery.tool.ToolManager;
import robbery.robberyLevel_XP.RobberyLevelUpEvent;
import org.bukkit.Sound;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static robbery.keys.KeyManager.getStoreName;

public class PlayerEventListener implements Listener {

    private static final String NOITEMS_PERMISSION = "robbery.noitems";
    private Robbery plugin;
    private boolean hasLoaded = false;

    public PlayerEventListener(Robbery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        PlayerData memory = new PlayerData(player);
        File playerFile = getPlayerFile(player);

        handleJoinMessage(player);
        loadPlayerDataFromFile(player, memory, playerFile);
        applyOutpostPerks(player, memory);

        if (!player.hasPermission(NOITEMS_PERMISSION)) {
            giveStartingItems(player, memory);
        }

        Rcrate.loadRewards(player.getUniqueId());
        PlayerDataManager.setPlayerData(player, memory);
        PrestigeLeaderboard.updateLeaderboard(player);

        plugin.getChatStyleManager().ensurePlayerExists(player, hasAnyRank(player));

        startAutoSaveTask(player, memory);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getIsBackup()) {
            PlayerData memory = PlayerDataManager.getPlayerData(event.getPlayer());
            Player player = event.getPlayer();

            if (player.hasPermission("robbery.rank7") || player.hasPermission("robbery.staff")) {
                String prefix = getLuckPermsPrefix(player);
                String color = extractColorFromPrefix(prefix);

                for (Player online : Bukkit.getOnlinePlayers()) {
                    Messages.sendComponentMessageFormatted(online, "quit-message",
                            Map.of("prefix", prefix, "player", color + player.getName()));
                }
            }

            savePlayerData(player, memory);
            PlayerDataManager.setPlayerData(player, null);
            PlayerDataManager.setPlayerData(event.getPlayer(), null);
        }
        event.quitMessage(null);
    }

    public void savePlayerData(Player player, PlayerData memory) {
        File playerFolder = new File(plugin.getDataFolder(), "player/" + player.getUniqueId());
        if (!playerFolder.exists()) playerFolder.mkdirs();

        File file = new File(playerFolder, "general.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Basic stats
        cfg.set("stats.xp", memory.getXp());
        cfg.set("stats.level", memory.getLevel());
        cfg.set("stats.rank", memory.getRank());
        cfg.set("stats.prestige", memory.getPrestige());

        // Store robbery stats
        cfg.set("stats.itemsStolen", memory.getItemsStolen());
        cfg.set("stats.storeItems", memory.getStoreItemsMap());
        cfg.set("stats.storeMilestones", memory.getStoreMilestoneMap());

        // Backpack & Tools & Keys
        cfg.set("stats.backpack", memory.getBackpackString());
        cfg.set("stats.material", memory.getBackpack().getMaterial().toString());
        cfg.set("stats.itemsbackpack", memory.getBackpackItemsString());
        cfg.set("stats.colorbackpack", memory.getBackpack().getColorname());
        cfg.set("stats.hasbackpack", memory.getBackpackunlucked());
        cfg.set("stats.hastool", memory.getToolsunlucked());
        cfg.set("stats.tool", memory.getToolString());
        cfg.set("stats.key", memory.getKeyString());
        cfg.set("stats.haskeys", memory.getKeysString());

        // Boosters
        cfg.set("stats.booster", memory.getActiveBoostString());
        cfg.set("stats.hasbooster", memory.getBoostersString());
        cfg.set("stats.boosterpaused", memory.isBoostersPaused());

        // Skillpoints & Tree
        cfg.set("stats.skillpoints", memory.getSkillPoints());
        cfg.set("skilltree.levels", memory.getAllSkillTreeLevels());
        cfg.set("skilltree.perks", memory.getAllPerkValues());
        cfg.set("skilltree.reset", memory.getResetSkillTreePoints());

        // Daily Quests
        cfg.set("dailyQuests.offered", memory.getOfferedDailyQuests());
        cfg.set("dailyQuests.accepted", new ArrayList<>(memory.getAcceptedDailyQuests()));
        cfg.set("dailyQuests.lastPick", memory.getLastDailyQuestPick());
        cfg.set("dailyQuests.completedCount", memory.getDailyQuestsCompleted());
        cfg.set("dailyQuests.lastResetDay", memory.getLastResetDay());
        cfg.set("dailyQuests.talkedToNPC", memory.hasTalkedToQuestNPC());

        // Quest progress
        Map<String, Map<String, Object>> progressMap = new HashMap<>();
        for (Map.Entry<String, QuestProgress> entry : memory.getQuestProgressMap().entrySet()) {
            QuestProgress pr = entry.getValue();
            Map<String, Object> data = new HashMap<>();
            data.put("itemsStolen", pr.getItemsCompleted());
            data.put("completed", pr.getCompleted());
            data.put("halfRewardGiven", pr.isHalfRewardGiven());
            progressMap.put(entry.getKey(), data);
        }
        cfg.set("dailyQuests.progress", progressMap);
        cfg.set("dailyQuests.active", memory.getActiveQuest());

        // Player location
        Location loc = player.getLocation();
        cfg.set("stats.location.world", loc.getWorld().getName());
        cfg.set("stats.location.x", loc.getX());
        cfg.set("stats.location.y", loc.getY());
        cfg.set("stats.location.z", loc.getZ());
        cfg.set("stats.location.yaw", loc.getYaw());
        cfg.set("stats.location.pitch", loc.getPitch());

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getPlayerFile(Player player) {
        File folder = new File(plugin.getDataFolder(), "player/" + player.getUniqueId());
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, "general.yml");
    }
    private void handleJoinMessage(Player player) {
        if (!player.hasPermission("robbery.rank7") && !player.hasPermission("robbery.staff")) return;

        String prefix = getLuckPermsPrefix(player);
        String color = extractColorFromPrefix(prefix);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Messages.sendComponentMessageFormatted(online, "join-message",
                    Map.of("prefix", prefix, "player", color + player.getName()));
        }
    }
    private void applyOutpostPerks(Player player, PlayerData memory) {
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
        Island playerIsland = sp.getIsland();
        Island outpostIsland = plugin.getOutpostManager().getCurrentIsland();

        if (playerIsland != null && playerIsland.equals(outpostIsland)) {
            memory.setOutMooneyMult(plugin.getOutpostManager().getPerk1());

            String perk2 = plugin.getOutpostManager().getPerk2();
            int value = Integer.parseInt(perk2.split("%")[0]);

            if (perk2.contains("Skillpoint")) memory.setOutSkillpointChance(value);
            else if (perk2.contains("Booster")) memory.setOutBoosterChance(value);
            else if (perk2.contains("Speed")) memory.setOutSpeedBonus(value);
        } else {
            memory.setOutMooneyMult(0);
            memory.setOutSkillpointChance(0);
            memory.setOutBoosterChance(0);
            memory.setOutSpeedBonus(0);
        }
    }
    private void loadPlayerDataFromFile(Player player, PlayerData memory, File file) {
        if (!file.exists()) {
            memory.setRank(getRank(player));
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Basic stats
        memory.setRank(cfg.getString("stats.rank"));
        memory.setXp(cfg.getLong("stats.xp", 0L));
        memory.setLevel(Math.max(1, cfg.getInt("stats.level", 1)));
        memory.setPrestige(cfg.getInt("stats.prestige", 0));

        // Tools & keys
        memory.setToolsunlocked(cfg.getString("stats.hastool"));
        memory.setTool(ToolManager.getToolsName(cfg.getString("stats.tool")));
        memory.setKeys(cfg.getString("stats.haskeys"));
        memory.setKey(getStoreName(cfg.getString("stats.key")));

        //Skillpoints
        memory.setSP(cfg.getString("stats.skillpoints"));

        // Stats & store
        memory.setItemsStolen(cfg.getInt("stats.itemsStolen", 0));
        loadMap(cfg, "stats.storeItems", memory::setStoreItemsMap);
        loadMap(cfg, "stats.storeMilestones", memory::setStoreMilestoneMap);

        // Skill tree
        if (cfg.contains("skilltree.levels")) {
            ConfigurationSection levelSec = cfg.getConfigurationSection("skilltree.levels");
            for (String key : levelSec.getKeys(false)) {
                memory.getAllSkillTreeLevels().put(key, levelSec.getInt(key));
            }
        }
        loadMapDouble(cfg, "skilltree.perks", memory.getAllPerkValues());
        memory.setResetSkillTreePoints(cfg.getInt("skilltree.reset", 0));

        // Backpack
        memory.setBackpack(BackpackManager.toBackpack(cfg.getString("stats.backpack"), cfg.getString("stats.material"),
                cfg.getString("stats.itemsbackpack"), cfg.getString("stats.colorbackpack")));
        memory.setBackpackunlucked(cfg.getString("stats.hasbackpack"));

        // Boosters
        memory.setBoostersPaused(cfg.getBoolean("stats.boosterpaused"));
        memory.setActiveBooster(cfg.getString("stats.booster"));
        memory.setBoostersFromString(cfg.getString("stats.hasbooster"));

        // Daily Quests
        if (cfg.contains("dailyQuests.offered"))
            memory.getOfferedDailyQuests().addAll(cfg.getStringList("dailyQuests.offered"));
        if (cfg.contains("dailyQuests.accepted"))
            memory.getAcceptedDailyQuests().addAll(cfg.getStringList("dailyQuests.accepted"));
        if (cfg.contains("dailyQuests.lastPick"))
            memory.setLastDailyQuestPick(cfg.getLong("dailyQuests.lastPick"));

        memory.setDailyQuestsCompleted(cfg.getInt("dailyQuests.completedCount", 0));
        memory.setLastResetDay(cfg.getInt("dailyQuests.lastResetDay", -1));
        memory.setTalkedToQuestNPC(cfg.getBoolean("dailyQuests.talkedToNPC", false));

        // FIXED: Quest progress (Casting fix)
        if (cfg.contains("dailyQuests.progress")) {
            ConfigurationSection progressSec = cfg.getConfigurationSection("dailyQuests.progress");
            for (String questId : progressSec.getKeys(false)) {
                // We use ConfigurationSection here, NOT FileConfiguration
                ConfigurationSection section = progressSec.getConfigurationSection(questId);
                if (section != null) {
                    QuestProgress pr = new QuestProgress(questId);
                    pr.setItemsCompleted(section.getInt("itemsStolen", 0));
                    pr.setHalfRewardGiven(section.getBoolean("halfRewardGiven", false));
                    pr.setCompleted(section.getBoolean("completed", false));
                    memory.getQuestProgressMap().put(questId, pr);
                }
            }
        }

        // Quest active flags
        if (cfg.contains("dailyQuests.active")) {
            ConfigurationSection activeSec = cfg.getConfigurationSection("dailyQuests.active");
            for (String questId : activeSec.getKeys(false)) {
                memory.getAcceptedDailyQuests().add(questId);
            }
        }

        // Location
        if (cfg.contains("stats.location")) {
            teleportPlayerFromConfig(player, cfg);
        }
    }
    private void loadMap(FileConfiguration cfg, String path, java.util.function.Consumer<Map<String, Integer>> consumer) {
        if (!cfg.contains(path)) return;
        Map<String, Integer> map = new HashMap<>();
        for (String key : cfg.getConfigurationSection(path).getKeys(false)) {
            map.put(key, cfg.getInt(path + "." + key));
        }
        consumer.accept(map);
    }

    private void loadMapDouble(FileConfiguration cfg, String path, Map<String, Double> target) {
        if (!cfg.contains(path)) return;
        for (Map.Entry<String, Object> e : cfg.getConfigurationSection(path).getValues(false).entrySet()) {
            target.put(e.getKey(), ((Number) e.getValue()).doubleValue());
        }
    }

    private void teleportPlayerFromConfig(Player player, FileConfiguration cfg) {
        String worldName = cfg.getString("stats.location.world");
        double x = cfg.getDouble("stats.location.x");
        double y = cfg.getDouble("stats.location.y");
        double z = cfg.getDouble("stats.location.z");
        float yaw = (float) cfg.getDouble("stats.location.yaw");
        float pitch = (float) cfg.getDouble("stats.location.pitch");

        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Location loc = new Location(world, x, y, z, yaw, pitch);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.teleport(loc);
        }, 5L);
    }

    private void giveStartingItems(Player player, PlayerData memory) {
        memory.giveToolToInv();
        memory.giveBackpackToInv();
        memory.giveBooster(plugin);
        memory.giveSkillTree(plugin);
        memory.giveMainmenu(plugin);
        memory.giveFeather();
        plugin.getHidePlayers().handleWorldChange(player);
    }

    private boolean hasAnyRank(Player player) {
        for (int i = 1; i <= 7; i++) {
            if (player.hasPermission("robbery.rank" + i)) return true;
        }
        return player.hasPermission("robbery.staff");
    }

    private void startAutoSaveTask(Player player, PlayerData memory) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                savePlayerData(player, memory);
            }
        }.runTaskTimer(plugin, 40L, 6000L);
    }

    private String getLuckPermsPrefix(Player player) {
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null)
            return "";
        CachedMetaData metaData = user.getCachedData().getMetaData(api.getContextManager().getQueryOptions(player));
        String prefix = metaData.getPrefix();
        return prefix == null ? "" : prefix;
    }

    public static String extractColorFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty())
            return "&f";

        int bracketEnd = prefix.indexOf('[');
        if (bracketEnd != -1) {
            String afterBracket = prefix.substring(bracketEnd + 1);

            int hexIndex = afterBracket.indexOf("&#");
            if (hexIndex != -1 && hexIndex + 8 <= afterBracket.length()) {
                return afterBracket.substring(hexIndex, hexIndex + 8);
            }

            int ampIndex = afterBracket.indexOf('&');
            if (ampIndex != -1 && ampIndex + 1 < afterBracket.length()) {
                return afterBracket.substring(ampIndex, ampIndex + 2);
            }
        }

        return "&f";
    }

    private String getRank(Player player) {
        if (player.hasPermission("robbery.rank7"))
            return "rank7";
        if (player.hasPermission("robbery.rank6"))
            return "rank6";
        if (player.hasPermission("robbery.rank5"))
            return "rank5";
        if (player.hasPermission("robbery.rank4"))
            return "rank4";
        if (player.hasPermission("robbery.rank3"))
            return "rank3";
        if (player.hasPermission("robbery.rank2"))
            return "rank2";
        if (player.hasPermission("robbery.rank1"))
            return "rank1";

        return "rank0";
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!hasLoaded && player.getWorld().getName().equalsIgnoreCase("world")) {
            hasLoaded = true;
            Bukkit.getScheduler().runTaskLater(plugin, plugin::updateDailyNPC, 20L);
            Bukkit.getScheduler().runTaskLater(plugin, plugin::loadItems, 40L);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (!hasLoaded && player.getWorld().getName().equalsIgnoreCase("world")) {
            hasLoaded = true;
            Bukkit.getScheduler().runTaskLater(plugin, plugin::updateDailyNPC, 20L);
            Bukkit.getScheduler().runTaskLater(plugin, plugin::loadItems, 40L);
        }
    }

    @EventHandler
    public void onPlayerLevelUp(RobberyLevelUpEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        Component mainTitle = Component.text("Robbery Level Up!", NamedTextColor.AQUA);

        Component subTitle = Component.text("You reached ", NamedTextColor.AQUA)
                .append(Component.text("Level " + newLevel, NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.AQUA));

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofMillis(3500),
                Duration.ofMillis(1000)
        );

        Title title = Title.title(mainTitle, subTitle, times);
        player.showTitle(title);
        Messages.sendFormatted(player,"events.robberyxp.level-up",java.util.Map.of("level",String.valueOf(newLevel)));
    }

    public boolean getHasLoaded() {
        return hasLoaded;
    }

}
