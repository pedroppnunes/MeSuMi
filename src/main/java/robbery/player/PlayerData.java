package robbery.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import robbery.core.Robbery;
import robbery.backpacks.BackpackManager;
import robbery.backpacks.Backpacks;
import robbery.booster.Booster;
import robbery.booster.BoosterItem;
import robbery.booster.BoosterManager;
import robbery.hotbar.HotbarListener;
import robbery.mechanics.InventoryManager;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.quest.QuestProgress;
import robbery.ranks.Rank;
import robbery.ranks.RankManager;
import robbery.skilltree.SkillPerk;
import robbery.skilltree.SkillTreeItem;
import robbery.tool.ToolManager;
import robbery.tool.Tools;

import java.util.*;

import static robbery.attribute.Attribute.*;
import static robbery.backpacks.BackpackManager.BACK1;
import static robbery.keys.KeyManager.STORE1;
import static robbery.ranks.RankManager.NONE;
import static robbery.tool.ToolManager.TOOL1;

public class PlayerData {

    private Backpacks backpack;
    private Tools tool;
    private Keys key;
    private long xp = 0L;
    private int level = 1;
    private final Deque<Booster> activeBoosters = new ArrayDeque<>();
    private boolean boostersPaused = false;
    private BukkitTask boosterTask;
    private final Map<String, Integer> boosters = new TreeMap<>();
    private Set<String> keys = new HashSet<>();
    private final Set<String> backpackunlucked = new HashSet<>();
    private final Set<String> toolsunlocked = new HashSet<>();
    private int skillpoints;
    private int itemsStolen;
    private final Map<String, Integer> storeItems = new java.util.HashMap<>();
    private final Map<String, Integer> storeMilestones = new java.util.HashMap<>();
    private final Map<String, Integer> skillTreeLevels = new HashMap<>();
    private final Map<String, Double> perkValues = new HashMap<>();
    private final Map<String, Long> temporaryPerks = new HashMap<>();
    private double itemStreakBonus = 0.0;
    private long lastItemStolenTimestamp = 0;

    private final Map<String, Integer> itemStolenCounts = new HashMap<>();
    private int bustedCount = 0;
    private final Map<String, Long> storePlaytime = new HashMap<>();
    private final Map<Integer, Long> prestigePlaytime = new HashMap<>();
    private final Map<String, Long> prestigeStorePlaytime = new HashMap<>();
    private final Map<Integer, Integer> prestigeItemsStolen = new HashMap<>();
    private final Map<String, Integer> prestigeStoreItemsStolen = new HashMap<>();

    private final List<String> offeredDailyQuests = new ArrayList<>();
    private final Set<String> acceptedDailyQuests = new HashSet<>();
    private int dailyQuestsCompleted = 0;
    private final Map<String, QuestProgress> questProgress = new HashMap<>();
    private boolean talkedToQuestNPC = false;
    public boolean hasTalkedToCryptoBatteryNPC() {
        return talkedToCryptoBatteryNPC;
    }

    public void setTalkedToCryptoBatteryNPC(boolean talked) {
        this.talkedToCryptoBatteryNPC = talked;
    }

    private boolean talkedToCryptoNPC = false;
    private boolean talkedToCryptoBatteryNPC = false;
    private boolean talkedToShopSellNPC = false;
    private String profilePrivacy = "HIDEOUT";

    public String getProfilePrivacy() {
        if (profilePrivacy == null || profilePrivacy.isEmpty()) return "HIDEOUT";
        return profilePrivacy;
    }

    public void setProfilePrivacy(String profilePrivacy) {
        this.profilePrivacy = profilePrivacy != null ? profilePrivacy.toUpperCase() : "HIDEOUT";
    }

    public boolean hasTalkedToShopSellNPC() {
        return talkedToShopSellNPC;
    }

    public void setTalkedToShopSellNPC(boolean talked) {
        this.talkedToShopSellNPC = talked;
    }

    public boolean hasTalkedToCryptoNPC() {
        return talkedToCryptoNPC;
    }

    public void setTalkedToCryptoNPC(boolean talked) {
        this.talkedToCryptoNPC = talked;
    }
    private int lastResetDay = -1;

    private boolean godOfRobberyTag = false;

    private long lastDailyQuestPick = 0L;
    private int resetSkillTreePoints = 0;
    private Rank rank;
    private final Player player;
    private int prestige;
    private double boostx = 0.0;
    private boolean doubleJump = true;
    private double hideoutValueContributed = 0.0;

