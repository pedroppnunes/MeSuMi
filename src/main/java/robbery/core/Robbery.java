package robbery.core;

import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.booster.BoosterItemListener;
import robbery.chat.ChatStyleManager;
import robbery.chunk.LoadChunks;
import robbery.hotbar.HotbarListener;
import robbery.items.AddItem;
import robbery.items.RemoveItem;
import robbery.economy.Baltop;
import robbery.economy.Sell;
import robbery.mechanics.Busted;
import robbery.backpacks.BuyBackpack;
import robbery.backpacks.PvCommand;
import robbery.keys.BuyKey;
import robbery.keys.Rcrate;
import robbery.player.PlayerDataManager;
import robbery.skilltree.*;
import robbery.storeMastery.StoreMasteryManager;
import robbery.tool.BuyTool;
import robbery.chat.ChatColorCommand;
import robbery.claim.Claim;
import robbery.mechanics.HidePlayers;
import robbery.mechanics.NightVision;
import robbery.mechanics.ToggleDoubleJump;
import robbery.teleport.Lobby;
import robbery.teleport.Mall;
import robbery.teleport.SpawnCommand;
import robbery.teleport.StoreTeleport;
import robbery.mutes.MuteCommand;
import robbery.mutes.MuteInfoCommand;
import robbery.mutes.UnmuteCommand;
import robbery.outpost.Outpost;
import robbery.prestige.Prestige;
import robbery.ranks.RankUp;
import robbery.ranks.RankUpdate;
import robbery.booster.StopBoosterCommand;
import robbery.booster.UseBooster;
import robbery.warnings.WarnCommand;
import robbery.warnings.WarningsCommand;
import robbery.leaderboard.WeeklyLeaderboardCommand;
import robbery.mechanics.ArmorStandInteractionListener;
import robbery.mechanics.BlockCraftListener;
import robbery.mechanics.DoubleJumpListener;
import robbery.mechanics.HideoutListener;
import robbery.mechanics.InventoryLockListener;
import robbery.chat.ChatItemReplacer;
import robbery.claim.ClaimGuiListener;
import robbery.leaderboard.HourlyLeaderboard;
import robbery.leaderboard.WeeklyLeaderboardTask;
import robbery.votes.VoteListener;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.mutes.MuteManager;
import robbery.outpost.CombatManager;
import robbery.outpost.OutpostManager;
import robbery.outpost.OutpostRegion;
import robbery.player.PlayerEventListener;
import robbery.prestige.PrestigeCountManager;
import robbery.ranks.RankPaperListener;
import robbery.robberyLevel_XP.AdminXPCommand;
import robbery.robberyLevel_XP.XPManager;
import robbery.votes.VotePartyManager;
import robbery.warnings.ChatWarningListener;
import robbery.warnings.WarningManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class Robbery extends JavaPlugin implements Listener {

    private static final Map<String, Items> itemsMap = new HashMap<>();

    private static Economy econ = null;
    private final List<Items> items = new ArrayList<>();
    private static Robbery main;
    private boolean isBackingUp = false;

    private Rcrate rcrate;
    private OutpostManager outpostManager;
    private MuteManager muteManager;
    private WarningManager warningManager;
    private HidePlayers hidePlayers;
    private VotePartyManager votePartyManager;
    private ChatStyleManager chatStyleManager;
    private WeeklyLeaderboardTask weeklyLeaderboardTask;
    private HourlyLeaderboard hourlyLeaderboard;
    private PlayerEventListener playerEventListener;
    private XPManager xpManager;
    private StoreMasteryManager storeMasteryManager;
    private static SkillTreeConfig skillTreeConfig;
    private SkillService skillService;

    private FileConfiguration itemConfig;
    private File itemConfigFile;



    @Override
    public void onEnable() {
        getLogger().info("Starting");
        main = this;
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Bukkit.getPluginManager().registerEvents(this, this);
            RobberyPlaceholderExpansion.registerHook();
        } else {
            getLogger().warning("Could not find PlaceholderAPI! This plugin is required.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        Plugin voting = Bukkit.getPluginManager().getPlugin("VotingPlugin");

        if (voting != null && voting.isEnabled()) {
            getLogger().info("Hooked into VotingPlugin tips.");
        } else {
            getLogger().warning("VotingPlugin not found. Tips feature disabled.");
        }
        this.hidePlayers = new HidePlayers(main);
        this.rcrate = new Rcrate();
        this.warningManager = new WarningManager(main);
        this.muteManager = new MuteManager(main);
        this.votePartyManager = new VotePartyManager(main);
        this.chatStyleManager = new ChatStyleManager(getDataFolder());
        this.playerEventListener = new PlayerEventListener(main);
        this.xpManager = new XPManager(main);
        this.storeMasteryManager = new StoreMasteryManager(main);

        getServer().getPluginManager().registerEvents(new VoteListener(main), main);
        BlockCraftListener blockCraft = new BlockCraftListener();
        Messages.init(main);
        new AutoReloadTask(this).runTaskTimerAsynchronously(this, 0L, 20L);
        getServer().getPluginManager().registerEvents(new InventoryLockListener(), main);
        getServer().getPluginManager().registerEvents(new ArmorStandInteractionListener(main), main);
        getServer().getPluginManager().registerEvents(playerEventListener, main);
        getServer().getPluginManager().registerEvents(new BoosterItemListener(main), main);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(main), main);
        getServer().getPluginManager().registerEvents(new HideoutListener(), main);
        getServer().getPluginManager().registerEvents(new ChatItemReplacer(main, muteManager), main);
        getServer().getPluginManager().registerEvents(new CombatManager(main), main);
        getServer().getPluginManager().registerEvents(new ChatWarningListener(main), main);
        getServer().getPluginManager().registerEvents(hidePlayers, main);
        getServer().getPluginManager().registerEvents(blockCraft, main);
        getServer().getPluginManager().registerEvents(rcrate, main);
        getServer().getPluginManager().registerEvents(new RankPaperListener(), main);
        getServer().getPluginManager().registerEvents(new ClaimGuiListener(), main);
        getServer().getPluginManager().registerEvents(new SkillTreeItem(main), main);
        getServer().getPluginManager().registerEvents(new HotbarListener(main), main);
        Objects.requireNonNull(getCommand("additem")).setExecutor(new AddItem(main));
        Objects.requireNonNull(getCommand("removeItem")).setExecutor(new RemoveItem(main));
        Objects.requireNonNull(getCommand("sellrob")).setExecutor(new Sell(main));
        Objects.requireNonNull(getCommand("buyback")).setExecutor(new BuyBackpack(main));
        Objects.requireNonNull(getCommand("buytool")).setExecutor(new BuyTool(main));
        Objects.requireNonNull(getCommand("buykey")).setExecutor(new BuyKey(main));
        Objects.requireNonNull(getCommand("busted")).setExecutor(new Busted(main));
        Objects.requireNonNull(getCommand("prestige")).setExecutor(new Prestige(main));
        Objects.requireNonNull(getCommand("load")).setExecutor(new Load(main));
        Objects.requireNonNull(getCommand("usebooster")).setExecutor(new UseBooster(main));
        Objects.requireNonNull(getCommand("toggledoublejump")).setExecutor(new ToggleDoubleJump(main));
        Objects.requireNonNull(getCommand("hp")).setExecutor(hidePlayers);
        Objects.requireNonNull(getCommand("mall")).setExecutor(new Mall(main));
        Objects.requireNonNull(getCommand("rankupdate")).setExecutor(new RankUpdate(main));
        Objects.requireNonNull(getCommand("nv")).setExecutor(new NightVision(main));
        Objects.requireNonNull(getCommand("pv")).setExecutor(new PvCommand());
        Objects.requireNonNull(getCommand("warn")).setExecutor(new WarnCommand(main));
        Objects.requireNonNull(getCommand("outpost")).setExecutor(new Outpost());
        Objects.requireNonNull(getCommand("robbery")).setExecutor(new RobberyReload(main));
        Objects.requireNonNull(getCommand("rcrate")).setExecutor(rcrate);
        Objects.requireNonNull(getCommand("claim")).setExecutor(new Claim());
        Objects.requireNonNull(getCommand("rankup")).setExecutor(new RankUp(main));
        Objects.requireNonNull(getCommand("store")).setExecutor(new StoreTeleport());
        Objects.requireNonNull(getCommand("warnings")).setExecutor(new WarningsCommand(warningManager));
        Objects.requireNonNull(getCommand("mute")).setExecutor(new MuteCommand(muteManager));
        Objects.requireNonNull(getCommand("unmute")).setExecutor(new UnmuteCommand(muteManager));
        Objects.requireNonNull(getCommand("muteinfo")).setExecutor(new MuteInfoCommand(muteManager));
        Objects.requireNonNull(getCommand("baltop")).setExecutor(new Baltop());
        Objects.requireNonNull(getCommand("help")).setExecutor(new HelpCommand(main));
        SpawnCommand s = new SpawnCommand();
        Objects.requireNonNull(getCommand("spawn")).setExecutor(s);
        Objects.requireNonNull(getCommand("s")).setExecutor(s);
        Objects.requireNonNull(getCommand("ct")).setExecutor(new ChatColorCommand(main));
        Lobby l = new Lobby(main);
        Objects.requireNonNull(getCommand("lobby")).setExecutor(l);
        Objects.requireNonNull(getCommand("l")).setExecutor(l);
        Objects.requireNonNull(getCommand("weeklyleaderboard")).setExecutor(new WeeklyLeaderboardCommand(main));
        Objects.requireNonNull(getCommand("loadbackup")).setExecutor(new LoadBackup(main));
        Objects.requireNonNull(getCommand("migrate")).setExecutor(new MigrateBackup(main));
        Objects.requireNonNull(getCommand("stopbooster")).setExecutor(new StopBoosterCommand());
        Objects.requireNonNull(getCommand("adminxp")).setExecutor(new AdminXPCommand(main));
        skillTreeConfig = new SkillTreeConfig(this);
        this.skillService = new SkillService(this, skillTreeConfig);
        Objects.requireNonNull(getCommand("skillbuy")).setExecutor(new SkillPerkBuyCommand(skillService,skillTreeConfig));
        Objects.requireNonNull(getCommand("resetskilltree")).setExecutor(new SkillTreeResetCommand(main,skillTreeConfig));
        getServer().getMessenger().registerOutgoingPluginChannel(main, "BungeeCord");
        World outpostWorld = Bukkit.getWorld("outpost");
        if (outpostWorld == null) {
            getLogger().severe("World 'outpost' not found!");
            return;
        }
        PrestigeCountManager.load();
        OutpostRegion region = new OutpostRegion(outpostWorld, -3, 1, -3, 4, 1, 4);
        outpostManager = new OutpostManager(main, region);
        blockCraft.removeRecipes();
        addItemstoMap();

        long ticksPerDay = 24 * 60 * 60 * 20L;
        long now = System.currentTimeMillis();
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atStartOfDay();
        long delayTicks = (nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - now) / 50;
        getServer().getScheduler().runTaskTimer(main, votePartyManager::resetVotes, delayTicks, ticksPerDay);
        saveDefaultConfig();
        tips = main.getConfig().getStringList("tips");

        if (getConfig().getBoolean("leaderboards.weekly.enabled")) {
            String webhookUrl = getConfig().getString("leaderboards.weekly.webhook");
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                weeklyLeaderboardTask = new WeeklyLeaderboardTask(this, webhookUrl);
                getLogger().info("Weekly leaderboard enabled.");
            } else {
                getLogger().warning("Weekly leaderboard enabled but webhook is missing.");
            }
        }

        if (getConfig().getBoolean("leaderboards.hourly.enabled")) {
            String webhookUrl = getConfig().getString("leaderboards.hourly.webhook");
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                hourlyLeaderboard = new HourlyLeaderboard(this, webhookUrl);
                getLogger().info("Hourly leaderboard enabled.");
            } else {
                getLogger().warning("Hourly leaderboard enabled but webhook is missing.");
            }
        }

        startItemClearTask();
        startVoteReminderTask();
        startTipsTask();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveItems, 20L * 60, 20L * 60 * 5);
    }

    public void addItemstoMap() {
        itemsMap.clear();
        itemConfigFile = new File(getDataFolder(), "additems.yml");
        if (!itemConfigFile.exists()) {
            saveResource("additems.yml", false);
            return;
        }

        itemConfig = YamlConfiguration.loadConfiguration(itemConfigFile);
        ConfigurationSection section = itemConfig.getConfigurationSection("items");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            double hp = section.getDouble(key + ".hp");
            int value = section.getInt(key + ".value");
            String name = section.getString(key + ".name");
            String playername = section.getString(key + ".playername");
            int time = section.getInt(key + ".time");

            Items it = new Items(hp, value, name, playername, time, key.toLowerCase());
            itemsMap.put(key.toLowerCase(), it);
        }
    }

    @Override
    public void onDisable() {
        this.isBackingUp = true;
        PrestigeCountManager.save();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerEventListener.savePlayerData(player, PlayerDataManager.getPlayerData(player));
            player.kick(Component.text(Messages.get("reload.player-kick")));
        }
        Bukkit.getScheduler().cancelTasks(this);
        saveItems();
        getLogger().info("Shutting Down Robbery!");
    }

    public static Map<String, Items> getItemsMap() {
        return itemsMap;
    }

    public void addItems(Items i) {
        items.add(i);
        saveBackupItem(i);
    }

    public List<Items> getItems() {
        return items;
    }

    public void removeItem(Items i) {
        items.remove(i);
        removeBackupItem(i);
        i.remove();
    }

    public XPManager getXpManager() {
        return xpManager;
    }

    public static Robbery getInstance() {
        return main;
    }

    public static Items getItemsbyName(String name) {
        return itemsMap.getOrDefault(name, null);
    }

    public void saveItems() {
        if (!playerEventListener.getHasLoaded())
            return;

        File itemsFile = new File(getDataFolder(), "items.yml");
        FileConfiguration itemsConfig = new YamlConfiguration();
        itemsConfig.set("items", null);

        Set<String> chunkCoords = new HashSet<>();

        boolean shuttingDown = !getServer().getPluginManager().isPluginEnabled(this);

        Runnable saveTask = () -> {
            for (Items item : items) {
                Map<String, Object> itemData = item.serialize();
                Object droppedIdObj = itemData.get("droppedItem");

                if (droppedIdObj instanceof String droppedId) {
                    itemsConfig.createSection("items." + droppedId, itemData);
                } else {
                    Object worldObj = itemData.get("world");
                    Object xObj = itemData.get("x");
                    Object yObj = itemData.get("y");
                    Object zObj = itemData.get("z");

                    if (worldObj instanceof String worldName &&
                            xObj instanceof Number xNum &&
                            yObj instanceof Number yNum &&
                            zObj instanceof Number zNum) {

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            getLogger().warning("Invalid world while recovering item: " + worldName);
                            continue;
                        }

                        Location loc = new Location(world, xNum.doubleValue(), yNum.doubleValue(), zNum.doubleValue());
                        double radius = 0.8;

                        Collection<Entity> entities = world.getNearbyEntities(loc, radius, radius, radius);
                        List<Item> nearbyItems = entities.stream()
                                .filter(e -> e instanceof Item)
                                .map(e -> (Item) e)
                                .toList();

                        if (!nearbyItems.isEmpty()) {
                            Item closestItem = nearbyItems.stream()
                                    .min(Comparator.comparingDouble(i -> i.getLocation().distanceSquared(loc)))
                                    .orElse(null);

                            UUID foundId = closestItem.getUniqueId();
                            itemData.put("droppedItem", foundId.toString());
                            item.setDroppedItem(closestItem);
                            itemsConfig.createSection("items." + foundId, itemData);

                            getLogger().info("Recovered missing droppedItem for '" + item.getName() + "' at "
                                    + worldName + " (" + xNum + ", " + yNum + ", " + zNum + ")");
                        } else {
                            getLogger().warning("No nearby item found to recover for: " + item.getName());
                        }
                    } else {
                        getLogger().warning("Skipping item with invalid position data: " + item);
                    }
                }

                Object worldObj = itemData.get("world");
                Object xObj = itemData.get("x");
                Object zObj = itemData.get("z");
                if (worldObj instanceof String worldName && xObj instanceof Number && zObj instanceof Number) {
                    int chunkX = (int) Math.floor(((Number) xObj).doubleValue()) >> 4;
                    int chunkZ = (int) Math.floor(((Number) zObj).doubleValue()) >> 4;
                    chunkCoords.add(worldName + ":" + chunkX + ":" + chunkZ);
                }
            }

            try {
                itemsConfig.save(itemsFile);
            } catch (Exception e) {
                getLogger().severe("Failed to save items.yml: " + e.getMessage());
            }

            File chunksFile = new File(getDataFolder(), "chunks.yml");
            FileConfiguration chunkCfg = YamlConfiguration.loadConfiguration(chunksFile);
            chunkCfg.set("chunks", new ArrayList<>(chunkCoords));
            try {
                chunkCfg.save(chunksFile);
            } catch (Exception e) {
                getLogger().severe("Failed to save chunks.yml: " + e.getMessage());
            }
        };

        if (shuttingDown || Bukkit.isPrimaryThread()) {
            saveTask.run();
        } else {
            Bukkit.getScheduler().runTask(this, saveTask);
        }
    }

    public void spawnLoadedItems() {
        for (Items item : items) {
            item.forceRespawnNow();
        }
    }

    public void loadItems() {
        items.clear();

        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            getLogger().warning("items.yml not found, loading backup...");
            loadBackupItems();
            return;
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection section = itemsConfig.getConfigurationSection("items");
        if (section == null) {
            getLogger().warning("No items section found in items.yml, loading backup...");
            loadBackupItems();
            return;
        }

        Set<String> chunksToLoad = new HashSet<>();
        Map<String, Map<String, Object>> itemsData = new HashMap<>();

        for (String key : section.getKeys(false)) {
            try {
                UUID.fromString(key);
                Map<String, Object> itemData = section.getConfigurationSection(key).getValues(false);
                itemsData.put(key, itemData);

                Object worldObj = itemData.get("world");
                Object xObj = itemData.get("x");
                Object zObj = itemData.get("z");
                if (worldObj instanceof String && xObj instanceof Number && zObj instanceof Number) {
                    String worldName = (String) worldObj;
                    int chunkX = ((Number) xObj).intValue() >> 4;
                    int chunkZ = ((Number) zObj).intValue() >> 4;
                    chunksToLoad.add(worldName + ":" + chunkX + ":" + chunkZ);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID in items.yml: " + key);
            }
        }

        LoadChunks loader = new LoadChunks(this);

        loader.loadSpecificChunks(chunksToLoad, () -> {
            getLogger().info("Chunks ready. Now restoring items from items.yml...");

            for (Map.Entry<String, Map<String, Object>> entry : itemsData.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> itemData = entry.getValue();

                UUID droppedUUID;
                try {
                    droppedUUID = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                if (Bukkit.getEntity(droppedUUID) != null) {
                    Items item = new Items(itemData);
                    item.setPickable(true);
                    items.add(item);
                    continue;
                }

                Object worldObj = itemData.get("world");
                Object xObj = itemData.get("x");
                Object yObj = itemData.get("y");
                Object zObj = itemData.get("z");

                if (worldObj instanceof String && xObj instanceof Number && yObj instanceof Number
                        && zObj instanceof Number) {
                    World w = Bukkit.getWorld((String) worldObj);
                    if (w == null)
                        continue;

                    Location loc = new Location(w,
                            ((Number) xObj).doubleValue(),
                            ((Number) yObj).doubleValue(),
                            ((Number) zObj).doubleValue());

                    boolean found = false;
                    for (Entity ent : w.getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (ent instanceof ArmorStand stand) {
                            if (stand.getPersistentDataContainer().has(new NamespacedKey(this, "item_uuid"),
                                    PersistentDataType.STRING)) {
                                String id = stand.getPersistentDataContainer().get(new NamespacedKey(this, "item_uuid"),
                                        PersistentDataType.STRING);
                                if (id != null && id.equals(key)) {
                                    Items item = new Items(itemData);
                                    item.setPickable(true);
                                    items.add(item);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found) {
                        Items item = new Items(itemData);
                        item.setPickable(true);
                        items.add(item);
                    }
                } else {
                    Items item = new Items(itemData);
                    item.setPickable(true);
                    items.add(item);
                }
            }

            if (items.isEmpty()) {
                getLogger().warning(
                        "No valid items found in items.yml after chunk load, attempting to load from backup...");
                loadBackupItems();
            } else {
                getLogger().info("Loaded " + items.size() + " items.");
            }
        });
        Bukkit.getScheduler().runTaskLater(this, this::spawnLoadedItems, 40L);
    }

    public void saveBackupItem(Items item) {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);

        Map<String, Object> serialized = item.serialize();
        String id = (String) serialized.get("droppedItem");
        if (id == null)
            return;

        backupConfig.createSection("items." + id, serialized);

        try {
            backupConfig.save(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeBackupItem(Items item) {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        if (!backupFile.exists())
            return;

        Map<String, Object> serialized = item.serialize();
        String id = (String) serialized.get("droppedItem");
        if (id == null)
            return;

        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        backupConfig.set("items." + id, null);

        try {
            backupConfig.save(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadBackupItems() {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        if (!backupFile.exists())
            return;

        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        ConfigurationSection section = backupConfig.getConfigurationSection("items");
        if (section == null)
            return;

        items.clear();
        boolean changed = false;

        for (String key : section.getKeys(false)) {
            try {
                UUID droppedUUID = UUID.fromString(key);
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection == null)
                    continue;

                Map<String, Object> itemData = itemSection.getValues(false);

                Entity ent = Bukkit.getEntity(droppedUUID);
                if (ent instanceof Item droppedEntity) {
                    if (!itemData.containsKey("world") || !itemData.containsKey("x") || !itemData.containsKey("z")
                            || !itemData.containsKey("y")) {
                        Location loc = droppedEntity.getLocation();
                        backupConfig.set("items." + key + ".world", loc.getWorld().getName());
                        backupConfig.set("items." + key + ".x", loc.getX());
                        backupConfig.set("items." + key + ".y", loc.getY());
                        backupConfig.set("items." + key + ".z", loc.getZ());
                        changed = true;
                    }

                    Map<String, Object> migratedData = backupConfig.getConfigurationSection("items." + key)
                            .getValues(false);
                    Items item = new Items(migratedData);
                    item.setDroppedItem(droppedEntity);
                    items.add(item);
                } else {
                    Items item = new Items(itemData);
                    items.add(item);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid UUID in backupitems.yml: " + key);
            }
        }

        if (changed) {
            try {
                backupConfig.save(backupFile);
                getLogger().info("backupitems.yml migrated with location data for found entities.");
            } catch (IOException e) {
                getLogger().severe("Failed to save migrated backupitems.yml: " + e.getMessage());
            }
        }
        Bukkit.getScheduler().runTaskLater(this, this::spawnLoadedItems, 40L);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    private void startItemClearTask() {
        final int clearIntervalSec = 900;
        final List<String> worlds = List.of("SuperiorWorld", "outpost");

        new BukkitRunnable() {
            int remaining = clearIntervalSec;

            @Override
            public void run() {
                remaining--;

                if (remaining == 300) {
                    Bukkit.broadcastMessage(Messages.get("itemclear.warning_5min"));
                } else if (remaining == 30) {
                    Map<String, String> placeholders = Map.of("seconds", String.valueOf(remaining));
                    Bukkit.broadcastMessage(Messages.getFormatted("itemclear.warning_seconds", placeholders));
                }

                if (remaining <= 0) {
                    int totalRemoved = 0;

                    for (String worldName : worlds) {
                        World w = Bukkit.getWorld(worldName);
                        if (w != null) {
                            int removed = 0;
                            for (Entity entity : w.getEntities()) {
                                if (entity instanceof Item) {
                                    entity.remove();
                                    removed++;
                                }
                            }
                            totalRemoved += removed;
                            Bukkit.getLogger().info("Cleared " + removed + " items in " + worldName);
                        }
                    }
                    if (totalRemoved > 0) {
                        Map<String, String> placeholders = Map.of("amount", String.valueOf(totalRemoved));
                        Bukkit.broadcastMessage(Messages.getFormatted("itemclear.cleared_items", placeholders));
                    } else {
                        Bukkit.broadcastMessage(Messages.get("itemclear.cleared_none"));
                    }
                    remaining = clearIntervalSec;
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void startVoteReminderTask() {
        long interval = 20L * 1800; // 30 minutes

        new BukkitRunnable() {
            @Override
            public void run() {
                int current = votePartyManager.getDisplayCurrentVotes();
                int required = votePartyManager.getDisplayRequiredVotes();

                Map<String, String> placeholders = Map.of(
                        "current", String.valueOf(current),
                        "required", String.valueOf(required));

                Bukkit.broadcastMessage(Messages.getFormatted("voteparty.reminder", placeholders));
            }
        }.runTaskTimer(this, 0L, interval);
    }

    private int tipIndex = 0;
    private List<String> tips = new ArrayList<>();

    private void startTipsTask() {
        long interval = 20L * 1200; // 20 minutes

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tips.isEmpty())
                    return;

                String tip = tips.get(tipIndex);

                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', tip));

                tipIndex++;

                if (tipIndex >= tips.size()) {
                    tipIndex = 0;
                }
            }
        }.runTaskTimer(this, 0L, interval);
    }

    public boolean getIsBackup() {
        return isBackingUp;
    }
//Getters
    public static Economy getEconomy() {
        return econ;
    }

    public OutpostManager getOutpostManager() {
        return outpostManager;
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }

    public HidePlayers getHidePlayers() {
        return hidePlayers;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public VotePartyManager getVotePartyManager() {
        return votePartyManager;
    }

    public ChatStyleManager getChatStyleManager() {
        return chatStyleManager;
    }

    public WeeklyLeaderboardTask getWeeklyLeaderboardTask() {
        return weeklyLeaderboardTask;
    }

    public FileConfiguration getItemConfig() {
        return this.itemConfig;
    }

    public StoreMasteryManager getMasteryManager() {return this.storeMasteryManager;}
    public static SkillTreeConfig getSkillTreeConfig(){return skillTreeConfig;}
    public SkillService getSkillService(){return this.skillService;}


}
