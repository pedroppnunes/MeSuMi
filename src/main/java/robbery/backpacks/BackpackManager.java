package robbery.backpacks;

import org.bukkit.Material;
import robbery.Robbery;
import robbery.items.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Manages all predefined backpacks in the Robbery plugin.
 * <p>
 * Provides methods to retrieve backpack instances by name, ID, or serialized data,
 * and defines all available backpack tiers as static constants.
 * </p>
 */
public class BackpackManager {

    /** Predefined backpacks with different tiers and configurations. */
    public final static Backpacks BACK1 = new Backpacks("Getaway Pack",5,null, Material.LEATHER_CHESTPLATE,0,"§x§5§D§5§D§5§DG§x§5§D§5§D§5§De§x§5§D§5§D§5§Dt§x§5§D§5§D§5§Da§x§5§D§5§D§5§Dw§x§5§D§5§D§5§Da§x§5§D§5§D§5§Dy §x§5§D§5§D§5§DP§x§5§D§5§D§5§Da§x§5§D§5§D§5§Dc§x§5§D§5§D§5§Dk");
    public final static Backpacks BACK2 = new Backpacks("Loot Hauler Pack",7,null,Material.LEATHER_CHESTPLATE,100,"§x§8§1§3§F§0§0L§x§8§1§3§F§0§0o§x§8§1§3§F§0§0o§x§8§1§3§F§0§0t §x§8§1§3§F§0§0H§x§8§1§3§F§0§0a§x§8§1§3§F§0§0u§x§8§1§3§F§0§0l§x§8§1§3§F§0§0e§x§8§1§3§F§0§0r §x§8§1§3§F§0§0P§x§8§1§3§F§0§0a§x§8§1§3§F§0§0c§x§8§1§3§F§0§0k");
    public final static Backpacks BACK3 = new Backpacks("Quick Grab Pack",9,null,Material.LEATHER_CHESTPLATE,500,"§x§F§C§F§F§0§0Q§x§F§C§F§F§0§0u§x§F§C§F§F§0§0i§x§F§C§F§F§0§0c§x§F§C§F§F§0§0k §x§F§C§F§F§0§0G§x§F§C§F§F§0§0r§x§F§C§F§F§0§0a§x§F§C§F§F§0§0b §x§F§C§F§F§0§0P§x§F§C§F§F§0§0a§x§F§C§F§F§0§0c§x§F§C§F§F§0§0k");
    public final static Backpacks BACK4 = new Backpacks("Shadow Heist Pack",11,null,Material.LEATHER_CHESTPLATE,2000,"§x§D§6§0§0§F§8S§x§D§9§0§0§F§7h§x§D§B§0§0§F§5a§x§D§E§0§0§F§4d§x§E§0§0§0§F§3o§x§E§3§0§0§F§1w §x§E§8§0§0§E§FH§x§E§B§0§0§E§Ee§x§E§D§0§0§E§Ci§x§F§0§0§0§E§Bs§x§F§2§0§0§E§At §x§F§7§0§0§E§7P§x§F§A§0§0§E§6a§x§F§C§0§0§E§4c§x§F§F§0§0§E§3k");
    public final static Backpacks BACK5 = new Backpacks("Silent Snatch Pack",13,null,Material.CHAINMAIL_CHESTPLATE,3500,"§x§7§F§7§F§7§FS§x§8§7§8§7§8§7i§x§8§E§8§E§8§El§x§9§6§9§6§9§6e§x§9§D§9§D§9§Dn§x§A§5§A§5§A§5t §x§B§4§B§4§B§4S§x§B§B§B§B§B§Bn§x§C§3§C§3§C§3a§x§C§A§C§A§C§At§x§D§2§D§2§D§2c§x§D§9§D§9§D§9h §x§E§8§E§8§E§8P§x§F§0§F§0§F§0a§x§F§7§F§7§F§7c§x§F§F§F§F§F§Fk");
    public final static Backpacks BACK6 = new Backpacks("Blackout Pack",16,null,Material.CHAINMAIL_CHESTPLATE,10000,"§x§A§9§0§0§F§FB§x§9§C§0§0§F§0l§x§8§F§0§0§E§2a§x§8§2§0§0§D§3c§x§7§5§0§0§C§5k§x§6§8§0§0§B§6o§x§5§B§0§0§A§8u§x§4§E§0§0§9§9t §x§3§4§0§0§7§CP§x§3§4§0§0§7§Ca§x§3§4§0§0§7§Cc§x§3§4§0§0§7§Ck");
    public final static Backpacks BACK7 = new Backpacks("Vault Cracker Pack",19,null,Material.CHAINMAIL_CHESTPLATE,20000,"§x§F§1§F§F§0§0§lV§x§F§2§F§C§0§0§la§x§F§3§F§8§0§0§lu§x§F§4§F§5§0§0§ll§x§F§5§F§1§0§0§lt §x§F§7§E§A§0§0§lC§x§F§8§E§7§0§0§lr§x§F§9§E§4§0§0§la§x§F§A§E§0§0§0§lc§x§F§B§D§D§0§0§lk§x§F§C§D§9§0§0§le§x§F§D§D§6§0§0§lr §x§F§F§C§F§0§0§lP§x§F§F§C§F§0§0§la§x§F§F§C§F§0§0§lc§x§F§F§C§F§0§0§lk");
    public final static Backpacks BACK8 = new Backpacks("Smuggler Stash Pack",22,null,Material.CHAINMAIL_CHESTPLATE,50000,"§x§F§F§0§0§0§0§lS§x§F§F§0§F§0§0§lm§x§F§F§1§E§0§0§lu§x§F§F§2§D§0§0§lg§x§F§F§3§C§0§0§lg§x§F§F§4§B§0§0§ll§x§F§F§5§A§0§0§le§x§F§F§6§9§0§0§lr §x§F§F§8§7§0§0§lS§x§F§F§9§6§0§0§lt§x§F§F§A§5§0§0§la§x§F§F§A§5§0§0§ls§x§F§F§A§5§0§0§lh §x§F§F§A§5§0§0§lP§x§F§F§A§5§0§0§la§x§F§F§A§5§0§0§lc§x§F§F§A§5§0§0§lk");
    public final static Backpacks BACK9 = new Backpacks("Break-In Pack",25,null,Material.GOLDEN_CHESTPLATE,75000,"§x§6§3§4§B§F§F§lB§x§5§8§5§F§F§8§lr§x§4§D§7§3§F§1§le§x§4§2§8§7§E§A§la§x§3§7§9§B§E§3§lk§x§2§C§A§F§D§D§l-§x§2§1§C§3§D§6§lI§x§1§6§D§7§C§F§ln §x§0§0§F§F§C§1§lP§x§0§0§F§F§C§1§la§x§0§0§F§F§C§1§lc§x§0§0§F§F§C§1§lk");
    public final static Backpacks BACK10 = new Backpacks("Hot Goods Pack",28,null,Material.GOLDEN_CHESTPLATE,100000,"§x§E§7§F§F§0§0§lH§x§D§5§F§F§0§4§lo§x§C§3§F§F§0§7§lt §x§A§0§F§F§0§F§lG§x§8§E§F§F§1§2§lo§x§7§C§F§F§1§6§lo§x§6§B§F§F§1§A§ld§x§5§9§F§F§1§E§ls §x§3§5§F§F§2§5§lP§x§2§4§F§F§2§9§la§x§1§2§F§F§2§C§lc§x§0§0§F§F§3§0§lk");
    public final static Backpacks BACK11 = new Backpacks("Phantom Grab Pack",31,null,Material.GOLDEN_CHESTPLATE,500_000,"§x§F§1§0§0§F§F§lP§x§E§2§0§3§F§F§lh§x§D§3§0§6§F§F§la§x§C§4§0§9§F§F§ln§x§B§5§0§C§F§F§lt§x§A§6§0§F§F§F§lo§x§9§7§1§2§F§F§lm §x§7§9§1§8§F§F§lG§x§6§9§1§B§F§F§lr§x§5§A§1§E§F§F§la§x§4§B§2§1§F§F§lb §x§2§D§2§7§F§F§lP§x§1§E§2§A§F§F§la§x§0§F§2§D§F§F§lc§x§0§0§3§0§F§F§lk");
    public final static Backpacks BACK12 = new Backpacks("Night Crawler Pack",35,null,Material.GOLDEN_CHESTPLATE,750_000,"§x§0§0§0§3§5§F§lN§x§0§0§0§4§6§8§li§x§0§0§0§5§7§2§lg§x§0§0§0§6§7§B§lh§x§0§0§0§7§8§5§lt §x§0§0§0§8§9§7§lC§x§0§0§0§9§A§1§lr§x§0§0§0§A§A§A§la§x§0§0§0§B§B§4§lw§x§0§0§0§C§B§D§ll§x§0§0§0§D§C§7§le§x§0§0§0§E§D§0§lr §x§0§0§0§F§E§3§lP§x§0§0§1§0§E§C§la§x§0§0§1§1§F§6§lc§x§0§0§1§2§F§F§lk");
    public final static Backpacks BACK13 = new Backpacks("Spider Stash Pack",39,null,Material.IRON_CHESTPLATE,1_500_000,"§x§F§F§0§0§0§0§lS§x§F§F§0§0§0§F§lp§x§F§F§0§0§1§E§li§x§F§F§0§0§2§D§ld§x§F§F§0§0§3§C§le§x§F§F§0§0§4§A§lr §x§F§F§0§0§6§8§lS§x§F§F§0§0§7§7§lt§x§F§F§0§0§8§6§la§x§F§F§0§0§9§5§ls§x§F§F§0§0§A§4§lh §x§F§F§0§0§C§1§lP§x§F§F§0§0§D§0§la§x§F§F§0§0§D§F§lc§x§F§F§0§0§E§E§lk");
    public final static Backpacks BACK14 = new Backpacks("Ghost Protocol Pack",43,null,Material.IRON_CHESTPLATE,3_000_000,"§x§F§F§F§F§F§F§lG§x§F§6§F§F§F§1§lh§x§E§D§F§F§E§3§lo§x§E§3§F§F§D§5§ls§x§D§A§F§F§C§6§lt §x§C§7§F§F§A§A§lP§x§B§E§F§F§9§C§lr§x§B§4§F§F§8§E§lo§x§A§B§F§F§8§0§lt§x§A§1§F§F§7§1§lo§x§9§8§F§F§6§3§lc§x§8§E§F§F§5§5§lo§x§8§5§F§F§4§7§ll §x§7§2§F§F§2§B§lP§x§6§9§F§F§1§C§la§x§5§F§F§F§0§E§lc§x§5§6§F§F§0§0§lk");
    public final static Backpacks BACK15 = new Backpacks("The Mandapack",47,null,Material.IRON_CHESTPLATE,7_500_000,"§x§F§F§A§5§0§0§lT§x§F§F§9§7§0§0§lh§x§F§F§8§A§0§0§le §x§F§F§6§E§0§0§lM§x§F§F§6§0§0§0§la§x§F§F§5§3§0§0§ln§x§F§F§4§5§0§0§ld§x§F§F§3§7§0§0§la§x§F§F§2§9§0§0§lp§x§F§F§1§C§0§0§la§x§F§F§0§E§0§0§lc§x§F§F§0§0§0§0§lk");
    public final static Backpacks BACK16 = new Backpacks("Green FN Pack",52,null,Material.IRON_CHESTPLATE,15_000_000,"§x§0§0§F§F§1§C§lG§x§0§0§E§3§1§8§lr§x§0§0§C§7§1§3§le§x§0§0§C§7§1§3§le§x§0§0§C§7§1§3§ln §x§0§0§C§7§1§3§lF§x§0§0§C§7§1§3§lN §x§0§0§C§7§1§3§lP§x§0§0§C§7§1§3§la§x§0§0§C§7§1§3§lc§x§0§0§C§7§1§3§lk");
    public final static Backpacks BACK17 = new Backpacks("Hunter’s Cloak Pack",57,null,Material.DIAMOND_CHESTPLATE,30_000_000,"§x§E§7§F§F§0§0§lH§x§E§7§E§E§0§2§lu§x§E§7§D§E§0§4§ln§x§E§7§C§D§0§6§lt§x§E§7§B§C§0§7§le§x§E§7§A§B§0§9§lr§x§E§7§9§B§0§B§l’§x§E§7§8§A§0§D§ls §x§E§6§6§9§1§1§lC§x§E§6§5§8§1§3§ll§x§E§6§4§7§1§4§lo§x§E§6§3§6§1§6§la§x§E§6§2§6§1§8§lk §x§0§0§D§6§F§F§lP§x§0§0§D§6§F§F§la§x§0§0§D§6§F§F§lc§x§0§0§D§6§F§F§lk");
    public final static Backpacks BACK18 = new Backpacks("Wraith Pack",62,null,Material.DIAMOND_CHESTPLATE,75_000_000,"§x§E§7§0§0§F§F§lW§x§D§F§0§0§F§3§lr§x§D§7§0§0§E§7§la§x§C§F§0§0§D§B§li§x§C§7§0§0§C§F§lt§x§B§F§0§0§C§2§lh §x§A§F§0§0§A§A§lP§x§A§7§0§0§9§E§la§x§D§3§0§0§7§E§lc§x§F§F§0§0§5§D§lk");
    public final static Backpacks BACK19 = new Backpacks("Kratos Pack",67,null,Material.DIAMOND_CHESTPLATE,150_000_000,"§x§F§F§0§0§0§0§lK§x§F§F§1§A§1§A§lr§x§F§F§3§3§3§3§la§x§F§F§4§D§4§D§lt§x§F§F§6§6§6§6§lo§x§F§F§8§0§8§0§ls §x§F§F§B§3§B§3§lP§x§F§F§C§C§C§C§la§x§F§F§E§6§E§6§lc§x§F§F§F§F§F§F§lk");
    public final static Backpacks BACK20 = new Backpacks("Matrix Upload Pack",72,null,Material.DIAMOND_CHESTPLATE,325_000_000,"§x§0§0§F§F§1§C§lM§x§0§0§E§3§1§9§la§x§0§0§C§6§1§6§lt§x§0§0§A§A§1§3§lr§x§0§0§8§E§1§0§li§x§0§0§7§1§0§C§lx §x§0§0§3§9§0§6§lU§x§0§0§1§C§0§3§lp§x§0§0§0§0§0§0§ll§x§0§0§1§2§0§0§lo§x§0§0§3§4§0§0§la§x§0§1§5§6§0§0§ld §x§0§2§9§9§0§0§lP§x§0§2§B§B§0§0§la§x§0§3§D§D§0§0§lc§x§0§3§F§F§0§0§lk");
    /**
     * Converts serialized backpack data into a {@link Backpacks} object.
     *
     * @param b the backpack name and slots in the format "name_slots_extra"
     * @param material the material name for the backpack chestplate
     * @param items serialized items as underscore-separated names
     * @param color the color code string
     * @return a new {@link Backpacks} instance, or {@link #BACK1} if invalid
     */
    public static Backpacks toBackpack(String b, String material, String items, String color){
        if(b == null || color == null){
            return BACK1;
        }
        Scanner scanner = new Scanner(b);
        scanner.useDelimiter("_");
        List<Items> it = new ArrayList<>();
        if(items != null) {
            Scanner scanner1 = new Scanner(items);
            scanner1.useDelimiter("_");
            while (scanner1.hasNext()) {
                Items item = Robbery.getItemsbyName(scanner1.next());
                if(item != null)
                    it.add(item);
            }
        }
        return new Backpacks(scanner.next(),Integer.parseInt(scanner.next()),it,Material.getMaterial(material),Integer.parseInt(scanner.next()),color);
    }