    public PlayerData(Player p) {
        this.backpack = BACK1;
        this.tool = TOOL1;
        this.key = STORE1;
        this.keys.add("store1");
        this.backpackunlucked.add("back1");
        this.toolsunlocked.add("tool1");
        this.player = p;
        this.prestige = 0;
        this.skillpoints = 0;
        this.rank = NONE;
        this.itemsStolen = 0;
        this.hideoutValueContributed = 0.0;
    }

    public double getHideoutValueContributed() {
        return hideoutValueContributed;
    }

    public void setHideoutValueContributed(double value) {
        this.hideoutValueContributed = value;
    }

    public void addHideoutValueContributed(double amount) {
        if (amount > 0) {
            this.hideoutValueContributed += amount;
        }
    }

//Backpacks
    public Backpacks getBackpack() {
        return backpack;
    }

    public String getBackpackunlucked() {
        StringBuilder total = new StringBuilder();
        for (String s : backpackunlucked)
            total.append("_").append(s);
        return total.toString();
    }

    public void addBackpackName(String backpack) {
        backpackunlucked.add(backpack);
    }

    public String getBackpackString() {
        return backpack.getName() + "_" + backpack.getcapacity() + "_" + backpack.getPrice();
    }

    public String getBackpackItemsString() {
        return backpack.toString();
    }

    public boolean hasBackpackName(String name) {
        return backpackunlucked.contains(name);
    }

    public void setBackpackunlucked(String set) {
        Scanner scanner = new Scanner(set);
        scanner.useDelimiter("_");
        while (scanner.hasNext()) {
            backpackunlucked.add(scanner.next());
        }
    }

    public int getBackpackUnlocked(){
        return backpackunlucked.size();
    }

    public void setnewBackpack(Backpacks b) {
        this.backpack.emptyBackpack();
        this.backpack = b;
    }

//Tools
    public Tools getTool() {
        return tool;
    }

    public String getToolString() {
        return ToolManager.getToolsNameR(tool.getName());
    }

    public void setTool(Tools t) {
        if (t == null)
            this.tool = TOOL1;
        else
            this.tool = t;
    }

    public boolean hasToolName(String name) {
        return toolsunlocked.contains(name);
    }

    public void addToolsName(String tool) {
        toolsunlocked.add(tool);
    }

    public void setToolsunlocked(String set) {
        Scanner scanner = new Scanner(set);
        scanner.useDelimiter("_");
        while (scanner.hasNext()) {
            toolsunlocked.add(scanner.next());
        }
    }

    public String getToolsunlucked() {
        StringBuilder total = new StringBuilder();
        for (String s : toolsunlocked)
            total.append("_").append(s);
        return total.toString();
    }

    public int getToolsUnlocked(){
        return toolsunlocked.size();
    }

//Keys
    public String getKeysString() {
        StringBuilder stores = new StringBuilder();
        for (String k : keys) {
            stores.append("_").append(k);
        }
        return stores.toString();
    }

    public String getKeyString() {
        if (this.key == null)
            return STORE1.getName();
        return KeyManager.getStoreNameR(key.getName());
    }

    public void setKeys(String string) {
        Scanner scanner = new Scanner(string);
        scanner.useDelimiter("_");
        while (scanner.hasNext()) {
            keys.add(scanner.next());
        }
    }


    public boolean hasKey(String k) {
        return keys.contains(k);
    }

    public void addKey(String k) {
        keys.add(k);
    }

    public Keys getKey() {
        return key;
    }

    public void setKey(Keys k) {
        if (k == null)
            this.key = STORE1;
        else
            this.key = k;
    }

//Prestige
    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int p) {
        this.prestige = p;
    }

    public void prestigeKeys() {
        this.key = STORE1;
        keys = new HashSet<>();
        keys.add("store1");
    }

