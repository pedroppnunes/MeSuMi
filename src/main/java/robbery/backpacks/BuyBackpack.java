package robbery.backpacks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.backpacks.BackpackManager;
import robbery.backpacks.Backpacks;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /buybackpack command, allowing a player to purchase a backpack
 * from the in-game store.
 * <p>
 * Players can buy backpacks only if they meet the required prestige level
 * and have enough in-game currency. If the player already owns the backpack,
 * it is simply given to their inventory without charging.
 * </p>
 * <p>
 * Usage: /buybackpack backpack_name player_name
 * </p>
 */
public class BuyBackpack implements CommandExecutor {

    /**
     * Constructs a BuyBackpack command handler.
     *
     * @param main the main plugin instance (not used in this class but kept for consistency)
     */
    public BuyBackpack(Robbery main) {
    }

    /**
     * Executes the /buybackpack command.
     *
     * @param sender  the sender of the command
     * @param command the command object
     * @param label   the command alias used
     * @param args    the command arguments: backpack_name player_name
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {

        if (args.length < 2) {
            Messages.send(sender, "command.buyback.usage");
            return true;
        }

        String backName = args[0].toLowerCase();
        Player player = Bukkit.getPlayer(args[1]);

        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData data = PlayerDataManager.getPlayerData(player);
        Backpacks backpack = BackpackManager.getBackpackName(backName, data.getExtraSlots());

        Economy econ = Robbery.getEconomy();

        if (backpack == null){
            Messages.send(sender, "command.buyback.invalid-backpack");
            return true;
        }

        // Determine required prestige for specific backpacks
        int prestige = data.getPrestige();
        int requiredPrestige = (backName.equalsIgnoreCase("back17") || backName.equalsIgnoreCase("back18")) ? 1
                : (backName.equalsIgnoreCase("back19") || backName.equalsIgnoreCase("back20")) ? 2
                : 0;

        if (prestige < requiredPrestige) {
            Map<String, String> ph = new HashMap<>();
            ph.put("prestige", String.valueOf(requiredPrestige));
            Messages.sendFormatted(player, "command.buyback.require-prestige", ph);
            return true;
        }

        // If player already owns backpack, just give it to inventory
        if (data.hasBackpackName(backName)) {
            data.setnewBackpack(backpack);
            data.giveBackpackToInv();
            Messages.send(player, "command.buyback.already-owned");
            return true;
        }

        double price = backpack.getPrice();
        if (econ.getBalance(player) >= price) {
            econ.withdrawPlayer(player, price);
            data.setBackpack(backpack);
            data.addBackpackName(backName);
            data.giveBackpackToInv();

            Map<String, String> ph = new HashMap<>();
            ph.put("backpack", BackpackManager.getBackPackN(backName));
            ph.put("price", backpack.getPriceformatted());
            Messages.sendFormatted(player, "command.buyback.success", ph);
        } else {
            Map<String, String> ph = new HashMap<>();
            ph.put("price", backpack.getPriceformatted());
            Messages.sendFormatted(player, "global.no-money", ph);
        }

        return true;
    }
}
