/*package robbery.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import robbery.Robbery;
import robbery.events.ShopManager;

import java.util.HashMap;
import java.util.Map;

public class ShopBuy implements CommandExecutor {

    private final Robbery main;
    private final Economy econ;
    private final Map<String, Integer> unitPrice = new HashMap<>();

    public ShopBuy(Robbery main) {
        this.main = main;
        this.econ = main.getEconomy();

        FileConfiguration shop = ShopManager.getShopConfig();
        for (String key : shop.getKeys(false)) {
            int price = shop.getInt(key + ".price");
            unitPrice.put(key.toUpperCase(), price);
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /shopbuy <item>_<amount|full> <player>");
            return true;
        }

        // parse item key + quantity
        String[] parts = args[0].toUpperCase().split("_", 2);
        if (parts.length != 2) {
            sender.sendMessage(ChatColor.RED + "Invalid format! Use IRON_10 or GOLD_FULL");
            return true;
        }

        String key = parts[0];
        String qtyStr = parts[1];

        Integer priceEach = unitPrice.get(key);
        if (priceEach == null) {
            sender.sendMessage(ChatColor.RED + "Unknown shop item: " + key);
            return true;
        }

        // find target player in SuperiorWorld
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null
                || !target.isOnline()
                || !target.getWorld().getName().equals("SuperiorWorld")) {
            sender.sendMessage(ChatColor.RED +
                    "Player not found, not online, or not in SuperiorWorld.");
            return true;
        }

        String materialName = ShopManager.getShopConfig().getString(key + ".material");
        assert materialName != null;
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            sender.sendMessage(ChatColor.RED + "Cannot resolve material for: " + key);
            return true;
        }

        PlayerInventory inv = target.getInventory();

        // calculate how many of this material the inventory can still fit
        int capacity = 0;
        for (ItemStack stack : inv.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                capacity += mat.getMaxStackSize();
            } else if (stack.getType() == mat) {
                capacity += mat.getMaxStackSize() - stack.getAmount();
            }
        }
        if (capacity == 0) {
            target.sendMessage(ChatColor.RED +
                    "You have no room left for " + key.toLowerCase() + "!");
            return true;
        }

        // parse desired amount
        int desired;
        if (qtyStr.equalsIgnoreCase("full")) {
            desired = capacity;
        } else {
            try {
                desired = Integer.parseInt(qtyStr);
                if (desired < 1) {
                    sender.sendMessage(ChatColor.RED + "Quantity must be positive.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + qtyStr);
                return true;
            }
        }

        // cap to capacity
        int toGive = Math.min(desired, capacity);
        double cost = toGive * priceEach;
        double balance = econ.getBalance(target);

        if (balance < cost) {
            target.sendMessage(ChatColor.RED +
                    "You need $" + cost + " but only have $" + balance);
            return true;
        }

        // do transaction
        econ.withdrawPlayer(target, cost);
        inv.addItem(new ItemStack(mat, toGive));
        target.updateInventory();

        target.sendMessage(ChatColor.GREEN +
                "Purchased " + toGive + " × " + key.toLowerCase() +
                " for $" + cost + ".");

        return true;
    }
}

 */