//General stuff
    public void setBoostersPaused(boolean paused){
        this.boostersPaused = paused;
    }

    public void addItemToBackpack(Items item) {
        String storeId = "store1";
        if (item != null && item.getId() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(item.getId());
            if (m.find()) {
                storeId = "store" + m.group();
            }
        }
        addItemToBackpack(item, storeId);
    }

    public void addItemToBackpack(Items item, String storeId) {
        backpack.addBackpackItem(item, getBoost(storeId));
    }

    public void busted() {
        this.getBackpack().emptyBackpack();
        this.incrementBustedCount();
    }

    public void setBackpack(Backpacks b) {
        this.backpack = Objects.requireNonNullElse(b, BACK1);
    }


    public double getBoost() {
        return getBoost("store1");
    }

    public double getBoost(String storeId) {
        double boost = 1
                + prestige * 0.10
                + rank.boost()
                + boostx
                + getOutMoneyMult()
                + getPerkValue(PERK_MONEY_MULT1)
                + getPerkValue(PERK_MONEY_MULT2)
                + getStoreMasteryMoneyMultiplier(storeId);

        if (hasTemporaryPerk(PERK_ABILITY_MONEYMULT1)) {
            boost += 0.5;
        }

        return boost;
    }

    public double getPrestigeBoost(){
        return (1 + prestige*0.10);
    }

    public String getRank(){
        return rank.rank();
    }
    public void setRank(String rank){
        this.rank = RankManager.getRank(rank);
        refreshBackpackSlots();
    }
    public void toggleDoubleJump(){
        doubleJump = !doubleJump;
    }
    public boolean isDoubleJump(){
        return doubleJump;
    }
    public int getExtraSlots() { return (int) (rank.extraSlots() + getPerkValue(PERK_BACK_SLOTS1));}
    public double getExtraDamage() {
        return getExtraDamage("store1");
    }

    public double getExtraDamage(String storeId) {
        double baseDamage = rank.extraDamage()*100
                + getOutSpeedBonus()
                + getPerkValue(PERK_STEAL_SPEED1)
                + getPerkValue(PERK_STEAL_SPEED2)
                + getItemStreakBonus()
                + getStoreMasteryStealSpeed(storeId);

        if (hasTemporaryPerk(PERK_ABILITY_STEALSPEED1)) {
            baseDamage += 50;
        }

        if (this.player != null) {
            robbery.crypto.CryptoMachine machine = robbery.core.Robbery.getInstance().getCryptoManager().getMachine(this.player.getUniqueId());
            if (machine != null && machine.getFuelTicks() > 0) {
                baseDamage += 5.0;
            }
        }

        return baseDamage;
    }
    public int getExtraPvSlots() {
        return rank.extraSlots();
    }

//Player
    public Player getPlayer() {
        return player;
    }

    public void giveToolToInv() {
        InventoryManager.giveItem(player, tool.getItem(), 0);
    }

    public void giveBackpackToInv(){
        InventoryManager.replaceChestplate(player, backpack.getItem());
    }

    public void giveBooster(JavaPlugin plugin) {
        InventoryManager.giveItem(player, BoosterItem.createBoosterItem(plugin), 1);
    }
    public void giveFeather(){
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bFlight");
        meta.setLore(Arrays.asList("§7Right click to fly for 5 seconds", "§c5 minute cooldown"));
        item.setItemMeta(meta);
        InventoryManager.giveItem(player, item, 7);
    }

    public void giveSkillTree(Robbery plugin){
        InventoryManager.giveItem(player, SkillTreeItem.createSkillTreeItem(plugin),2);
    }

    public void giveMainmenu(Robbery plugin){
        InventoryManager.giveItem(player, HotbarListener.createMainMenuItem(plugin), 8);
    }

//Items Stolen
    public int getItemsStolen() {
        return itemsStolen;
    }

