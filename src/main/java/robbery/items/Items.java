package robbery.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import robbery.Robbery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Items {
    private final double initialhp;
    private final String playername;

    private double hp;
    private final int value;
    private final String name;
    private final ItemStack skull;
    private UUID uniqueId;
    private boolean isPickable = true;
    private Location position;
    private Item droppedItem;
    private final String id;
    private final int time;

    public Items(double hp, int value, String name, String playername, int time, String id){
        this.hp = hp;
        this.value = value;
        this.name = name;
        this.playername = playername;
        this.uniqueId = UUID.randomUUID();
        this.skull = getPlayerHead(playername);
        this.initialhp = hp;
        this.time = time;
        this.id = id;
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
        this.id = other.id;
    }
    public Items(Map<String, Object> itemData) {
        this.hp = asDouble(itemData.get("hp"));
        this.value = asInt(itemData.get("value"));
        this.name = (String) itemData.getOrDefault("name", "unknown");
        this.playername = (String) itemData.getOrDefault("playerName", "");
        this.initialhp = this.hp;
        this.time = asInt(itemData.get("time"));
        this.id = (String) itemData.getOrDefault("id", "unknown");

        String uidStr = (String) itemData.get("uniqueId");
        if (uidStr != null) {
            try {
                this.uniqueId = UUID.fromString(uidStr);
            } catch (IllegalArgumentException e) {
                this.uniqueId = UUID.randomUUID();
            }
        } else {
            this.uniqueId = UUID.randomUUID();
        }

        this.skull = getPlayerHead(this.playername);

        this.droppedItem = null;
        Object droppedObj = itemData.get("droppedItem");
        if (droppedObj instanceof String droppedUuidStr) {
            try {
                UUID droppedUuid = UUID.fromString(droppedUuidStr);
                if (Bukkit.getEntity(droppedUuid) instanceof Item ent) {
                    this.droppedItem = ent;
                    this.position = ent.getLocation();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (this.droppedItem == null) {
            Object worldObj = itemData.get("world");
            Object xObj = itemData.get("x");
            Object yObj = itemData.get("y");
            Object zObj = itemData.get("z");

            if (worldObj instanceof String && (xObj != null && zObj != null && yObj != null)) {
                World w = Bukkit.getWorld((String) worldObj);
                if (w != null) {
                    double x = asDouble(xObj);
                    double y = asDouble(yObj);
                    double z = asDouble(zObj);
                    this.position = new Location(w, x, y, z);
                }
            }
        }
    }

    private static double asDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof String) {
            try { return Double.parseDouble((String) o); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private static int asInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt((String) o); } catch (NumberFormatException ignored) {}
        }
        return 0;
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
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("hp", this.initialhp);
        itemData.put("value", this.value);
        itemData.put("name", this.name);
        itemData.put("playerName", this.playername);
        itemData.put("time", this.time);
        itemData.put("uniqueId", this.uniqueId.toString());
        itemData.put("id", this.id);

        if (this.droppedItem != null) {
            itemData.put("droppedItem", this.droppedItem.getUniqueId().toString());
            Location loc = this.droppedItem.getLocation();
            itemData.put("world", loc.getWorld().getName());
            itemData.put("x", loc.getX());
            itemData.put("y", loc.getY());
            itemData.put("z", loc.getZ());
        } else if (this.position != null) {
            itemData.put("world", this.position.getWorld().getName());
            itemData.put("x", this.position.getX());
            itemData.put("y", this.position.getY());
            itemData.put("z", this.position.getZ());
            itemData.put("droppedItem", null);
        } else {
            itemData.put("droppedItem", null);
        }

        return itemData;
    }

    public Items copyForBackpack(double boost) {
        Items copy = new Items(this.hp, (int) (this.value * boost), this.name, this.playername, this.time,this.id);
        copy.setUniqueId(UUID.randomUUID());
        return copy;
    }

    public Item getDroppedItem(){
        return droppedItem;
    }

    public void setUniqueId(UUID id){
        this.uniqueId = id;
    }

    public String getId(){
        return id;
    }


}
