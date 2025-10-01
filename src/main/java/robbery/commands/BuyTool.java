package robbery.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.tool.ToolManager;
import robbery.tool.Tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /buytool command, allowing a player to purchase a tool from the store.
 * <p>
 * Players may be restricted from buying certain tools based on their prestige level.
 * Tools must be purchased individually, and if the player already owns a tool,
 * it is simply equipped without additional cost.
 * </p>
 * <p>
 * Upon successful purchase, the player's balance is deducted, the tool is added
 * to their owned tools, and the tool is given to their inventory.
 * </p>
 * <p>
 * Usage: /buytool tool_name player_name
 * </p>
 */
public class BuyTool implements CommandExecutor {

    /**
     * Constructs a BuyTool command handler.
     *
     * @param main the main plugin instance (not used in this class but kept for consistency)
     */
    public BuyTool(Robbery main) {
    }

    /**
     * Executes the /buytool command.
     *
     * @param sender  the sender of the command
     * @param command the command object
     * @param label   the command alias used
     * @param args    the command arguments: tool_name player_name
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {

        if (args.length < 2) {
            Messages.send(sender, "command.buytool.usage");
            return true;
        }

        String toolName = args[0].toLowerCase();
        Player player = Bukkit.getPlayer(args[1]);

        if (player == null || !player.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return true;
        }

        PlayerData data = PlayerDataManager.getPlayerData(player);
        Tools tool = ToolManager.getToolsName(toolName);
        Economy econ = Robbery.getEconomy();

        if (tool == null) {
            Messages.send(sender, "command.buytool.invalid-tool");
            return true;
        }

        int prestige = data.getPrestige();
        Map<String, String> placeholders = new HashMap<>();

        // Check prestige requirements for high-tier tools
        if ((toolName.equalsIgnoreCase("tool17") || toolName.equalsIgnoreCase("tool18")) && prestige < 1) {
            placeholders.put("prestige", "1");
            Messages.sendFormatted(player, "command.buytool.require-prestige", placeholders);
            return true;
        }

        if ((toolName.equalsIgnoreCase("tool19") || toolName.equalsIgnoreCase("tool20")) && prestige < 2) {
            placeholders.put("prestige", "2");
            Messages.sendFormatted(player, "command.buytool.require-prestige", placeholders);
            return true;
        }

        // If the player already owns the tool, equip it
        if (data.hasToolName(toolName)) {
            data.setTool(tool);
            data.giveToolToInv();
            Messages.send(player, "command.buytool.already-owned");
            return true;
        }

        // Purchase the tool if the player has enough money
        double price = tool.getPrice();
        if (econ.getBalance(player) >= price) {
            econ.withdrawPlayer(player, price);
            data.setTool(tool);
            data.addToolsName(toolName);
            data.giveToolToInv();

            placeholders.put("tool", tool.getName());
            placeholders.put("price", tool.getPriceformatted());
            Messages.sendFormatted(player, "command.buytool.success", placeholders);
        } else {
            placeholders.put("price", tool.getPriceformatted());
            Messages.sendFormatted(player, "global.no-money", placeholders);
        }

        return true;
    }
}
