package robbery.player;

import org.bukkit.Bukkit;
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
import robbery.mechanics.InventoryManager;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.ranks.Rank;
import robbery.ranks.RankManager;
import robbery.skillpoints.SkillPoint;
import robbery.skillpoints.SkillPointItem;
import robbery.tool.ToolManager;
import robbery.tool.Tools;

import java.util.*;

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

    private SkillPoint sp;
    private Rank rank;
    private final Player player;
    private int prestige;
    private double boostx = 0.0;
    private boolean doubleJump = true;
    private double outboost = 0.0;
    private double outspchance = 0.0;
    private double outboosterchance = 0.0;
    private double outspeed = 0.0;

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
        this.sp = new SkillPoint(0,0,0,0,0,0,0);
        this.rank = NONE;
        this.xp = 0L;
        this.level = 1;
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
        backpack.addBackpackItem(item,getBoost());
    }

    public void busted() {
        this.getBackpack().emptyBackpack();
    }

    public void setBackpack(Backpacks b) {
        this.backpack = Objects.requireNonNullElse(b, BACK1);
    }


    public double getBoost() {
        return 1 + prestige * 0.10 + rank.boost() + sp.extraMoney() + outboost + boostx;
    }
    public double getPrestigeBoost(){
        return (1 + prestige*0.10);
    }

    public String getRank(){
        return rank.rank();
    }
    public void setRank(String rank){
        this.rank = RankManager.getRank(rank);
        String name = BackpackManager.getBackpackNameR(backpack.getName());
        setBackpack(BackpackManager.getBackpackName(Objects.requireNonNullElse(name, "back1"), this.rank.extraSlots() + sp.extraSlots()));
    }
    public void toggleDoubleJump(){
        doubleJump = !doubleJump;
    }
    public boolean isDoubleJump(){
        return doubleJump;
    }
    public int getExtraSlots() { return rank.extraSlots() + sp.extraSlots();}
    public double getExtraDamage(){return rank.extraDamage() * (1+sp.extraDamage()+outspeed);}

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
        InventoryManager.giveItem(player, item, 8);
    }

    public void giveSkillPoint(Robbery plugin){
        InventoryManager.giveItem(player, SkillPointItem.createSkillPointItem(plugin),2);
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

    public void stopBoosters(Player player) {
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

    public void setOutpostBoost(double boost){
        this.outboost = boost;
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

        // If boosters are paused, don't start timer
        if (boostersPaused) {
            boostx = 0.0;
            return;
        }

        // Start ticking if any exist
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
    public int getSP(){ return skillpoints;}
    public void addSkillpoint(){
        this.skillpoints++;
    }
    public void setSP(String sp){
        if(sp == null)
            this.skillpoints = 0;
        else
            this.skillpoints = Integer.parseInt(sp);
    }
    public void addSkillpoint(int sp){
        this.skillpoints += sp;
    }
    public long getXp() {
        return xp;
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
        this.skillpoints += Math.max(0, n);
    }
    public int getSkillPoints() {
        return skillpoints;
    }
    public void setSkillPoints(int pts) {
        this.skillpoints = Math.max(0, pts);
    }


//SkillPointShop
    public SkillPoint getSPShop(){ return sp;}
    public void setSPShop(SkillPoint spShop) {
        if (sp.extraSlots() != spShop.extraSlots()) {
            String currentName = BackpackManager.getBackpackNameR(backpack.getName());

            int totalExtra = this.rank.extraSlots() + spShop.extraSlots();

            setBackpack(BackpackManager.getBackpackName(Objects.requireNonNullElse(currentName, "back1"), totalExtra));
        }
        sp = spShop;
    }


    public String getSPShopString(){
        return sp.doubleItemChance() + "_" + sp.extraDamage() + "_" + sp.extraMoney() + "_" + sp.extraSlots() + "_" + sp.skillpointChance() + "_" + sp.moneypouchChance() + "_" + sp.instastealChance();
    }
    public void setSPShopString(String sp){
        StringBuilder total = new StringBuilder();
        Scanner scanner = new Scanner(sp);
        scanner.useDelimiter("_");
        double itemChance = 0.0;
        double extraDamage = 0.0;
        double extraMoney = 0.0;
        double spChance = 0.0;
        int extraSlots = 0;
        double moneyChance = 0.0;
        double instachance = 0.0;
        while(scanner.hasNext()){
            itemChance = Double.parseDouble(scanner.next());
            extraDamage = Double.parseDouble(scanner.next());
            extraMoney = Double.parseDouble(scanner.next());
            extraSlots = Integer.parseInt(scanner.next());
            spChance = Double.parseDouble(scanner.next());
            moneyChance = Double.parseDouble(scanner.next());
            instachance = Double.parseDouble(scanner.next());
        }
        this.sp = new SkillPoint(itemChance,extraDamage,extraMoney,extraSlots,spChance,moneyChance,instachance);
    }

    public void setSkillpointChance(int perk2Value) {
        this.outspchance = (double) perk2Value /100;
    }

    public void setBoosterChance(int perk2Value) {
        this.outboosterchance = (double) perk2Value /100;
    }

    public void setSpeedBonus(int perk2Value) {
        this.outspeed = (double) perk2Value /100;
    }

    public double getOutBoosterChance() {
        return outboosterchance;
    }

    public double getOutSpChance() {
        return outspchance;
    }
}

