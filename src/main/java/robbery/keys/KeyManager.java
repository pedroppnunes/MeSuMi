package robbery.keys;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class KeyManager {
    public final static Keys STORE1 = new Keys("Supermarket",0,1,"&#FB0808S&#FB0808u&#FB0808p&#FB0808e&#FB0808r&#FB0808m&#FB0808a&#FB0808r&#FB0808k&#FB0808e&#FB0808t","store1");
    public final static Keys STORE2 = new Keys("The Griffin's",500L,2,"&#084CFBT&#084CFBh&#084CFBe &#084CFBG&#084CFBr&#084CFBi&#084CFBf&#084CFBf&#084CFBi&#084CFBn&#084CFB'&#084CFBs","store2");
    public final static Keys STORE3 = new Keys("Gym",6_000L,3,"&#FFFFFFG&#FFFFFFy&#FFFFFFm","store3");
    public final static Keys STORE4 = new Keys("Arcade",75_000L,4,"&#FCD05CA&#F4A67Dr&#EB7D9Dc&#E353BEa&#DA2ADEd&#D200FFe","store4");
    public final static Keys STORE5 = new Keys("School",500_000L,5,"&#F1FF00S&#F1FF00c&#F1FF00h&#F1FF00o&#F1FF00o&#F1FF00l","store5");
    public final static Keys STORE6 = new Keys("Casino",3_000_000L,6,"&#FF0000&lC&#FC3300&la&#F96600&ls&#F79900&li&#F4CC00&ln&#F1FF00&lo","store6");
    public final static Keys STORE7 = new Keys("Oceanarium",20_000_000L,7,"&#00EAFF&lO&#00ECE3&lc&#01EFC6&le&#01F1AA&la&#01F38E&ln&#02F671&la&#02F855&lr&#02FA39&li&#03FD1C&lu&#03FF00&lm","store7");
    public final static Keys STORE8 = new Keys("Steakhouse",80_000_000L,8,"&#732F00&lS&#833800&lt&#924000&le&#A24900&la&#B15100&lk&#C15A00&lh&#D06200&lo&#E06B00&lu&#EF7300&ls&#FF7C00&le","store8");
    public final static Keys STORE9 = new Keys("Diamond Store",300_000_000L,9,"&#00FFFF&lD&#13EAFF&li&#27D5FF&la&#3ABFFF&lm&#4DAAFF&lo&#6095FF&ln&#7480FF&ld &#9A55FF&lS&#AD40FF&lt&#C12BFF&lo&#D415FF&lr&#E700FF&le","store9");
    public final static Keys STORE10 = new Keys("Balenziaga",1_100_000_000L,10,"&#FFFFFF&lB&#FFFFFF&la&#FFFFFF&ll&#FFFFFF&le&#808080&ln&#000000&lz&#808080&li&#FFFFFF&la&#FFFFFF&lg&#FFFFFF&la","store10");
    public final static Keys STORE11 = new Keys("Samzung",2_250_000_000L,11,"&#00FF07&lS&#04EA06&la&#07D405&lm&#0BBF04&lz&#0FA902&lu&#129401&ln&#167E00&lg","store11");
    public final static Keys STORE12 = new Keys("The Bank",7_000_000_000L,12,"&#FFB000&lT&#FFB300&lh&#FFB600&le &#FFBB00&lB&#FFBE00&la&#FFC100&ln&#FFC400&lk","store12");


    /** Removes player from all store regions */
    public static void removePlayerFromAllRegions(Player p) {
        for (int i = 2; i <= 13; i++) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "rg removemember -w world store" + i + " " + p.getName());
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
