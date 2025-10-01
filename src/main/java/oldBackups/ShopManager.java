/*package robbery.events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.Robbery;

import java.io.File;

public class ShopManager {
    private static final Robbery main = Robbery.getInstance();
    private static FileConfiguration shopConfig;
    private static File shopFile;

    public static void loadShop() {
        shopFile = new File(main.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            main.saveResource("shop.yml", false); // copies default from jar if not exists
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
    }

    public static FileConfiguration getShopConfig() {
        return shopConfig;
    }

    public static void reloadShop() {
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);
        main.getLogger().info("shop.yml reloaded.");
    }
}

 */

