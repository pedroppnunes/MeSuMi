package robbery.keys;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import robbery.player.PlayerData;

import java.text.DecimalFormat;
import java.util.List;

public class KeyManager {
    private static final World w = Bukkit.getWorld("world");

    public final static Keys STORE1 = new Keys("Supermarket",0,1,"&#FB0808S&#FB0808u&#FB0808p&#FB0808e&#FB0808r&#FB0808m&#FB0808a&#FB0808r&#FB0808k&#FB0808e&#FB0808t","store1", List.of(new DoorArea(new Location(w, 20122, 101, 20046), new Location(w, 20126, 104, 20046))));
    public final static Keys STORE2 = new Keys("The Griffin's",1000,2,"&#084CFBT&#084CFBh&#084CFBe &#084CFBG&#084CFBr&#084CFBi&#084CFBf&#084CFBf&#084CFBi&#084CFBn&#084CFB'&#084CFBs","store2",List.of(new DoorArea(new Location(w, 20129,103,19979),new Location(w, 20131,108,19979))));
    public final static Keys STORE3 = new Keys("Gym",22_500,3,"&#FFFFFFG&#FFFFFFy&#FFFFFFm","store3",List.of(new DoorArea(new Location(w, 20175,101,20044),new Location(w, 20173,107,20044))));
    public final static Keys STORE4 = new Keys("Arcade",75_000,4,"&#FCD05CA&#F4A67Dr&#EB7D9Dc&#E353BEa&#DA2ADEd&#D200FFe","store4",List.of(new DoorArea(new Location(w, 20202,101,19982),new Location(w, 20220,106,19982))));
    public final static Keys STORE5 = new Keys("School",250_000,5,"&#F1FF00S&#F1FF00c&#F1FF00h&#F1FF00o&#F1FF00o&#F1FF00l","store5",List.of(new DoorArea(new Location(w, 20252,102,20098),new Location(w, 20252,105,20097))));
    public final static Keys STORE6 = new Keys("Casino",1_000_000,6,"&#FF0000&lC&#FC3300&la&#F96600&ls&#F79900&li&#F4CC00&ln&#F1FF00&lo","store6",List.of(new DoorArea(new Location(w, 20328,101,20017),new Location(w, 20328,107,20016)),
                                                                                                                                                                                        new DoorArea(new Location(w, 20327,101,20016),new Location(w, 20327,108,20015)),
                                                                                                                                                                                        new DoorArea(new Location(w,20324,101,20013),new Location(w,20325,108,20013)),
                                                                                                                                                                                        new DoorArea(new Location(w,20325,101,20014),new Location(w,20326,108,20014)),
                                                                                                                                                                                        new DoorArea(new Location(w,20326,101,20015),new Location(w,20326,109,20015)),
                                                                                                                                                                                        new DoorArea(new Location(w, 20320,101,20009),new Location(w, 20320,107,20009)),
                                                                                                                                                                                        new DoorArea(new Location(w, 20319,101,20008),new Location(w, 20319,108,20007)),
                                                                                                                                                                                        new DoorArea(new Location(w,220316,101,20005),new Location(w,20317,107,20005)),
                                                                                                                                                                                        new DoorArea(new Location(w,20317,101,20006),new Location(w,20318,108,20006)),
                                                                                                                                                                                        new DoorArea(new Location(w,20318,101,20007),new Location(w,20318,109,20007))));
    public final static Keys STORE7 = new Keys("Oceanarium",2_500_000,7,"&#00EAFF&lO&#00ECE3&lc&#01EFC6&le&#01F1AA&la&#01F38E&ln&#02F671&la&#02F855&lr&#02FA39&li&#03FD1C&lu&#03FF00&lm","store7",List.of(new DoorArea(new Location(w, 20334,101,20130),new Location(w, 20334,103,20130)),
                                                                                                                                                                                                                                        new DoorArea(new Location(w, 20334,101,20131),new Location(w, 20334,104,20131)),
                                                                                                                                                                                                                                        new DoorArea(new Location(w, 20334,101,20132),new Location(w, 20334,105,20132)),
                                                                                                                                                                                                                                        new DoorArea(new Location(w, 20334,101,20134),new Location(w, 20334,105,20134)),
                                                                                                                                                                                                                                        new DoorArea(new Location(w, 20334,101,20135),new Location(w, 20334,104,20135)),
                                                                                                                                                                                                                                        new DoorArea(new Location(w, 20334,101,20136),new Location(w, 20334,103,20136))));
    public final static Keys STORE8 = new Keys("Steakhouse",12_500_000,8,"&#732F00&lS&#833800&lt&#924000&le&#A24900&la&#B15100&lk&#C15A00&lh&#D06200&lo&#E06B00&lu&#EF7300&ls&#FF7C00&le","store8",List.of(new DoorArea(new Location(w, 20232,101,20236),new Location(w, 20228,103,20236))));
    public final static Keys STORE9 = new Keys("Diamond Store",50_000_000,9,"&#00FFFF&lD&#13EAFF&li&#27D5FF&la&#3ABFFF&lm&#4DAAFF&lo&#6095FF&ln&#7480FF&ld &#9A55FF&lS&#AD40FF&lt&#C12BFF&lo&#D415FF&lr&#E700FF&le","store9",List.of(new DoorArea(new Location(w, 20187,101,20152),new Location(w, 20190,104,20152))));
    public final static Keys STORE10 = new Keys("Balenziaga",125_000_000,10,"&#FFFFFF&lB&#FFFFFF&la&#FFFFFF&ll&#FFFFFF&le&#808080&ln&#000000&lz&#808080&li&#FFFFFF&la&#FFFFFF&lg&#FFFFFF&la","store10",List.of(new DoorArea(new Location(w, 20186,101,20210),new Location(w, 20175,109,20210))));
    public final static Keys STORE11 = new Keys("Samzung",300_000_000,11,"&#00FF07&lS&#04EA06&la&#07D405&lm&#0BBF04&lz&#0FA902&lu&#129401&ln&#167E00&lg","store11",List.of(new DoorArea(new Location(w, 20122,101,20148),new Location(w, 20123,106,20148)),
                                                                                                                                                                                                        new DoorArea(new Location(w, 20123,101,20147),new Location(w, 20125,106,20147)),
                                                                                                                                                                                                        new DoorArea(new Location(w, 20125,101,20146),new Location(w, 20126,106,20146)),
                                                                                                                                                                                                        new DoorArea(new Location(w, 20126,101,20145),new Location(w, 20127,106,20145))));
    public final static Keys STORE12 = new Keys("The Bank",750_000_000,12,"&#FFB000&lT&#FFB300&lh&#FFB600&le &#FFBB00&lB&#FFBE00&la&#FFC100&ln&#FFC400&lk","store12",List.of(new DoorArea(new Location(w, 20096,103,20243),new Location(w, 20084,108,20243))));


