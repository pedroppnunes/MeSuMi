package robbery.tool;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ToolManager {
    public static final Tools TOOL1 = new Tools("Wooden Stick", 0.1, Material.MUSIC_DISC_OTHERSIDE,0,"§x§7§0§3§B§0§0W§x§7§0§3§B§0§0o§x§7§0§3§B§0§0o§x§7§0§3§B§0§0d§x§7§0§3§B§0§0e§x§7§0§3§B§0§0n §x§7§0§3§B§0§0S§x§7§0§3§B§0§0t§x§7§0§3§B§0§0i§x§7§0§3§B§0§0c§x§7§0§3§B§0§0k");
    public static final Tools TOOL2 = new Tools("Shoe String", 0.2, Material.MUSIC_DISC_11,200,"§x§9§1§9§1§9§1S§x§9§1§9§1§9§1h§x§9§1§9§1§9§1o§x§9§1§9§1§9§1e §x§9§1§9§1§9§1S§x§9§1§9§1§9§1t§x§9§1§9§1§9§1r§x§9§1§9§1§9§1i§x§9§1§9§1§9§1n§x§9§1§9§1§9§1g");
    public static final Tools TOOL3 = new Tools("Wire Cutter", 0.3, Material.MUSIC_DISC_13,1_200,"§x§F§F§0§0§0§0W§x§F§F§1§A§1§Ai§x§F§F§3§3§3§3r§x§F§F§4§D§4§De §x§F§F§8§0§8§0C§x§F§F§9§9§9§9u§x§F§F§B§3§B§3t§x§F§F§C§C§C§Ct§x§F§F§E§6§E§6e§x§F§F§F§F§F§Fr");
    public static final Tools TOOL4 = new Tools("Razor Blade", 0.5, Material.MUSIC_DISC_BLOCKS,2_000,"§x§4§1§F§F§0§0R§x§4§F§F§F§1§2a§x§5§C§F§F§2§4z§x§6§A§F§F§3§7o§x§7§7§F§F§4§9r §x§9§2§F§F§6§DB§x§A§0§F§F§7§Fl§x§A§D§F§F§9§2a§x§B§B§F§F§A§4d§x§C§8§F§F§B§6e");
    public static final Tools TOOL5 = new Tools("Utility Knife", 0.7, Material.MUSIC_DISC_STAL,15_000,"§x§F§F§F§8§0§0U§x§F§F§F§9§0§Ft§x§F§E§F§9§1§Ei§x§F§E§F§A§2§El§x§F§E§F§A§3§Di§x§F§D§F§B§4§Ct§x§F§D§F§C§5§By §x§F§C§F§D§7§9K§x§F§C§F§D§8§9n§x§F§C§F§E§9§8i§x§F§B§F§E§A§7f§x§F§B§F§F§B§6e");
    public static final Tools TOOL6 = new Tools("Crowbar", 0.9, Material.MUSIC_DISC_CAT,25_000,"§x§4§A§4§A§4§AC§x§5§3§5§3§5§3r§x§5§C§5§C§5§Co§x§6§5§6§5§6§5w§x§6§E§6§E§6§Eb§x§7§7§7§7§7§7a§x§8§0§8§0§8§0r");
    public static final Tools TOOL7 = new Tools("Leather Gloves", 1.1, Material.MUSIC_DISC_CHIRP,100_000,"§x§5§F§3§A§0§0§lL§x§5§F§3§A§0§0§le§x§5§F§3§9§0§0§la§x§5§F§3§9§0§0§lt§x§5§E§3§8§0§0§lh§x§5§E§3§8§0§0§le§x§5§E§3§8§0§0§lr §x§5§E§3§7§0§0§lG§x§5§E§3§6§0§0§ll§x§5§D§3§6§0§0§lo§x§5§D§3§5§0§0§lv§x§5§D§3§5§0§0§le§x§5§D§3§5§0§0§ls");
    public static final Tools TOOL8 = new Tools("Iron Gloves", 1.4, Material.MUSIC_DISC_STRAD,175_000,"§x§F§F§F§F§F§F§lI§x§E§7§E§7§E§7§lr§x§C§E§C§E§C§E§lo§x§B§6§B§6§B§6§ln §x§8§5§8§5§8§5§lG§x§6§D§6§D§6§D§ll§x§6§D§6§D§6§D§lo§x§6§D§6§D§6§D§lv§x§6§D§6§D§6§D§le§x§6§D§6§D§6§D§ls");
    public static final Tools TOOL9 = new Tools("Golden Gloves", 1.7, Material.MUSIC_DISC_FAR,600_000,"§x§F§1§F§F§0§0§lG§x§F§3§F§A§0§0§lo§x§F§5§F§4§0§0§ll§x§F§7§E§F§0§0§ld§x§F§9§E§9§0§0§le§x§F§B§E§4§0§0§ln §x§F§F§D§9§0§0§lG§x§F§F§D§9§0§0§ll§x§F§F§D§9§0§0§lo§x§F§F§D§9§0§0§lv§x§F§F§D§9§0§0§le§x§F§F§D§9§0§0§ls");
    public static final Tools TOOL10 = new Tools("Diamond Gloves", 2.0, Material.MUSIC_DISC_MALL,1_000_000,"§x§0§0§F§F§E§A§lD§x§0§0§F§A§E§D§li§x§0§0§F§5§E§F§la§x§0§0§F§0§F§2§lm§x§0§0§E§B§F§5§lo§x§0§0§E§5§F§7§ln§x§0§0§E§0§F§A§ld §x§0§0§D§6§F§F§lG§x§0§0§D§6§F§F§ll§x§0§0§D§6§F§F§lo§x§0§0§D§6§F§F§lv§x§0§0§D§6§F§F§le§x§0§0§D§6§F§F§ls");
    public static final Tools TOOL11 = new Tools("Briefcase", 2.4, Material.MUSIC_DISC_MELLOHI,4_000_000,"§x§8§6§8§6§8§6§lB§x§7§8§7§8§7§8§lr§x§6§9§6§9§6§9§li§x§5§B§5§B§5§B§le§x§4§C§4§C§4§C§lf§x§3§E§3§E§3§E§lc§x§3§E§3§E§3§E§la§x§3§E§3§E§3§E§ls§x§3§E§3§E§3§E§le");
    public static final Tools TOOL12 = new Tools("Magnet", 2.8, Material.MUSIC_DISC_WARD,7_000_000,"§x§F§F§0§0§0§0§lM§x§F§F§3§3§3§3§la§x§F§F§6§6§6§6§lg§x§F§F§9§9§9§9§ln§x§F§F§C§C§C§C§le§x§F§F§F§F§F§F§lt");
    public static final Tools TOOL13 = new Tools("Yo-Yo", 3.2, Material.MUSIC_DISC_WAIT,27_500_000,"§x§F§1§0§0§F§F§lY§x§4§1§F§F§0§0§lo§x§F§F§D§D§0§0§l-§x§F§5§2§0§9§0§lY§x§0§0§E§A§F§F§lo");
    public static final Tools TOOL14 = new Tools("Nunchaku", 3.6, Material.IRON_HORSE_ARMOR,100_000_000,"§x§A§9§0§0§F§F§lN§x§9§6§0§5§E§0§lu§x§8§3§0§B§C§0§ln§x§7§0§1§0§A§1§lc§x§5§D§1§6§8§1§lh§x§4§A§1§B§6§2§la§x§4§A§1§B§6§2§lk§x§4§A§1§B§6§2§lu");
    public static final Tools TOOL15 = new Tools("Grappling Hook", 4.1, Material.GOLDEN_HORSE_ARMOR,175_000_000L,"§x§3§C§3§0§0§0§lG§x§4§B§3§B§0§0§lr§x§5§A§4§7§0§0§la§x§6§9§5§2§0§0§lp§x§7§8§5§E§0§0§lp§x§8§7§6§9§0§0§ll§x§9§6§7§4§0§0§li§x§A§5§8§0§0§0§ln§x§B§4§8§B§0§0§lg §x§D§2§A§2§0§0§lH§x§E§1§A§D§0§0§lo§x§F§0§B§9§0§0§lo§x§F§F§C§4§0§0§lk");
    public static final Tools TOOL16 = new Tools("Gravity Gun", 4.6, Material.DIAMOND_HORSE_ARMOR,350_000_000L,"§x§F§F§A§5§0§0§lG§x§F§F§9§5§0§0§lr§x§F§F§8§4§0§0§la§x§F§F§7§4§0§0§lv§x§F§F§6§3§0§0§li§x§F§F§5§3§0§0§lt§x§F§F§4§2§0§0§ly §x§F§F§2§1§0§0§lG§x§F§F§1§1§0§0§lu§x§F§F§0§0§0§0§ln");
    public static final Tools TOOL17 = new Tools("Plasma Claws", 5.1, Material.HEART_OF_THE_SEA,650_000_000L,"§x§A§9§0§0§F§F§lP§x§B§7§0§0§F§2§ll§x§C§6§0§0§E§5§la§x§D§4§0§0§D§8§ls§x§E§2§0§0§C§A§lm§x§F§1§0§0§B§D§la §x§F§F§0§0§B§0§lC§x§F§F§0§0§B§0§ll§x§F§F§0§0§B§0§la§x§F§F§0§0§B§0§lw§x§F§F§0§0§B§0§ls");
    public static final Tools TOOL18 = new Tools("Lightsaber", 5.6, Material.FIREWORK_STAR,900_000_000L,"§x§0§0§F§F§0§7§lL§x§0§0§F§F§4§5§li§x§0§0§F§F§8§3§lg§x§0§0§F§F§C§1§lh§x§0§0§F§F§F§F§lt§x§3§3§C§C§C§C§ls§x§6§6§9§9§9§9§la§x§9§9§6§6§6§6§lb§x§C§C§3§3§3§3§le§x§F§F§0§0§0§0§lr");
    public static final Tools TOOL19 = new Tools("Butterfly", 6.2, Material.SADDLE,1_500_000_000L,"§x§0§0§F§F§0§7§lB§x§0§0§F§F§2§0§lu§x§0§0§F§F§3§9§lt§x§0§0§F§F§5§1§lt§x§0§0§F§F§6§A§le§x§0§0§F§F§8§3§lr§x§0§0§F§F§8§3§lf§x§0§0§F§F§8§3§ll§x§0§0§F§F§8§3§ly");
    public static final Tools TOOL20 = new Tools("Karambit", 7.5, Material.CARROT_ON_A_STICK,3_500_000_000L,"§x§F§F§0§0§0§0§lK§x§E§E§8§0§0§0§la§x§D§D§F§F§0§0§lr§x§8§0§F§F§0§0§la§x§2§2§F§F§0§0§lm§x§0§0§F§F§F§5§lb§x§8§0§8§0§E§C§li§x§F§F§0§0§E§3§lt");

    public static Tools getToolFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return null;
        }

        return switch (item.getType()) {
            case Material.MUSIC_DISC_OTHERSIDE -> TOOL1;
            case Material.MUSIC_DISC_11 -> TOOL2;
            case Material.MUSIC_DISC_13 -> TOOL3;
            case Material.MUSIC_DISC_BLOCKS -> TOOL4;
            case Material.MUSIC_DISC_STAL -> TOOL5;
            case Material.MUSIC_DISC_CAT -> TOOL6;
            case Material.MUSIC_DISC_CHIRP -> TOOL7;
            case Material.MUSIC_DISC_STRAD -> TOOL8;
            case Material.MUSIC_DISC_FAR -> TOOL9;
            case Material.MUSIC_DISC_MALL -> TOOL10;
            case Material.MUSIC_DISC_MELLOHI -> TOOL11;
            case Material.MUSIC_DISC_WARD -> TOOL12;
            case Material.MUSIC_DISC_WAIT -> TOOL13;
            case Material.IRON_HORSE_ARMOR -> TOOL14;
            case Material.GOLDEN_HORSE_ARMOR -> TOOL15;
            case Material.DIAMOND_HORSE_ARMOR -> TOOL16;
            case Material.HEART_OF_THE_SEA -> TOOL17;
            case Material.FIREWORK_STAR -> TOOL18;
            case Material.SADDLE -> TOOL19;
            case Material.CARROT_ON_A_STICK -> TOOL20;
            default -> null;
        };
    }

    public static Tools getToolsName(String n){
        return switch (n) {
            case "tool1" -> TOOL1;
            case "tool2" -> TOOL2;
            case "tool3" -> TOOL3;
            case "tool4" -> TOOL4;
            case "tool5" -> TOOL5;
            case "tool6" -> TOOL6;
            case "tool7" -> TOOL7;
            case "tool8" -> TOOL8;
            case "tool9" -> TOOL9;
            case "tool10" -> TOOL10;
            case "tool11" -> TOOL11;
            case "tool12" -> TOOL12;
            case "tool13" -> TOOL13;
            case "tool14" -> TOOL14;
            case "tool15" -> TOOL15;
            case "tool16" -> TOOL16;
            case "tool17" -> TOOL17;
            case "tool18" -> TOOL18;
            case "tool19" -> TOOL19;
            case "tool20" -> TOOL20;
            default -> null;
        };
    }
    public static String getToolsNameR(String n){
        return switch (n) {
            case "Wooden Stick" -> "tool1";
            case "Shoe String" -> "tool2";
            case "Wire Cutter" -> "tool3";
            case "Razor Blade" -> "tool4";
            case "Utility Knife" -> "tool5";
            case "Crowbar" -> "tool6";
            case "Leather Gloves" -> "tool7";
            case "Iron Gloves" -> "tool8";
            case "Golden Gloves" -> "tool9";
            case "Diamond Gloves" -> "tool10";
            case "Briefcase" -> "tool11";
            case "Magnet" -> "tool12";
            case "Yo-Yo" -> "tool13";
            case "Nunchaku" -> "tool14";
            case "Grappling Hook" -> "tool15";
            case "Gravity Gun" -> "tool16";
            case "Plasma Claws" -> "tool17";
            case "Lightsaber" -> "tool18";
            case "Butterfly" -> "tool19";
            case "Karambit" -> "tool20";
            default -> null;
        };
    }

    public static String getToolName(String id) {
        return switch (id) {
            case "tool1" -> "Wooden Stick";
            case "tool2" -> "Shoe String";
            case "tool3" -> "Wire Cutter";
            case "tool4" -> "Razor Blade";
            case "tool5" -> "Utility Knife";
            case "tool6" -> "Crowbar";
            case "tool7" -> "Leather Gloves";
            case "tool8" -> "Iron Gloves";
            case "tool9" -> "Golden Gloves";
            case "tool10" -> "Diamond Gloves";
            case "tool11" -> "Briefcase";
            case "tool12" -> "Magnet";
            case "tool13" -> "Yo-Yo";
            case "tool14" -> "Nunchaku";
            case "tool15" -> "Grappling Hook";
            case "tool16" -> "Gravity Gun";
            case "tool17" -> "Plasma Claws";
            case "tool18" -> "Lightsaber";
            case "tool19" -> "Butterfly";
            case "tool20" -> "Karambit";
            default -> null;
        };
    }

}