//Boosters
    public void setBoosters(String name, Player player) {
        if (!boosters.containsKey(name) || boosters.get(name) == 0) return;

        Booster newBoost = BoosterManager.getBooster(name);
        boosters.put(name, boosters.get(name) - 1);
        int minutes = newBoost.getSeconds() / 60;

        if (activeBoosters.isEmpty()) {
            activeBoosters.addLast(newBoost);
            startNextBooster(player);
            Messages.sendFormatted(player, "boosters.activated", Map.of(
                    "boost_name", newBoost.getName(),
                    "minutes", String.valueOf(minutes)
            ));
            return;
        }

        Booster current = activeBoosters.peekFirst();

        if (current.getPriority() < newBoost.getPriority()) {
            activeBoosters.addFirst(newBoost);
            Messages.sendFormatted(player, "boosters.activated", Map.of(
                    "boost_name", newBoost.getName(),
                    "minutes", String.valueOf(minutes)
            ));
            startNextBooster(player);
        } else if (current.getPriority() == newBoost.getPriority()) {
            current.addTime(newBoost.getSeconds());
            Messages.sendFormatted(player, "boosters.added_time", "minutes", String.valueOf(minutes));
        } else {
            activeBoosters.addLast(newBoost);
            Messages.sendFormatted(player, "boosters.queued", "boost_name", newBoost.getName());
        }
    }

    private void startNextBooster(Player player) {
        if (boosterTask != null) boosterTask.cancel();
        if (activeBoosters.isEmpty()) {
            boostx = 0.0;
            return;
        }

        Booster active = activeBoosters.peekFirst();
        boostx = active.getMultiplier();

        boosterTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    boostx = 0.0;
                    return;
                }

                active.setSeconds(active.getSeconds() - 1);
                if (active.getSeconds() <= 0) {

                    Messages.send(player,"boosters.expired");
                    activeBoosters.pollFirst();
                    this.cancel();
                    startNextBooster(player);
                }
            }
        }.runTaskTimer(Robbery.getInstance(), 20L, 20L);
    }

    public void stopBoosters() {
        boostersPaused = true;
        if (boosterTask != null) boosterTask.cancel();
        boostx = 0.0;
    }

    public void resumeBoosters(Player player) {
        if (!boostersPaused) return;
        boostersPaused = false;
        startNextBooster(player);
    }

    public boolean isBoostersPaused() {
        return boostersPaused;
    }

    public void addBoosters(Booster boost) {
        boosters.put(boost.getTag(), boosters.getOrDefault(boost.getTag(), 0) + 1);
    }

    public String getBoostersString() {
        StringBuilder total = new StringBuilder();
        for (String k : boosters.keySet()) {
            total.append(",").append(k).append(",").append(boosters.getOrDefault(k, 0));
        }
        return total.toString();
    }

    public void setBoostersFromString(String total) {
        Scanner scanner = new Scanner(total);
        scanner.useDelimiter(",");
        while (scanner.hasNext()) {
            String key = scanner.next();
            int quantity = Integer.parseInt(scanner.next());
            boosters.put(key, quantity);
        }
    }



    public Booster getActiveboost() {
        if (activeBoosters.isEmpty()) {
            return new Booster("None", 1.0, 0, 0, "null");
        }
        return activeBoosters.peekFirst();
    }

    public String getActiveBoostString() {
        if (activeBoosters.isEmpty()) return "none";

        StringBuilder builder = new StringBuilder();
        for (Booster b : activeBoosters) {
            if (!builder.isEmpty()) builder.append("|");
            builder.append(b.getTag()).append("_").append(b.getSeconds());
        }
        return builder.toString();
    }

    public void setActiveBooster(String string) {
        activeBoosters.clear();

        if (string == null || string.equalsIgnoreCase("none") || string.isEmpty()) {
            boostx = 0.0;
            return;
        }

        String[] parts = string.split("\\|");

        for (String part : parts) {
            String[] data = part.split("_");
            if (data.length != 2) continue;

            Booster template = BoosterManager.getBooster(data[0]);
            if (template == null) continue;

            try {
                int seconds = Integer.parseInt(data[1]);
                if (seconds <= 0) continue;

                Booster booster = new Booster(
                        template.getName(),
                        template.getMultiplier(),
                        seconds,
                        template.getPriority(),
                        template.getTag()
                );

                activeBoosters.addLast(booster);

            } catch (NumberFormatException ignored) {
            }
        }
        if (boostersPaused) {
            boostx = 0.0;
            return;
        }
        if (!activeBoosters.isEmpty()) {
            startNextBooster(player);
        } else {
            boostx = 0.0;
        }
    }


    public int getBoosterQuantity(String name) {
        return boosters.getOrDefault(name,0);
    }

//Skillpoints & XP
    public void setSP(String sp){
        if(sp == null)
            this.skillpoints = 0;
        else
            this.skillpoints = Integer.parseInt(sp);
    }
    public long getXp() {
        return xp;
    }
    public double getXPBoost(){
        return getXPBoost("store1");
    }

    public double getXPBoost(String storeId){
        double xpBoost = rank.xpboost() + getPerkValue(PERK_XP1) + getPerkValue(PERK_XP2) + getStoreMasteryRobberyXp(storeId);
        
        if (this.player != null) {
            robbery.crypto.CryptoMachine machine = robbery.core.Robbery.getInstance().getCryptoManager().getMachine(this.player.getUniqueId());
            if (machine != null && machine.getFuelTicks() > 0) {
                xpBoost += 0.05;
            }
        }
        
        return xpBoost;
    }
    public void setXp(long xp) {
        this.xp = Math.max(0L, xp);
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }
    public void addSkillPoints(int n) {
        this.skillpoints += n;
    }
    public void setSkillPoints(int n) {
        this.skillpoints = Math.max(0, n);
    }
    public int getSkillPoints() {
        return skillpoints;
    }

