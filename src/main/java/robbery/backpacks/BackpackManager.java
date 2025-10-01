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
    public final static Backpacks BACK1 = new Backpacks("Getaway Pack",5,null, Material.LEATHER_CHESTPLATE,0,"§x§5§D§5§D§5§DG§x§5§D§5§D§5§De...");
    public final static Backpacks BACK2 = new Backpacks("Loot Hauler Pack",7,null,Material.LEATHER_CHESTPLATE,100,"§x§8§1§3§F§0§0L...");
    public final static Backpacks BACK3 = new Backpacks("Quick Grab Pack",9,null,Material.LEATHER_CHESTPLATE,500,"§x§F§C§F§F§0§0Q...");
    public final static Backpacks BACK4 = new Backpacks("Shadow Heist Pack",11,null,Material.LEATHER_CHESTPLATE,2000,"§x§D§6§0§0§F§8S...");
    public final static Backpacks BACK5 = new Backpacks("Silent Snatch Pack",13,null,Material.CHAINMAIL_CHESTPLATE,3500,"§x§7§F§7§F§7§FS...");
    public final static Backpacks BACK6 = new Backpacks("Blackout Pack",16,null,Material.CHAINMAIL_CHESTPLATE,10000,"§x§A§9§0§0§F§FB...");
    public final static Backpacks BACK7 = new Backpacks("Vault Cracker Pack",19,null,Material.CHAINMAIL_CHESTPLATE,20000,"§x§F§1§F§F§0§0§lV...");
    public final static Backpacks BACK8 = new Backpacks("Smuggler Stash Pack",22,null,Material.CHAINMAIL_CHESTPLATE,50000,"§x§F§F§0§0§0§0§lS...");
    public final static Backpacks BACK9 = new Backpacks("Break-In Pack",25,null,Material.GOLDEN_CHESTPLATE,75000,"§x§6§3§4§B§F§F§lB...");
    public final static Backpacks BACK10 = new Backpacks("Hot Goods Pack",28,null,Material.GOLDEN_CHESTPLATE,100000,"§x§E§7§F§F§0§0§lH...");
    public final static Backpacks BACK11 = new Backpacks("Phantom Grab Pack",31,null,Material.GOLDEN_CHESTPLATE,500_000,"§x§F§1§0§0§F§F§lP...");
    public final static Backpacks BACK12 = new Backpacks("Night Crawler Pack",35,null,Material.GOLDEN_CHESTPLATE,750_000,"§x§0§0§0§3§5§F§lN...");
    public final static Backpacks BACK13 = new Backpacks("Spider Stash Pack",39,null,Material.IRON_CHESTPLATE,1_500_000,"§x§F§F§0§0§0§0§lS...");
    public final static Backpacks BACK14 = new Backpacks("Ghost Protocol Pack",43,null,Material.IRON_CHESTPLATE,3_000_000,"§x§F§F§F§F§F§F§lG...");
    public final static Backpacks BACK15 = new Backpacks("The Mandapack",47,null,Material.IRON_CHESTPLATE,7_500_000,"§x§F§F§A§5§0§0§lT...");
    public final static Backpacks BACK16 = new Backpacks("Green FN Pack",52,null,Material.IRON_CHESTPLATE,15_000_000,"§x§0§0§F§F§1§C§lG...");
    public final static Backpacks BACK17 = new Backpacks("Hunter’s Cloak Pack",57,null,Material.DIAMOND_CHESTPLATE,30_000_000,"§x§E§7§F§F§0§0§lH...");
    public final static Backpacks BACK18 = new Backpacks("Wraith Pack",62,null,Material.DIAMOND_CHESTPLATE,75_000_000,"§x§E§7§0§0§F§F§lW...");
    public final static Backpacks BACK19 = new Backpacks("Kratos Pack",67,null,Material.DIAMOND_CHESTPLATE,150_000_000,"§x§F§F§0§0§0§0§lK...");
    public final static Backpacks BACK20 = new Backpacks("Matrix Upload Pack",72,null,Material.DIAMOND_CHESTPLATE,325_000_000,"§x§0§0§F§F§1§C§lM...");

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
