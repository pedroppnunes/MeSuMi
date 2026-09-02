package robbery.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.robberyLevel_XP.XPManager;
import robbery.util.ItemsReloader;

import java.util.*;

/**
 * Handles /robbery command and subcommands:
 *  - /robbery reload
 *  - /robbery admin xp <give|set|remove|setlevel|reset|calculate> <player> [amount]
 *  - /robbery admin sp <give|set|remove|reset> <player> [amount]
 *  - /robbery admin itemsstolen <give|set|remove|reset> <player> [amount]
 *  - /robbery admin additems <itemName>
 *  - /robbery admin reload
 */
public class RobberyReload implements CommandExecutor, TabCompleter {

    private final Robbery main;

    public RobberyReload(Robbery main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. Direct /robbery reload
        if (sub.equals("reload")) {
            if (!hasAdminPerm(sender)) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            handleReload(sender);
            return true;
        }

        // 2. /robbery admin ...
        if (sub.equals("admin")) {
            if (!hasAdminPerm(sender)) {
                Messages.send(sender, "global.no-permission");
                return true;
            }

            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }

            String category = args[1].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 2, args.length);

            switch (category) {
                case "xp" -> handleXp(sender, subArgs);
                case "sp", "skillpoints" -> handleSp(sender, subArgs);
                case "itemsstolen", "items_stolen", "stolenitems", "stolen" -> handleItemsStolen(sender, subArgs);
                case "additems", "additem" -> handleAddItems(sender, subArgs);
                case "reload" -> handleReload(sender);
                default -> sendHelp(sender);
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private boolean hasAdminPerm(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        return sender.isOp() || sender.hasPermission("robbery.op") || sender.hasPermission("robbery.admin");
    }

    private void handleReload(CommandSender sender) {
        reloadAddItems();
        Messages.reload();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aConfiguration and items reloaded successfully!"));
    }

    // --- XP Category ---
    private void handleXp(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp <give|set|remove|setlevel|reset|calculate> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        XPManager xpManager = main.getXpManager();

        if (action.equals("calculate")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp calculate <itemID>");
                return;
            }
            String itemId = args[1].toLowerCase();
            double hp = main.getItemConfig().getDouble("items." + itemId + ".hp", -1.0);
            if (hp == -1.0) {
                sender.sendMessage(ChatColor.RED + "Item ID '" + itemId + "' not found in additems.yml!");
                return;
            }
            double xpPerItem = Math.pow(hp, 0.85) * 1.4;
            sender.sendMessage("§8§m---------------------------------");
            sender.sendMessage("§aCalculation for: §f" + itemId);
            sender.sendMessage("§2HP: §f" + hp);
            sender.sendMessage("§2Base XP per Item: §f" + String.format("%.2f", xpPerItem));
            sender.sendMessage("§8§m---------------------------------");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp " + action + " <player> [amount]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        try {
            switch (action) {
                case "give" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp give <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(args[2]);
                    xpManager.addXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aXP to &f" + target.getName() + "&a."));
                }
                case "set" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp set <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(args[2]);
                    xpManager.setXP(target, amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aXP to &e" + amount + "&a."));
                }
                case "remove" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp remove <player> <amount>");
                        return;
                    }
                    long amount = Long.parseLong(args[2]);
                    long newXp = Math.max(0L, pd.getXp() - amount);
                    xpManager.setXP(target, newXp);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aXP from &f" + target.getName() + "&a (New XP: &e" + newXp + "&a)."));
                }
                case "setlevel" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin xp setlevel <player> <level>");
                        return;
                    }
                    int level = Integer.parseInt(args[2]);
                    xpManager.setLevel(target, level);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aLevel to &e" + level + "&a."));
                }
                case "reset" -> {
                    xpManager.setXP(target, 0L);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aXP and level to default."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown xp action: " + action);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    // --- SP Category ---
    private void handleSp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp <give|set|remove|reset> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        try {
            switch (action) {
                case "give" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp give <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    pd.addSkillPoints(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aGiven &e" + amount + " &aSkill Points to &f" + target.getName() + " &7(Total: &e" + pd.getSkillPoints() + "&7)."));
                }
                case "set" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp set <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    pd.setSkillPoints(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aSkill Points to &e" + pd.getSkillPoints() + "&a."));
                }
                case "remove" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin sp remove <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    int newSp = Math.max(0, pd.getSkillPoints() - amount);
                    pd.setSkillPoints(newSp);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aSkill Points from &f" + target.getName() + " &7(New: &e" + newSp + "&7)."));
                }
                case "reset" -> {
                    pd.setSkillPoints(0);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aSkill Points to 0."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown sp action: " + action);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    // --- ItemsStolen Category ---
    private void handleItemsStolen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen <give|set|remove|reset> <player> [amount]");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            Messages.send(sender, "global.player-not-found");
            return;
        }

        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        try {
            switch (action) {
                case "give" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen give <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    pd.addItemsStolen(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aAdded &e" + amount + " &aItems Stolen to &f" + target.getName() + " &7(Total: &e" + pd.getItemsStolen() + "&7)."));
                }
                case "set" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen set <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    pd.setItemsStolen(amount);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSet &f" + target.getName() + "'s &aItems Stolen to &e" + pd.getItemsStolen() + "&a."));
                }
                case "remove" -> {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /robbery admin itemsstolen remove <player> <amount>");
                        return;
                    }
                    int amount = Integer.parseInt(args[2]);
                    int newStolen = Math.max(0, pd.getItemsStolen() - amount);
                    pd.setItemsStolen(newStolen);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aRemoved &e" + amount + " &aItems Stolen from &f" + target.getName() + " &7(New: &e" + newStolen + "&7)."));
                }
                case "reset" -> {
                    pd.setItemsStolen(0);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aReset &f" + target.getName() + "'s &aItems Stolen to 0."));
                }
                default -> sender.sendMessage(ChatColor.RED + "Unknown itemsstolen action: " + action);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
        }
    }

    // --- AddItems Category ---
    private void handleAddItems(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by players in-game.");
            return;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /robbery admin additems <itemName>");
            if (Robbery.getItemsMap() != null && !Robbery.getItemsMap().isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Available items: " + ChatColor.YELLOW + String.join(", ", Robbery.getItemsMap().keySet()));
            }
            return;
        }

        String itemName = args[0].toLowerCase();
        Items template = Robbery.getItemsMap() != null ? Robbery.getItemsMap().get(itemName) : null;

        if (template == null) {
            player.sendMessage(ChatColor.RED + "Item '" + itemName + "' not found!");
            if (Robbery.getItemsMap() != null) {
                player.sendMessage(ChatColor.GRAY + "Available: " + ChatColor.YELLOW + String.join(", ", Robbery.getItemsMap().keySet()));
            }
            return;
        }

        Items selectedItem = new Items(template);
        spawnFloatingItem(player, selectedItem);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&lRobbery &8> &aSuccessfully spawned floating &e" + selectedItem.getName() + "&a!"));
    }

    public Item spawnFloatingItem(Player player, Items item) {
        Location spawnLoc = player.getLocation();
        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);

        World world = player.getWorld();
        item.setPosition(spawnLoc.clone());
        ItemStack skull = item.getSkull();

        ArmorStand stand = world.spawn(spawnLoc.clone(), ArmorStand.class);
        stand.setInvisible(true);
        stand.setHealth(20);
        stand.setArms(false);
        stand.setBasePlate(false);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setCustomNameVisible(false);
        stand.setCustomName(item.getUniqueId().toString());
        stand.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());

        NamespacedKey key = new NamespacedKey("robbery", "item_uuid");
        PersistentDataContainer dataContainer = stand.getPersistentDataContainer();
        dataContainer.set(key, PersistentDataType.STRING, item.getUniqueId().toString());

        Item droppedItem = world.spawn(spawnLoc.clone(), Item.class);
        droppedItem.setItemStack(skull);
        droppedItem.setPickupDelay(Integer.MAX_VALUE);
        droppedItem.setUnlimitedLifetime(true);
        droppedItem.setVelocity(new Vector(0, 0, 0));
        droppedItem.setGravity(false);
        droppedItem.setCustomName(item.getUniqueId().toString());

        item.setDroppedItem(droppedItem);
        main.addItems(item);

        return droppedItem;
    }

    public void reloadAddItems() {
        int updated = ItemsReloader.reloadAndSync(main);
        Messages.sendFormatted(Bukkit.getConsoleSender(), "command.reload.updated-items", Map.of("count", String.valueOf(updated)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8=== §5§lRobbery Admin Commands §8===");
        sender.sendMessage("§d/robbery reload §7- Reload plugin data & items");
        sender.sendMessage("§d/robbery admin xp <give|set|remove|setlevel|reset> <player> [amount] §7- Manage player XP");
        sender.sendMessage("§d/robbery admin sp <give|set|remove|reset> <player> [amount] §7- Manage Skill Points");
        sender.sendMessage("§d/robbery admin itemsstolen <give|set|remove|reset> <player> [amount] §7- Manage Items Stolen");
        sender.sendMessage("§d/robbery admin additems <itemName> §7- Spawn floating store item");
        sender.sendMessage("§d/robbery admin reload §7- Reload plugin data & items");
        sender.sendMessage("§8=================================");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPerm(sender)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("admin", "reload"));
            return filter(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            completions.addAll(List.of("xp", "sp", "itemsstolen", "additems", "reload"));
            return filter(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            switch (cat) {
                case "xp" -> completions.addAll(List.of("give", "set", "remove", "setlevel", "reset", "calculate"));
                case "sp", "itemsstolen" -> completions.addAll(List.of("give", "set", "remove", "reset"));
                case "additems", "additem" -> {
                    if (Robbery.getItemsMap() != null) {
                        completions.addAll(Robbery.getItemsMap().keySet());
                    }
                }
            }
            return filter(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            String cat = args[1].toLowerCase();
            if (cat.equals("xp") || cat.equals("sp") || cat.equals("itemsstolen")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
            return filter(completions, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}