    /** Removes player from all store regions */
    public static void removePlayerFromAllRegions(PlayerData pd,Player p) {
        for (int i = 2; i <= 12; i++) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "rg removemember -w world store" + i + " " + p.getName());
            Keys k = getStoreName("store"+i);
            if (k != null) {
                k.updateStoreVisibility(pd,p);
            }
        }
    }

    public static Keys getStoreName(String n){
        return switch (n) {
            case "store1" -> STORE1;
            case "store2" -> STORE2;
            case "store3" -> STORE3;
            case "store4" -> STORE4;
            case "store5" -> STORE5;
            case "store6" -> STORE6;
            case "store7" -> STORE7;
            case "store8" -> STORE8;
            case "store9" -> STORE9;
            case "store10" -> STORE10;
            case "store11" -> STORE11;
            case "store12" -> STORE12;
            default -> null;
        };
    }
    public static String getStoreNameR(String n){
        return switch (n) {
            case "Supermarket" -> "store1";
            case "The Griffin's" -> "store2";
            case "Gym" -> "store3";
            case "Arcade" -> "store4";
            case "School" -> "store5";
            case "Casino" -> "store6";
            case "Oceanarium" -> "store7";
            case "Steakhouse" -> "store8";
            case "Diamond Store" -> "store9";
            case "Balenziaga" -> "store10";
            case "Samzung" -> "store11";
            case "The Bank" -> "store12";
            default -> null;
        };
    }

    public static String getStoreN(String id) {
        return switch (id) {
            case "store1" -> "Supermarket";
            case "store2" -> "The Griffin's";
            case "store3" -> "Gym";
            case "store4" -> "Arcade";
            case "store5" -> "School";
            case "store6" -> "Casino";
            case "store7" -> "Oceanarium";
            case "store8" -> "Steakhouse";
            case "store9" -> "Diamond Store";
            case "store10" -> "Balenziaga";
            case "store11" -> "Samzung";
            case "store12" -> "The Bank";
            default -> null;
        };
    }

    public static int roundToNearest(int price) {
        if (price <= 0) return 0;

        int magnitude = (int) Math.pow(10, (int) Math.log10(price));

        int step;
        if (magnitude < 100) {
            step = 5;
        } else if (magnitude < 1000) {
            step = 10;
        } else if (magnitude < 10000) {
            step = 50;
        } else if (magnitude < 100000) {
            step = 100;
        } else if (magnitude < 1000000) {
            step = 500;
        } else {
            step = 25000;
        }
        return Math.round((float) price / step) * step;
    }

    public static int applyPrestigeIncrease(double currentPrice, PlayerData p) {
        double newPrice = currentPrice * p.getPrestigeBoost();
        return roundToNearest((int) Math.round(newPrice));
    }

    public static String formatNumber(double num) {
        if (num < 1000) return formatDouble(num); // No formatting for small numbers
        if (num < 1_000_000) return formatDouble(num / 1_000) + "K"; // Thousands (K)
        if (num < 1_000_000_000) return formatDouble(num / 1_000_000) + "M"; // Millions (M)
        return formatDouble(num / 1_000_000_000) + "B"; // Billions (B)
    }

    public static String formatDouble(double num) {
        DecimalFormat df = new DecimalFormat("0.##");
        return df.format(num);
    }

    public static Keys getKeyByOrder(int order) {
        return switch (order) {
            case 1  -> STORE1;
            case 2  -> STORE2;
            case 3  -> STORE3;
            case 4  -> STORE4;
            case 5  -> STORE5;
            case 6  -> STORE6;
            case 7  -> STORE7;
            case 8  -> STORE8;
            case 9  -> STORE9;
            case 10 -> STORE10;
            case 11 -> STORE11;
            case 12 -> STORE12;
            default -> null;
        };
    }


}
