package robbery.crypto;

import org.bukkit.entity.Player;
import robbery.core.Robbery;

public class CryptoDealerGUI {

    private final Robbery plugin;

    public CryptoDealerGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player != null && player.isOnline()) {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_dealer " + player.getName());
        }
    }
}
