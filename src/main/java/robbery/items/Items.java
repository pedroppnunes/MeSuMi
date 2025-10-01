package robbery.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import robbery.Robbery;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Items {
    private final double initialhp;
    private final String playername;

    private double hp;
    private final int value;
    private final String name;
    private final ItemStack skull;
    private final UUID uniqueId;
    private boolean isPickable = true;
    private Location position;
    private Item droppedItem;
    private final int time;

    public Items(double hp, int value, String name, String playername, int time){
        this.hp = hp;
        this.value = value;
        this.name = name;
        this.playername = playername;
        this.uniqueId = UUID.randomUUID();
        this.skull = getPlayerHead(playername);
        this.initialhp = hp;
        this.time = time;
    }

    public Items(Items other) {
        this.initialhp = other.hp;
        this.hp = other.hp;
        this.value = other.value;
        this.name = other.name;
        this.playername = other.playername;
        this.skull = other.skull;
        this.time = other.time;
        this.uniqueId = UUID.randomUUID();
    }
    public Items(Map<String, Object> itemData) {
        this.hp = (double) itemData.get("hp");
        this.value = (int) itemData.get("value");
        this.name = (String) itemData.get("name");
        this.playername = (String) itemData.get("playerName");
        this.uniqueId = UUID.fromString((String) itemData.get("uniqueId"));
        this.skull = getPlayerHead(this.playername);
        this.initialhp = this.hp;
        this.time = (int) itemData.get("time");
        this.droppedItem = (Item) Bukkit.getEntity(UUID.fromString((String) itemData.get("droppedItem")));
        this.position = droppedItem.getLocation();
    }

    public double getHp(){
        return Math.max(0,hp);
    }
    public int getValue(){
        return value;
    }
    public String getName(){
        return name;
    }
    public ItemStack getSkull(){
        return skull;
    }
    public UUID getUniqueId() {
        return uniqueId;
    }

    public static ItemStack getPlayerHead(String value) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1, (short)3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", value));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);

        return head;
    }



    public void setHp(double newHp) {
        this.hp = newHp;
    }


    public void setPosition(Location pos){
        this.position = pos;
    }
    public Location getPosition() {
        return position;
    }
    public boolean isPickable(){
        return isPickable;
    }
    public void setPickable(){
        this.isPickable = !isPickable;
    }
    public double getInitialhp(){
        return initialhp;
    }
    public void setDroppedItem(Item item){
        this.droppedItem = item;
        this.position = item.getLocation();
    }
    public void remove(){
        if(droppedItem != null)
            this.droppedItem.remove();
    }


    public void resetspawn(int delayInSeconds) {
        if (droppedItem != null) {
            Location itemLocation = droppedItem.getLocation();
            droppedItem.remove();

            new BukkitRunnable() {
                @Override
                public void run() {
                    // Respawn the item at the original location
                    Item newItem = itemLocation.getWorld().dropItem(itemLocation, skull);
                    newItem.setPickupDelay(Integer.MAX_VALUE); // Item can't be picked up
                    newItem.setUnlimitedLifetime(true); // Item will not despawn
                    newItem.setVelocity(new Vector(0, 0, 0)); // Prevent movement
                    newItem.setCustomName(uniqueId.toString()); // Set custom name
                   droppedItem = newItem; // Update the reference

                    hp = initialhp;
                    isPickable = !isPickable;

                }
            }.runTaskLater(Robbery.getInstance(), delayInSeconds * 20L); // Convert seconds to ticks
        }
    }

    public int getTime() {
        return time;
    }


    public Map<String, Object> serialize() {
        Map<String, Object> itemData = new ConcurrentHashMap<>();
        itemData.put("hp", this.initialhp);
        itemData.put("value", this.value);
        itemData.put("name", this.name);
        itemData.put("playerName", this.playername);
        itemData.put("time", this.time);
        itemData.put("uniqueId",this.uniqueId.toString());
        itemData.put("droppedItem",this.droppedItem.getUniqueId().toString());
        return itemData;
    }

}
