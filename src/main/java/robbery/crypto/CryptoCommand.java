package robbery.crypto;

import org.MSM.mesumiEconomy.economy.MoneyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CryptoCommand implements CommandExecutor {

    private final Robbery plugin;

    public CryptoCommand(Robbery plugin) {
        this.plugin = plugin;
    }

    private void sendAdminUsage(CommandSender sender) {
        sender.sendMessage(Messages.colorize("&cAdmin Commands:"));
        sender.sendMessage(Messages.colorize("&e/crypto admin resetnpc <player>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin givemachine <player> [force]"));
        sender.sendMessage(Messages.colorize("&e/crypto admin check <player>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin addcredits <player> <amount>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin upgrade <player>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin addstoredfuel <player> <quality>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin addstoredbattery <player> <quality>"));
        sender.sendMessage(Messages.colorize("&e/crypto admin sacrifice <player>"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                PlayerData pd = PlayerDataManager.getPlayerData(p);

                // Require Arcade (Store 4+) or Prestige 1+
                boolean hasKnowledge = pd != null && (pd.getPrestige() >= 1
                        || pd.getHighestOwnedStoreTier() >= 4);

                if (!hasKnowledge) {
                    Messages.send(p, "crypto-dealer.not-enough-knowledge");
                    return true;
                }

                if (!pd.hasTalkedToCryptoNPC()) {
                    Messages.send(p, "crypto.must-talk-npc");
                    return true;
                }
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_machine " + p.getName());
            } else {
                Messages.send(sender, "global.player-only");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("resetnpc")) {
            if (!sender.hasPermission("robbery.op") && !sender.isOp()) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(Messages.colorize("&cUsage: /crypto resetnpc <player>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }
            PlayerData pd = PlayerDataManager.getPlayerData(target);
            if (pd != null) {
                pd.setTalkedToCryptoNPC(false);
                pd.setTalkedToCryptoBatteryNPC(false);
                Robbery.getInstance().getPlayerEventListener().savePlayerData(target, pd);
                target.sendMessage(Messages.colorize("&aYour Crypto NPC dialogue has been reset!"));
                sender.sendMessage(Messages.colorize("&aReset NPC dialogue for " + target.getName()));
                new CryptoNPCListener(plugin).updateNPCVisibility(target);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("credits") || args[0].equalsIgnoreCase("balance")) {
            if (sender instanceof Player p) {
                PlayerData pd = PlayerDataManager.getPlayerData(p);
                int credits = (pd != null) ? pd.getCryptoCredits() : 0;
                p.sendMessage(Messages.colorize("&8[&dCrypto&8] &7Your Crypto Credits: &e" + credits + " Credits"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("buycredits") || args[0].equalsIgnoreCase("buycredit")) {
            if (!sender.hasPermission("robbery.op") && !sender.isOp()) {
                Messages.send(sender, "global.no-permission");
                return true;
            }

            if (!(sender instanceof Player p)) {
                Messages.send(sender, "global.player-only");
                return true;
            }

            if (!p.getWorld().getName().equalsIgnoreCase("SuperiorWorld")) {
                p.sendMessage(Messages.colorize("&cYou can only exchange XP for Crypto Credits while in SuperiorWorld!"));
                return true;
            }

            int amount = 1;
            if (args.length >= 2) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[1]));
                } catch (NumberFormatException ignored) {}
            }

            int xpPerCredit = CryptoUpgradeManager.getXpPerCredit();
            int totalXpNeeded = amount * xpPerCredit;

            int currentTotalExp = getTotalExperience(p);
            if (currentTotalExp < totalXpNeeded) {
                p.sendMessage(Messages.colorize("&cYou need &e" + String.format("%,d", totalXpNeeded) + " XP &cto buy &e" + amount + " Crypto Credit(s)&c! (You have: &e" + String.format("%,d", currentTotalExp) + " XP&c)"));
                return true;
            }

            setTotalExperience(p, currentTotalExp - totalXpNeeded);
            PlayerData pd = PlayerDataManager.getPlayerData(p);
            if (pd != null) {
                pd.addCryptoCredits(amount);
                plugin.getPlayerEventListener().savePlayerData(p, pd);
            }

            p.sendMessage(Messages.colorize("&aSuccessfully purchased &e" + amount + " Crypto Credit(s) &afor &e" + String.format("%,d", totalXpNeeded) + " XP&a!"));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("robbery.op") && !sender.isOp()) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            CryptoUpgradeManager.reloadConfig();
            Messages.send(sender, "crypto.config-reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("robbery.op") && !sender.isOp()) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            if (args.length < 3) {
                sendAdminUsage(sender);
                return true;
            }

            String action = args[1];
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }
            CryptoMachine machine = plugin.getCryptoManager().getOrCreateMachine(target.getUniqueId());

            if (action.equalsIgnoreCase("resetnpc")) {
                PlayerData pd = PlayerDataManager.getPlayerData(target);
                if (pd != null) {
                    pd.setTalkedToCryptoNPC(false);
                    pd.setTalkedToCryptoBatteryNPC(false);
                    Robbery.getInstance().getPlayerEventListener().savePlayerData(target, pd);
                    target.sendMessage(Messages.colorize("&aYour Crypto NPC dialogue has been reset!"));
                    sender.sendMessage(Messages.colorize("&aReset NPC dialogue for " + target.getName()));
                    new CryptoNPCListener(plugin).updateNPCVisibility(target);
                }
                return true;
            }

            if (action.equalsIgnoreCase("givemachine") || action.equalsIgnoreCase("givereward")) {
                boolean force = args.length >= 4 && args[3].equalsIgnoreCase("force");
                if (!force && CryptoItemHelper.playerAlreadyHasMachine(target, plugin)) {
                    Messages.send(sender, "crypto.already-possess");
                    return true;
                }
                plugin.getCryptoManager().getOrCreateMachine(target.getUniqueId());
                robbery.keys.Rcrate.addPendingCryptoMachine(target.getUniqueId());
                robbery.keys.Rcrate.saveRewards(target.getUniqueId());

                Messages.sendFormatted(sender, "crypto.admin-give-machine", "player", target.getName());
                if (target.isOnline()) {
                    Messages.send(target, "command.rcrate.notify-claim");
                }
                return true;
            }

            if (action.equalsIgnoreCase("addcredits") || action.equalsIgnoreCase("addcredit")) {
                if (args.length < 4) {
                    sendAdminUsage(sender);
                    return true;
                }
                try {
                    int amt = Math.max(1, Integer.parseInt(args[3]));
                    PlayerData targetPd = PlayerDataManager.getPlayerData(target);
                    if (targetPd != null) {
                        targetPd.addCryptoCredits(amt);
                        Robbery.getInstance().getPlayerEventListener().savePlayerData(target, targetPd);
                        sender.sendMessage(Messages.colorize("&aAdded &e" + amt + " Crypto Credit(s) &ato &b" + target.getName() + "&a!"));
                        if (target.isOnline()) {
                            target.sendMessage(Messages.colorize("&aYou received &e" + amt + " Crypto Credit(s)&a!"));
                        }
                    }
                } catch (NumberFormatException e) {
                    sendAdminUsage(sender);
                }
                return true;
            }

            if (action.equalsIgnoreCase("upgrade")) {
                CryptoUpgradeManager.upgradeMachine(target, machine);
                sender.sendMessage(Messages.colorize("&aAttempted upgrade for " + target.getName()));
                return true;
            } else if (action.equalsIgnoreCase("addstoredfuel") || action.equalsIgnoreCase("addstoredbattery")) {
                if (args.length < 4) {
                    sendAdminUsage(sender);
                    return true;
                }
                try {
                    double quality = Double.parseDouble(args[3]);
                    machine.addStoredFuel(new StoredFuel(quality));
                    plugin.getCryptoManager().saveMachine(machine);
                    Messages.sendFormatted(sender, "crypto.admin-add-storedfuel", Map.of("player", target.getName(), "quality", String.format("%.1f", quality)));
                } catch (NumberFormatException e) {
                    sendAdminUsage(sender);
                }
            } else if (action.equalsIgnoreCase("sacrifice")) {
                PlayerData pd = PlayerDataManager.getPlayerData(target);
                if (pd == null) return true;
                new CryptoSacrificeGUI(plugin).open(target);
                Messages.sendFormatted(sender, "crypto.admin-open-sacrifice", "player", target.getName());
            } else if (action.equalsIgnoreCase("check")) {
                sender.sendMessage(Messages.colorize("&8[&dCrypto Admin&8] &a" + target.getName() + "'s Machine Stats:"));
                sender.sendMessage(Messages.colorize(" &7- &fStatus: " + (machine.getFuelTicks() > 0 ? (target.isOnline() ? "&aActive &7(&a+20% Online Buff&7)" : "&aActive") : "&cInactive")));
                sender.sendMessage(Messages.colorize(" &7- &fUnclaimed Money: &e$" + robbery.number.NumberFormatter.formatDoubleNumber((double) machine.getUnclaimedMoney())));
                sender.sendMessage(Messages.colorize(" &7- &fFuel Remaining: &e" + CryptoMachine.getFuelDurationFormattedForTicks(machine.getFuelTicks())));
                if (machine.getFuelTicks() > 0) {
                    sender.sendMessage(Messages.colorize(" &7- &fCurrent Fuel Quality: &e" + String.format("%.1f%%", machine.getFuelQuality())));
                }
                sender.sendMessage(Messages.colorize(" &7- &fSpeed Level: &a" + machine.getSpeedLevel() + "&8/&a" + CryptoUpgradeManager.getMaxLevel()));
                sender.sendMessage(Messages.colorize(" &7- &fBattery Time Level: &a" + machine.getFuelTimeLevel() + "&8/&a" + CryptoUpgradeManager.getMaxLevel()));
                sender.sendMessage(Messages.colorize(" &7- &fReward Level: &a" + machine.getRewardLevel() + "&8/&a" + CryptoUpgradeManager.getMaxLevel()));
                sender.sendMessage(Messages.colorize(" &7- &fStored Batteries: &e" + machine.getStoredFuels().size() + "&8/&a36"));
            } else {
                sendAdminUsage(sender);
            }
            return true;
        }

        if (sender instanceof Player p) {
            PlayerData pd = PlayerDataManager.getPlayerData(p);
            if (pd != null && !pd.hasTalkedToCryptoNPC()) {
                Messages.send(p, "crypto.must-talk-npc");
                return true;
            }

            CryptoMachine machine = plugin.getCryptoManager().getMachine(p.getUniqueId());

            // Upgrade command
            if (args[0].equalsIgnoreCase("upgrade") && args.length >= 2) {
                String track = args[1];
                if (machine == null) {
                    Messages.send(p, "crypto.already-possess");
                    return true;
                }
                CryptoUpgradeManager.upgradeTrack(p, machine, track);
                return true;
            }

            // Dealer / Buy commands
            if (args[0].equalsIgnoreCase("dealer") || args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("shop")) {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_dealer " + p.getName());
                return true;
            }

            // Battery / Fuel Storage Commands
            if (args[0].equalsIgnoreCase("battery") || args[0].equalsIgnoreCase("batterystorage") || args[0].equalsIgnoreCase("fuel") || args[0].equalsIgnoreCase("fuelstorage") || args[0].equalsIgnoreCase("storage")) {
                if (args.length >= 3) {
                    String subAction = args[1];
                    try {
                        int index = Integer.parseInt(args[2]) - 1; // 1-based index from DeluxeMenus

                        if (machine != null && index >= 0 && index < machine.getStoredFuels().size()) {
                            StoredFuel fuelObj = machine.getStoredFuels().get(index);
                            if (subAction.equalsIgnoreCase("load")) {
                                if (machine.getFuelTicks() > 0) {
                                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                                    Messages.send(p, "crypto.battery-already-active");
                                    return true;
                                }
                                long baseDurationTicks = machine.getFuelDurationTicks();
                                long scaledDurationTicks = (long) (baseDurationTicks * (fuelObj.getQuality() / 100.0));
                                machine.setFuelTicks(scaledDurationTicks);
                                machine.setFuelQuality(fuelObj.getQuality());
                                machine.getStoredFuels().remove(index);
                                plugin.getCryptoManager().saveMachine(machine);

                                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                                Messages.sendFormatted(p, "crypto.battery-loaded", Map.of("quality", String.format("%.1f", fuelObj.getQuality()), "duration", CryptoMachine.getFuelDurationFormattedForTicks(scaledDurationTicks)));
                            } else if (subAction.equalsIgnoreCase("trash") || subAction.equalsIgnoreCase("delete")) {
                                machine.getStoredFuels().remove(index);
                                plugin.getCryptoManager().saveMachine(machine);
                                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                Messages.send(p, "crypto.battery-trashed");
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                } else {
                    plugin.getCryptoBatteryStorageGUI().open(p);
                }
                return true;
            }

            // Sacrifice Commands
            if (args[0].equalsIgnoreCase("sacrifice")) {
                SacrificeManager sm = plugin.getSacrificeManager();
                if (args.length >= 2) {
                    String subAction = args[1];
                    if (subAction.equalsIgnoreCase("add") && args.length >= 3) {
                        String itemId = args[2];
                        int amt = 1;
                        if (args.length >= 4) {
                            try { amt = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                        }
                        int avail = sm.getAvailableAmountInBackpack(p, itemId);
                        sm.addSelectedAmount(p.getUniqueId(), itemId, amt, avail);
                    } else if (subAction.equalsIgnoreCase("remove") && args.length >= 3) {
                        String itemId = args[2];
                        int amt = 1;
                        if (args.length >= 4) {
                            try { amt = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                        }
                        int avail = sm.getAvailableAmountInBackpack(p, itemId);
                        sm.addSelectedAmount(p.getUniqueId(), itemId, -amt, avail);
                    } else if (subAction.equalsIgnoreCase("clear")) {
                        sm.clear(p.getUniqueId());
                    } else if (subAction.equalsIgnoreCase("confirm")) {
                        long totalValue = sm.getTotalSacrificeValue(p);
                        if (totalValue <= 0) {
                            Messages.send(p, "crypto.sacrifice-empty");
                            return true;
                        }

                        // Deduct selected items from backpack
                        if (pd != null) {
                            List<Items> liveBackpack = pd.getBackpack().getItems();
                            for (Map.Entry<String, Integer> entry : sm.getSelectedMap(p.getUniqueId()).entrySet()) {
                                String id = entry.getKey();
                                int amountToRemove = entry.getValue();

                                Iterator<Items> it = liveBackpack.iterator();
                                int removed = 0;
                                while (it.hasNext() && removed < amountToRemove) {
                                    Items item = it.next();
                                    if (item.getId().equalsIgnoreCase(id)) {
                                        it.remove();
                                        removed++;
                                    }
                                }
                            }
                        }

                        sm.clear(p.getUniqueId());
                        plugin.getFuelRouletteGUI().startSpin(p, totalValue);
                    }
                } else {
                    plugin.getCryptoSacrificeGUI().open(p);
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("claim")) {
                String worldName = p.getWorld().getName();
                if (!worldName.equalsIgnoreCase("outpost") && !worldName.equalsIgnoreCase("SuperiorWorld") && !worldName.equalsIgnoreCase("world")) {
                    Messages.send(p, "global.not-here");
                    return true;
                }

                if (machine == null) return true;

                double money = machine.getUnclaimedMoneyDouble();
                if (money > 0.0) {
                    MoneyManager moneyManager = Robbery.getMoneyManager();
                    if (moneyManager != null) {
                        moneyManager.addMoney(p.getUniqueId(), money);
                    } else if (Robbery.getEconomy() != null) {
                        Robbery.getEconomy().depositPlayer(p, money);
                    }
                    machine.setUnclaimedMoney(0.0);
                    machine.updateHologram();
                    Messages.sendFormatted(p, "crypto.claim", "money", robbery.number.NumberFormatter.formatDoubleNumber(money));
                } else {
                    Messages.send(p, "crypto.no-claim");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("pickup")) {
                String worldName = p.getWorld().getName();
                if (!worldName.equalsIgnoreCase("outpost") && !worldName.equalsIgnoreCase("SuperiorWorld")) {
                    Messages.send(p, "global.not-here");
                    return true;
                }

                if (machine == null) return true;

                if (machine.isPlaced()) {
                    if (p.getInventory().firstEmpty() == -1) {
                        Messages.send(p, "crypto.inventory-full");
                        return true;
                    }
                    Location loc = machine.getLocation();
                    if (loc != null) {
                        Block block = loc.getBlock();
                        Material type = block.getType();
                        if (type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD || type == Material.LOOM) {
                            block.setType(Material.AIR);
                        }
                    }
                    machine.setLocation(null);
                    machine.updateHologram();
                    ItemStack machineItem = CryptoItemHelper.createMachineItem(plugin);
                    p.getInventory().addItem(machineItem);
                    Messages.send(p, "crypto.pickup");
                }
                return true;
            }
        }
        return true;
    }

    public static int getTotalExperience(Player player) {
        int exp = 0;
        int level = player.getLevel();
        for (int i = 0; i < level; i++) {
            exp += getExpToLevel(i);
        }
        exp += Math.round(getExpToLevel(level) * player.getExp());
        return exp;
    }

    public static int getExpToLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    public static void setTotalExperience(Player player, int exp) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        int currentExp = exp;

        while (currentExp > 0) {
            int expToNextLevel = getExpToLevel(player.getLevel());
            if (currentExp >= expToNextLevel) {
                currentExp -= expToNextLevel;
                player.setLevel(player.getLevel() + 1);
            } else {
                float expFraction = (float) currentExp / (float) expToNextLevel;
                player.setExp(expFraction);
                currentExp = 0;
            }
        }
    }
}