//Store Milestones
public int getStoreItems(String storeId) {
    return storeItems.getOrDefault(storeId, 0);
}

    public void addStoreItems(String storeId, int amount) {
        int current = getStoreItems(storeId);
        storeItems.put(storeId, current + amount);
    }

    public Map<String, Integer> getStoreItemsMap() {
        return storeItems;
    }

    public void setStoreItemsMap(Map<String, Integer> map) {
        storeItems.clear();
        if (map != null) storeItems.putAll(map);
    }

    public int getStoreMilestone(String storeId) {
        return storeMilestones.getOrDefault(storeId, 0);
    }

    public void setStoreMilestone(String storeId, int milestoneLevel) {
        storeMilestones.put(storeId, milestoneLevel);
    }

    public void addStoreMilestone(String storeId, int amount) {
        int current = getStoreMilestone(storeId);
        storeMilestones.put(storeId, current + amount);
    }

    public Map<String, Integer> getStoreMilestoneMap() {
        return storeMilestones;
    }

    public void setStoreMilestoneMap(Map<String, Integer> map) {
        storeMilestones.clear();
        if (map != null) storeMilestones.putAll(map);
    }

    public void addItemsStolen(int amount) {
        this.itemsStolen += amount;
    }

    public void setItemsStolen(int amount) {
        this.itemsStolen = amount;
    }

    // --- Statistics & Completionist Methods ---

    public int getItemStolenCount(String itemId) {
        if (itemId == null) return 0;
        return itemStolenCounts.getOrDefault(itemId.toLowerCase(), 0);
    }

    public void incrementItemStolenCount(String itemId, String storeId) {
        if (itemId == null) return;
        String key = itemId.toLowerCase();
        itemStolenCounts.put(key, itemStolenCounts.getOrDefault(key, 0) + 1);

        // Track per-prestige item stats
        int pLevel = getPrestige();
        prestigeItemsStolen.put(pLevel, prestigeItemsStolen.getOrDefault(pLevel, 0) + 1);
        if (storeId != null && !storeId.isEmpty()) {
            String pStoreKey = "p" + pLevel + "_" + storeId.toLowerCase();
            prestigeStoreItemsStolen.put(pStoreKey, prestigeStoreItemsStolen.getOrDefault(pStoreKey, 0) + 1);
        }
    }

    public Map<String, Integer> getItemStolenCountsMap() {
        return itemStolenCounts;
    }

    public void setItemStolenCountsMap(Map<String, Integer> map) {
        itemStolenCounts.clear();
        if (map != null) itemStolenCounts.putAll(map);
    }

    public int getBustedCount() {
        return bustedCount;
    }

    public void incrementBustedCount() {
        this.bustedCount++;
    }

    public void setBustedCount(int count) {
        this.bustedCount = Math.max(0, count);
    }

    public long getStorePlaytime(String storeId) {
        if (storeId == null) return 0L;
        return storePlaytime.getOrDefault(storeId.toLowerCase(), 0L);
    }

    public void addStorePlaytime(String storeId, long seconds) {
        if (storeId == null || storeId.isEmpty() || seconds <= 0) return;
        String sId = storeId.toLowerCase();
        storePlaytime.put(sId, storePlaytime.getOrDefault(sId, 0L) + seconds);

        int pLevel = getPrestige();
        prestigePlaytime.put(pLevel, prestigePlaytime.getOrDefault(pLevel, 0L) + seconds);

        String pStoreKey = "p" + pLevel + "_" + sId;
        prestigeStorePlaytime.put(pStoreKey, prestigeStorePlaytime.getOrDefault(pStoreKey, 0L) + seconds);
    }

    public Map<String, Long> getStorePlaytimeMap() {
        return storePlaytime;
    }

    public void setStorePlaytimeMap(Map<String, Long> map) {
        storePlaytime.clear();
        if (map != null) storePlaytime.putAll(map);
    }

    public long getPrestigePlaytime(int prestige) {
        return prestigePlaytime.getOrDefault(prestige, 0L);
    }

    public Map<Integer, Long> getPrestigePlaytimeMap() {
        return prestigePlaytime;
    }

    public void setPrestigePlaytimeMap(Map<Integer, Long> map) {
        prestigePlaytime.clear();
        if (map != null) prestigePlaytime.putAll(map);
    }

    public long getPrestigeStorePlaytime(int prestige, String storeId) {
        if (storeId == null) return 0L;
        String key = "p" + prestige + "_" + storeId.toLowerCase();
        return prestigeStorePlaytime.getOrDefault(key, 0L);
    }

    public Map<String, Long> getPrestigeStorePlaytimeMap() {
        return prestigeStorePlaytime;
    }

    public void setPrestigeStorePlaytimeMap(Map<String, Long> map) {
        prestigeStorePlaytime.clear();
        if (map != null) prestigeStorePlaytime.putAll(map);
    }

    public int getPrestigeItemsStolen(int prestige) {
        return prestigeItemsStolen.getOrDefault(prestige, 0);
    }

    public Map<Integer, Integer> getPrestigeItemsStolenMap() {
        return prestigeItemsStolen;
    }

    public void setPrestigeItemsStolenMap(Map<Integer, Integer> map) {
        prestigeItemsStolen.clear();
        if (map != null) prestigeItemsStolen.putAll(map);
    }

    public int getPrestigeStoreItemsStolen(int prestige, String storeId) {
        if (storeId == null) return 0;
        String key = "p" + prestige + "_" + storeId.toLowerCase();
        return prestigeStoreItemsStolen.getOrDefault(key, 0);
    }

    public Map<String, Integer> getPrestigeStoreItemsStolenMap() {
        return prestigeStoreItemsStolen;
    }

    public void setPrestigeStoreItemsStolenMap(Map<String, Integer> map) {
        prestigeStoreItemsStolen.clear();
        if (map != null) prestigeStoreItemsStolen.putAll(map);
    }

    /**
     * Finds which store has taken the most playtime during a specific prestige level.
     * @param prestige the prestige level
     * @return String representation formatted as "StoreName (FormattedTime)" or "None"
     */
    public String getMostTimeConsumingStore(int prestige) {
        String maxStoreId = null;
        long maxTime = 0L;

        for (int i = 1; i <= 12; i++) {
            String sId = "store" + i;
            long time = getPrestigeStorePlaytime(prestige, sId);
            if (time > maxTime) {
                maxTime = time;
                maxStoreId = sId;
            }
        }

        if (maxStoreId == null || maxTime <= 0) return "None";
        String storeName = robbery.keys.KeyManager.getStoreN(maxStoreId);
        if (storeName == null) storeName = maxStoreId;
        return storeName + " (" + formatSeconds(maxTime) + ")";
    }

    private String formatSeconds(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public int getStoreMastery(String storeId) {
        return getStoreItems(storeId);
    }

    /** Returns the store mastery milestone level (0-10) for this store. */
    public int getStoreMasteryLevel(String storeId) {
        return robbery.core.Robbery.getInstance().getMasteryManager().getLevelFromItems(storeId, getStoreItems(storeId));
    }

    /**
     * M1 = +10% money multiplier, M6 = +25% total (extra +15%)
     * Applied only when selling/earning in the specific store.
     */
    public double getStoreMasteryMoneyMultiplier(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 6) return 0.25;
        if (level >= 1) return 0.10;
        return 0.0;
    }

    /**
     * M2 = +10% steal speed, M7 = +25% total (extra +15%)
     * Applied only when stealing in the specific store.
     */
    public double getStoreMasteryStealSpeed(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 7) return 25.0;
        if (level >= 2) return 10.0;
        return 0.0;
    }

    /**
     * M3 = +5% Robbery XP gain
     * Applied only when earning XP from items stolen in the specific store.
     */
    public double getStoreMasteryRobberyXp(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 3) return 0.05;
        return 0.0;
    }

    /**
     * M4 = +1% chance to gain a bonus Skill Point on sell
     * Applied only in the specific store context.
     */
    public double getStoreMasterySkillPointChance(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 4) return 0.01;
        return 0.0;
    }

    /**
     * M8 = +1% chance to get a doubled item count when stealing
     * Applied only in the specific store context.
     */
    public double getStoreMasteryDoubleItemChance(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 8) return 0.01;
        return 0.0;
    }

    /**
     * M9 = +1% chance to instantly steal (skip progress bar)
     * Applied only in the specific store context.
     */
    public double getStoreMasteryInstaStealChance(String storeId) {
        int level = getStoreMasteryLevel(storeId);
        if (level >= 9) return 0.01;
        return 0.0;
    }

