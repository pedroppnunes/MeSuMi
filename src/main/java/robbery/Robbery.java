package robbery;

import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.booster.BoosterItemListener;
import robbery.chat.ChatStyleManager;
import robbery.commands.*;
import robbery.events.*;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.mutes.MuteManager;
import robbery.outpost.CombatManager;
import robbery.outpost.OutpostManager;
import robbery.outpost.OutpostRegion;
import robbery.player.PlayerEventListener;
import robbery.ranks.RankPaperListener;
import robbery.skillpoints.SkillPointListener;
import robbery.votes.VotePartyManager;
import robbery.warnings.ChatWarningListener;
import robbery.warnings.WarningManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Robbery extends JavaPlugin implements Listener {

    private static final Map<String, Items> itemsMap = new ConcurrentHashMap<>();

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
        this.hidePlayers = new HidePlayers(main);
        this.rcrate = new Rcrate();
        this.warningManager = new WarningManager(main);
        this.muteManager = new MuteManager(main);
        this.votePartyManager = new VotePartyManager(main);
        this.chatStyleManager = new ChatStyleManager(getDataFolder());
        getServer().getPluginManager().registerEvents(new VoteListener(main), main);
        BlockCraftListener blockCraft = new BlockCraftListener();
        Messages.init(main);
        getServer().getPluginManager().registerEvents(new InventoryLockListener(), main);
        getServer().getPluginManager().registerEvents(new ArmorStandInteractionListener(main), main);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(main), main);
        getServer().getPluginManager().registerEvents(new BoosterItemListener(main), main);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(main), main);
        getServer().getPluginManager().registerEvents(new HideoutListener(), main);
        getServer().getPluginManager().registerEvents(new SkillPointListener(main), main);
        getServer().getPluginManager().registerEvents(new ChatItemReplacer(main), main);
        getServer().getPluginManager().registerEvents(new CombatManager(main), main);
        getServer().getPluginManager().registerEvents(new ChatWarningListener(main), main);
        getServer().getPluginManager().registerEvents(hidePlayers, main);
        getServer().getPluginManager().registerEvents(blockCraft, main);
        getServer().getPluginManager().registerEvents(rcrate, main);
        getServer().getPluginManager().registerEvents(new RankPaperListener(), main);
        getServer().getPluginManager().registerEvents(new ClaimGuiListener(), main);
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
        Objects.requireNonNull(getCommand("skillpointbuy")).setExecutor(new SkillpointBuy(main));
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
        Objects.requireNonNull(getCommand("spawn")).setExecutor(new SpawnCommand());
        Objects.requireNonNull(getCommand("ct")).setExecutor(new ChatColorCommand(main));
        Objects.requireNonNull(getCommand("lobby")).setExecutor(new Lobby(main));
        Objects.requireNonNull(getCommand("weeklyleaderboard")).setExecutor(new WeeklyLeaderboardCommand(main));
        getServer().getMessenger().registerOutgoingPluginChannel(main, "BungeeCord");
        World outpostWorld = Bukkit.getWorld("outpost");
        if (outpostWorld == null) {
            getLogger().severe("World 'outpost' not found!");
            return;
        }
        OutpostRegion region = new OutpostRegion(outpostWorld, -3, 1, -3, 4, 1, 4);
        outpostManager = new OutpostManager(main, region, 30);
        startItemClearTask();
        blockCraft.removeRecipes();
        addItemstoMap();

        long ticksPerDay = 24 * 60 * 60 * 20L;
        long now = System.currentTimeMillis();
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atStartOfDay();
        long delayTicks = (nextMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - now) / 50;
        getServer().getScheduler().runTaskTimer(main, votePartyManager::resetVotes, delayTicks, ticksPerDay);

        saveDefaultConfig();

        String webhookUrl = "https://discord.com/api/webhooks/1418305594716192830/LqLGQ8B-f6JRkBJw0fyQVndskVuWx8GnLpaD33lHmQwwlZw5IN_CrkBoV-YdpzLHFmqS";
        weeklyLeaderboardTask = new WeeklyLeaderboardTask(this, webhookUrl);
    }

    public void addItemstoMap(){
        itemsMap.clear();
        File file = new File(getDataFolder(), "additems.yml");
        if (!file.exists()) {
            saveResource("additems.yml", false);
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            double hp = section.getDouble(key + ".hp");
            int value = section.getInt(key + ".value");
            String playername = section.getString(key + ".playername");
            int time = section.getInt(key + ".time");

            String name = key.substring(key.indexOf('_') + 1);
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            Items it = new Items(hp, value, name, playername, time);
            itemsMap.put(key.toLowerCase(), it);
        }
    }

    @Override
    public void onDisable() {
        this.isBackingUp = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(Component.text(Messages.get("reload.player-kick")));
        }
        
        getLogger().info("Shutting Down Robbery!");
        saveItems();
    }


    public static Map<String, Items> getItemsMap(){
        return itemsMap;
    }

    public void addItems(Items i){
        items.add(i);
        saveBackupItems();
    }
    public List<Items> getItems(){
        return items;
    }

    public void removeItem(Items i){
        items.remove(i);
        i.remove();
        saveBackupItems();
    }
    public static Robbery getInstance() {
        return main;
    }
    public static Items getItemsbyName(String name){
        return itemsMap.getOrDefault(name,null);
    }

    public void saveItems() {
        File itemsFile = new File(getDataFolder(), "items.yml");
        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (Items item : items) {
            serializedItems.add(item.serialize());
        }
        itemsConfig.set("items", serializedItems);

        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadItems() {
        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            loadBackupItems();
            return;
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        List<Map<String, Object>> serializedItems = (List<Map<String, Object>>) itemsConfig.getList("items");

        if (serializedItems != null) {
            for (Map<String, Object> itemData : serializedItems) {
                Object droppedIdObj = itemData.get("droppedItem");
                if (droppedIdObj instanceof String droppedId) {
                    try {
                        UUID droppedUUID = UUID.fromString(droppedId);
                        if (Bukkit.getEntity(droppedUUID) != null) {
                            Items item = new Items(itemData);
                            items.add(item);
                        }
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid UUID in items.yml: " + droppedId);
                    }
                }
            }
        }
    }

    public void saveBackupItems() {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);

        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (Items item : items) {
            serializedItems.add(item.serialize());
        }

        backupConfig.set("items", serializedItems);

        try {
            backupConfig.save(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadBackupItems() {
        File backupFile = new File(getDataFolder(), "backupitems.yml");
        if (!backupFile.exists()) {
            return;
        }

        FileConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
        List<Map<String, Object>> serializedItems = (List<Map<String, Object>>) backupConfig.getList("items");

        if (serializedItems != null) {
            for (Map<String, Object> itemData : serializedItems) {
                Object droppedIdObj = itemData.get("droppedItem");
                if (droppedIdObj instanceof String droppedId) {
                    try {
                        UUID droppedUUID = UUID.fromString(droppedId);
                        if (Bukkit.getEntity(droppedUUID) != null) {
                            Items item = new Items(itemData);
                            items.add(item);
                        }
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid UUID in backupitems.yml: " + droppedId);
                    }
                }
            }
        }
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

    public static Economy getEconomy() {
        return econ;
    }

    public OutpostManager getOutpostManager() {
        return outpostManager;
    }
    public WarningManager getWarningManager() {
        return warningManager;
    }
    public HidePlayers getHidePlayers(){
        return hidePlayers;
    }
    public MuteManager getMuteManager(){return muteManager;}
    public VotePartyManager getVotePartyManager(){return votePartyManager;}
    public ChatStyleManager getChatStyleManager() {return chatStyleManager;}
    public WeeklyLeaderboardTask getWeeklyLeaderboardTask(){return weeklyLeaderboardTask;}

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
                } else if (remaining == 30){
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

    public boolean getIsBackup(){
        return isBackingUp;
    }

}
