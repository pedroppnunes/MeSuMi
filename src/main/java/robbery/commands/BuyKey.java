package robbery.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /buykey command, allowing a player to purchase a key from the store.
 * <p>
 * Players must purchase keys in order, cannot skip ahead, and may be restricted
 * based on their prestige level. If the player already owns the key, they cannot
 * purchase it again.
 * </p>
 * <p>
 * Upon successful purchase, the player’s balance is reduced, the key is added
 * to their owned keys, and the player is added to the corresponding region.
 * </p>
 * <p>
 * Usage: /buykey key_name player_name
 * </p>
 */
public class BuyKey implements CommandExecutor {

    /**
     * Constructs a BuyKey command handler.
     *
     * @param main the main plugin instance (not used in this class but kept for consistency)
     */
    public BuyKey(Robbery main) {
    }

    /**
     * Executes the /buykey command.
     *
     * @param sender  the sender of the command
     * @param command the command object
     * @param label   the command alias used
     * @param args    the command arguments: key_name player_name
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {

        if (args.length < 2) {
            Messages.send(sender, "command.buykey.usage");
            return true;
        }

        String keyName = args[0].toLowerCase();
        Player player = Bukkit.getPlayer(args[1]);

        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData data = PlayerDataManager.getPlayerData(player);
        Keys key = KeyManager.getStoreName(keyName);
        if (key == null) {
            Messages.send(sender, "command.buykey.invalid-key");
            return true;
        }

        int orderDiff = key.getOrder() - data.getKey().getOrder();

        // Player already owns the key
        if (data.hasKey(keyName)) {
            Messages.send(player, "command.buykey.already-owned");
            return true;
        }

        // Must buy keys in sequence
        if (orderDiff != 1) {
            Messages.send(player, "command.buykey.must-buy-in-order");
            return true;
        }

        // Prestige restrictions
        int prestige = data.getPrestige();
        int maxAllowedOrder = (prestige == 0) ? 10 : (prestige == 1 ? 11 : Integer.MAX_VALUE);

        if (key.getOrder() > maxAllowedOrder) {
            Map<String, String> ph = new HashMap<>();
            ph.put("prestige", String.valueOf(prestige));
            ph.put("maxorder", String.valueOf(maxAllowedOrder));
            Messages.sendFormatted(player, "command.buykey.require-prestige", ph);
            return true;
        }

        Economy econ = Robbery.getEconomy();
        double price = key.getPrice(data);

        if (econ.getBalance(player) >= price) {
            econ.withdrawPlayer(player, price);
            data.addKey(keyName);
            data.setKey(key);

            // Add player to the key's region
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "rg addmember -w world " + keyName + " " + player.getName());

            Map<String, String> ph = new HashMap<>();
            ph.put("key", KeyManager.getStoreN(keyName));
            ph.put("price", key.getPriceformatted(data));
            Messages.sendFormatted(player, "command.buykey.success", ph);
        } else {
            Map<String, String> ph = new HashMap<>();
            ph.put("price", key.getPriceformatted(data));
            Messages.sendFormatted(player, "global.no-money", ph);
        }

        return true;
    }
}