//SkillTree
    public int getSkillTreeLevel(String tierId) {
        return skillTreeLevels.getOrDefault(tierId, 0);
    }
    public void setSkillTreeLevel(String tierId, int level) {
        if (level <= 0) skillTreeLevels.remove(tierId);
        else skillTreeLevels.put(tierId, level);
    }
    public Map<String,Integer> getAllSkillTreeLevels() { return skillTreeLevels; }
    public int getResetSkillTreePoints() {
        return resetSkillTreePoints;
    }
    public void addResetSkillTreePoints(int amount) {
        this.resetSkillTreePoints = Math.max(0, this.resetSkillTreePoints + amount);
    }

    public boolean consumeResetSkillTreePoint() {
        if (resetSkillTreePoints <= 0) return false;
        resetSkillTreePoints--;
        return true;
    }
    public void setResetSkillTreePoints(int amount){
        this.resetSkillTreePoints = amount;
    }

//Outpost Buffs
    public void setOutSkillpointChance(int value) {
        setPerkValue(ATTR_SKILLPOINT_CHANCE, value / 100.0);
    }

    public void setOutBoosterChance(int value) {
        setPerkValue(ATTR_BOOSTER_CHANCE, value / 100.0);
    }

    public void setOutSpeedBonus(int value) {
        setPerkValue(ATTR_SPEED_BONUS, value);
    }

    public void setOutMooneyMult(double mult){
        setPerkValue(ATTR_MONEY_MULT, mult);
    }

    public double getBoostx() {
        return boostx;
    }

    public double getOutBoosterChance() {
        double mult = getPerkValue(PERK_OUT_BUFF1) / 100.0;
        return getPerkValue(ATTR_BOOSTER_CHANCE) * (1.0 + mult);
    }

    public double getOutSpChance() {
        double mult = getPerkValue(PERK_OUT_BUFF1) / 100.0;
        return getPerkValue(ATTR_SKILLPOINT_CHANCE) * (1.0 + mult);
    }

    public double getOutSpeedBonus(){
        double mult = getPerkValue(PERK_OUT_BUFF1) / 100.0;
        return getPerkValue(ATTR_SPEED_BONUS) * (1.0 + mult);
    }

    private double getOutMoneyMult() {
        double mult = getPerkValue(PERK_OUT_BUFF1) / 100.0;
        return getPerkValue(ATTR_MONEY_MULT) * (1.0 + mult);
    }