    /**
     * Retrieves a predefined backpack by its internal ID and adds extra slots.
     *
     * @param n the backpack ID string (e.g., "back1")
     * @param extraslots number of extra slots to add
     * @return a new {@link Backpacks} instance with the specified extra slots, or null if invalid ID
     */
    public static Backpacks getBackpackName(String n, int extraslots){
        return switch (n) {
            case "back1" -> new Backpacks(BACK1,extraslots);
            case "back2" -> new Backpacks(BACK2,extraslots);
            case "back3" -> new Backpacks(BACK3,extraslots);
            case "back4" -> new Backpacks(BACK4,extraslots);
            case "back5" -> new Backpacks(BACK5,extraslots);
            case "back6" -> new Backpacks(BACK6,extraslots);
            case "back7" -> new Backpacks(BACK7,extraslots);
            case "back8" -> new Backpacks(BACK8,extraslots);
            case "back9" -> new Backpacks(BACK9,extraslots);
            case "back10" -> new Backpacks(BACK10,extraslots);
            case "back11" -> new Backpacks(BACK11,extraslots);
            case "back12" -> new Backpacks(BACK12,extraslots);
            case "back13" -> new Backpacks(BACK13,extraslots);
            case "back14" -> new Backpacks(BACK14,extraslots);
            case "back15" -> new Backpacks(BACK15,extraslots);
            case "back16" -> new Backpacks(BACK16,extraslots);
            case "back17" -> new Backpacks(BACK17,extraslots);
            case "back18" -> new Backpacks(BACK18,extraslots);
            case "back19" -> new Backpacks(BACK19,extraslots);
            case "back20" -> new Backpacks(BACK20,extraslots);
            default -> null;
        };
    }

