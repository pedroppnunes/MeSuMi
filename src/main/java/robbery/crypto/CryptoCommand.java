package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.messages.Messages;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import robbery.core.Robbery;
import robbery.items.Items;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CryptoCommand implements CommandExecutor {

    private final Robbery plugin;

    public CryptoCommand(Robbery plugin) {
        this.plugin = plugin;
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

                if (pd != null && !pd.hasTalkedToCryptoNPC()) {
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

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("robbery.op") && !sender.isOp()) {
                Messages.send(sender, "global.no-permission");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Messages.colorize("&cAdmin Commands:"));
                sender.sendMessage(Messages.colorize("&e/crypto admin resetnpc <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin givemachine <player> [force]"));
                sender.sendMessage(Messages.colorize("&e/crypto admin check <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin upgradespeed <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin upgradefueltime <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin upgradebatterytime <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin upgradereward <player>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin addstoredfuel <player> <quality>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin addstoredbattery <player> <quality>"));
                sender.sendMessage(Messages.colorize("&e/crypto admin sacrifice <player>"));
                return true;
            }

            String action = args[1];
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }

            if (action.equalsIgnoreCase("resetnpc")) {
                PlayerData pd = PlayerDataManager.getPlayerData(target);
                if (pd != null) {
                    pd.setTalkedToCryptoNPC(false);
                    pd.setTalkedToCryptoBatteryNPC(false);
                    Robbery.getInstance().getPlayerEventListener().savePlayerData(target, pd);
                    target.sendMessage(Messages.colorize("&aYour Crypto NPC dialogue has been reset!"));
                    sender.sendMessage(Messages.colorize("&aReset NPC dialogue for " + target.getName()));
                    
                    // Trigger visibility update
                    new robbery.crypto.CryptoNPCListener(plugin).updateNPCVisibility(target);
                }
                return true;
            }

            CryptoMachine machine = plugin.getCryptoManager().getMachine(target.getUniqueId());
            if (machine == null) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }

            if (action.equalsIgnoreCase("givemachine") || action.equalsIgnoreCase("givereward")) {
                boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
                if (!force && CryptoItemHelper.playerAlreadyHasMachine(target, plugin)) {
                    Messages.send(sender, "crypto.already-possess");
                    return true;
                }
                ItemStack machineItem = CryptoItemHelper.createMachineItem(plugin);
                if (target.getInventory().firstEmpty() != -1) {
                    target.getInventory().addItem(machineItem);
                } else {
                    target.getWorld().dropItemNaturally(target.getLocation(), machineItem);
                }
                Messages.sendFormatted(sender, "crypto.admin-give-machine", "player", target.getName());
            } else if (action.equalsIgnoreCase("upgradespeed")) {
                int newLvl = Math.min(CryptoUpgradeManager.MAX_LEVEL, machine.getSpeedLevel() + 1);
                machine.setSpeedLevel(newLvl);
                Messages.sendFormatted(sender, "crypto.admin-upgrade-speed", Map.of("player", target.getName(), "level", String.valueOf(machine.getSpeedLevel())));
            } else if (action.equalsIgnoreCase("upgradefueltime") || action.equalsIgnoreCase("upgradebatterytime")) {
                int newLvl = Math.min(CryptoUpgradeManager.MAX_LEVEL, machine.getFuelTimeLevel() + 1);
                machine.setFuelTimeLevel(newLvl);
                Messages.sendFormatted(sender, "crypto.admin-upgrade-fueltime", Map.of("player", target.getName(), "level", String.valueOf(machine.getFuelTimeLevel()), "duration", CryptoMachine.getFuelDurationFormattedForLevel(machine.getFuelTimeLevel())));
            } else if (action.equalsIgnoreCase("upgradereward")) {
                int newLvl = Math.min(CryptoUpgradeManager.MAX_LEVEL, machine.getRewardLevel() + 1);
                machine.setRewardLevel(newLvl);
                Messages.sendFormatted(sender, "crypto.admin-upgrade-reward", Map.of("player", target.getName(), "level", String.valueOf(machine.getRewardLevel())));
            } else if (action.equalsIgnoreCase("addstoredfuel") || action.equalsIgnoreCase("addstoredbattery")) {
                if (args.length < 4) return true;
                double quality = Double.parseDouble(args[3]);
                machine.addStoredFuel(new StoredFuel(quality));
                Messages.sendFormatted(sender, "crypto.admin-add-storedfuel", Map.of("player", target.getName(), "quality", String.format("%.1f", quality)));
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
                sender.sendMessage(Messages.colorize(" &7- &fSpeed Level: &a" + machine.getSpeedLevel() + "&8/&a" + CryptoUpgradeManager.MAX_LEVEL));
                sender.sendMessage(Messages.colorize(" &7- &fBattery Time Level: &a" + machine.getFuelTimeLevel() + "&8/&a" + CryptoUpgradeManager.MAX_LEVEL));
                sender.sendMessage(Messages.colorize(" &7- &fReward Level: &a" + machine.getRewardLevel() + "&8/&a" + CryptoUpgradeManager.MAX_LEVEL));
                sender.sendMessage(Messages.colorize(" &7- &fStored Batteries: &e" + machine.getStoredFuels().size() + "&8/&a36"));
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

                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                            Messages.sendFormatted(p, "crypto.battery-loaded", Map.of("quality", String.format("%.1f", fuelObj.getQuality()), "duration", CryptoMachine.getFuelDurationFormattedForTicks(scaledDurationTicks)));
                        } else if (subAction.equalsIgnoreCase("trash") || subAction.equalsIgnoreCase("delete")) {
                            machine.getStoredFuels().remove(index);
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                            Messages.send(p, "crypto.battery-trashed");
                        }
                    }
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
                        int amt = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
                        int avail = sm.getAvailableAmountInBackpack(p, itemId);
                        sm.addSelectedAmount(p.getUniqueId(), itemId, amt, avail);
                    } else if (subAction.equalsIgnoreCase("remove") && args.length >= 3) {
                        String itemId = args[2];
                        int amt = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
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

                long money = machine.getUnclaimedMoney();
                if (money > 0) {
                    Robbery.getEconomy().depositPlayer(p, money);
                    machine.setUnclaimedMoney(0);
                    machine.updateHologram();
                    Messages.sendFormatted(p, "crypto.claim", "money", robbery.number.NumberFormatter.formatDoubleNumber((double) money));
                } else {
                    Messages.send(p, "crypto.no-claim");
                }
                return true;
            }
            else if (args[0].equalsIgnoreCase("pickup")) {
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
}