//Attributes
    public void setPerkValue(String perkId, double value) {
        if (value == 0) {
            perkValues.remove(perkId);
        } else {
            double finalValue = Math.round(value * 1000.0) / 1000.0;
            perkValues.put(perkId, finalValue);
        }
        if (perkId.equals(PERK_BACK_SLOTS1)) {
            refreshBackpackSlots();
        }
    }

    public void refreshBackpackSlots() {
        String baseName = this.backpack != null ? BackpackManager.getBackpackNameR(this.backpack.getName()) : "back1";
        if (baseName == null) baseName = "back1";

        int totalExtraSlots = getExtraSlots();
        Backpacks newPack = BackpackManager.getBackpackName(baseName, totalExtraSlots);
        if (newPack != null) {
            if (this.backpack != null) {
                for (robbery.items.Items it : this.backpack.getItems()) {
                    if (!newPack.isFull()) {
                        newPack.getItems().add(it);
                    }
                }
            }
            this.setBackpack(newPack);
        }

        if (this.player != null && this.player.isOnline()) {
            giveBackpackToInv();
        }
    }

    public double getPerkValue(String perkId) {
        if (perkId == null) return 0.0;
        Double val = perkValues.get(perkId);
        if (val != null && val > 0.0) {
            if (PERCENTAGE_PERKS.contains(perkId) && val <= 1.0) {
                val = val * 100.0;
                perkValues.put(perkId, val);
            }
            return val;
        }
        int lvl = getSkillTreeLevel(perkId);
        if (lvl > 0 && robbery.core.Robbery.getSkillTreeConfig() != null) {
            robbery.skilltree.SkillPerk perk = robbery.core.Robbery.getSkillTreeConfig().getTier(perkId);
            if (perk != null) {
                double calculated = perk.valueForLevel(lvl);
                perkValues.put(perkId, calculated);
                return calculated;
            }
        }
        return val != null ? val : 0.0;
    }

    public boolean hasPerk(String perkId) {
        return getPerkValue(perkId) > 0;
    }
    public Map<String,Double> getAllPerkValues() { return perkValues; }


    public boolean canBuyPerk(SkillPerk perk) {
        if (perk.requiredPerks().isEmpty()) return true;

        Set<String> requireMax = Set.of("featherflight", "keyschance", "doublejump");

        for (String req : perk.requiredPerks()) {
            int level = getSkillTreeLevel(req);

            if (requireMax.contains(perk.id())) {
                SkillPerk reqPerk = Robbery.getSkillTreeConfig().getTier(req);
                if (reqPerk != null && level >= reqPerk.maxLevel()) {
                    return true;
                }
            } else {
                if (level > 0) {
                    return true;
                }
            }
        }

        return false;
    }
    public void setTemporaryPerk(String perkId, double durationSeconds) {
        temporaryPerks.put(perkId, System.currentTimeMillis() + (long)(durationSeconds * 1000));
    }

    public boolean hasTemporaryPerk(String perkId) {
        Long expire = temporaryPerks.get(perkId);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            temporaryPerks.remove(perkId);
            return false;
        }
        return true;
    }

    public double getItemStreakBonus() {
        if (System.currentTimeMillis() - lastItemStolenTimestamp > 5000) {
            itemStreakBonus = 0.0;
        }
        return itemStreakBonus;
    }

    public void addItemStreak(double amount) {
        long now = System.currentTimeMillis();
        if (now - lastItemStolenTimestamp > 5000) {
            itemStreakBonus = 0.0;
        }
        itemStreakBonus = Math.min(35.0, itemStreakBonus + amount);
        lastItemStolenTimestamp = now;
    }