    /**
     * Retrieves the internal ID of a backpack by its display name.
     *
     * @param n the display name of the backpack
     * @return the internal ID (e.g., "back1") or null if not found
     */
    public static String getBackpackNameR(String n){
        return switch (n) {
            case "Getaway Pack" -> "back1";
            case "Loot Hauler Pack" -> "back2";
            case "Quick Grab Pack" -> "back3";
            case "Shadow Heist Pack" -> "back4";
            case "Silent Snatch Pack" -> "back5";
            case "Blackout Pack" -> "back6";
            case "Vault Cracker Pack" -> "back7";
            case "Smuggler Stash Pack" -> "back8";
            case "Break-In Pack" -> "back9";
            case "Hot Goods Pack" -> "back10";
            case "Phantom Grab Pack" -> "back11";
            case "Night Crawler Pack" -> "back12";
            case "Spider Stash Pack" -> "back13";
            case "Ghost Protocol Pack" -> "back14";
            case "The Mandapack" -> "back15";
            case "Green FN Pack" -> "back16";
            case "Hunter’s Cloak Pack" -> "back17";
            case "Wraith Pack" -> "back18";
            case "Kratos Pack" -> "back19";
            case "Matrix Upload Pack" -> "back20";
            default -> null;
        };
    }

    /**
     * Retrieves the display name of a backpack by its internal ID.
     *
     * @param id the internal ID of the backpack (e.g., "back1")
     * @return the display name, or null if not found
     */
    public static String getBackPackN(String id){
        return switch (id) {
            case "back1" -> "Getaway Pack";
            case "back2" -> "Loot Hauler Pack";
            case "back3" -> "Quick Grab Pack";
            case "back4" -> "Shadow Heist Pack";
            case "back5" -> "Silent Snatch Pack";
            case "back6" -> "Blackout Pack";
            case "back7" -> "Vault Cracker Pack";
            case "back8" -> "Smuggler Stash Pack";
            case "back9" -> "Break-In Pack";
            case "back10" -> "Hot Goods Pack";
            case "back11" -> "Phantom Grab Pack";
            case "back12" -> "Night Crawler Pack";
            case "back13" -> "Spider Stash Pack";
            case "back14" -> "Ghost Protocol Pack";
            case "back15" -> "The Mandapack";
            case "back16" -> "Green FN Pack";
            case "back17" -> "Hunter’s Cloak Pack";
            case "back18" -> "Wraith Pack";
            case "back19" -> "Kratos Pack";
            case "back20" -> "Matrix Upload Pack";
            default -> null;
        };
    }
}