//GodOfRobbery

    public boolean hasGodOfRobbery() {
        return godOfRobberyTag;
    }
    public void setGodOfRobbery(boolean value) {
        godOfRobberyTag = value;
    }

//Quests
    public int getHighestOwnedStoreTier() {
        int max = 0;
        if (keys != null) {
            for (String s : keys) {
                if (s.toLowerCase().startsWith("store")) {
                    try {
                        String numPart = s.substring(5); // skips "store"
                        max = Math.max(max, Integer.parseInt(numPart));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        // Prestige scaling: P1 = Balenziaga (10), P2 = Samzung (11), P3+ = The Bank (12)
        if (prestige >= 3) {
            max = Math.max(max, 12);
        } else if (prestige == 2) {
            max = Math.max(max, 11);
        } else if (prestige == 1) {
            max = Math.max(max, 10);
        }
        return Math.max(1, max);
    }

    public void setOfferedDailyQuests(List<String> quests) {
        offeredDailyQuests.clear();
        offeredDailyQuests.addAll(quests);
    }
    public List<String> getOfferedDailyQuests(){ return offeredDailyQuests; }
    public void acceptDailyQuest(String questId) {
        if (!offeredDailyQuests.contains(questId)) return;
        acceptedDailyQuests.add(questId);
        questProgress.put(questId, new QuestProgress(questId));
    }
    public Set<String> getActiveQuest(){return acceptedDailyQuests;}
    public Map<String, QuestProgress> getQuestProgressMap(){ return questProgress; }
    public Set<String> getAcceptedDailyQuests() {
        return acceptedDailyQuests;
    }
    public long getLastDailyQuestPick() { return lastDailyQuestPick; }
    public void setLastDailyQuestPick(long timestamp) { this.lastDailyQuestPick = timestamp; }
    public int getDailyQuestsCompleted() { return dailyQuestsCompleted; }
    public void setDailyQuestsCompleted(int count) { this.dailyQuestsCompleted = count; }
    public void incrementDailyQuestsCompleted() { this.dailyQuestsCompleted++; }
    public boolean hasTalkedToQuestNPC() { return talkedToQuestNPC; }
    public void setTalkedToQuestNPC(boolean talked) { this.talkedToQuestNPC = talked; }
    public int getLastResetDay() { return lastResetDay; }
    public void setLastResetDay(int lastResetDay) { this.lastResetDay = lastResetDay; }
}

