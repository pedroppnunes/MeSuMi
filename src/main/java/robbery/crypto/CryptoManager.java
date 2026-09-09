package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.messages.Messages;
import robbery.notifications.NotificationType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CryptoManager {

    private final Robbery plugin;
    private final Map<UUID, CryptoMachine> activeMachines = new ConcurrentHashMap<>();
    private final CryptoMachineDao dao;

    public CryptoManager(Robbery plugin) {
        this.plugin = plugin;
        this.dao = new CryptoMachineDao(plugin);
        
        loadAllMachinesOnStartup();
        startTask();
    }

    private void loadAllMachinesOnStartup() {
        dao.loadAllMachines().thenAccept(machines -> {
            for (CryptoMachine m : machines) {
                if (m != null) {
                    activeMachines.put(m.getOwnerId(), m);
                }
            }
            plugin.getLogger().info("Loaded " + activeMachines.size() + " Crypto Machines into memory.");
        });
    }
    
    public void loadPlayer(Player player) {
        if (player == null) return;
        dao.loadMachine(player.getUniqueId()).thenAccept(machine -> {
            PlayerData pd = PlayerDataManager.getPlayerData(player);
            
            // Offline progress calculation (item stealing batches)
            if (pd != null && machine != null && machine.isPlaced() && machine.getFuelTicks() > 0 && machine.getLastUpdated() > 0) {
                long now = System.currentTimeMillis();
                long secondsPassed = (now - machine.getLastUpdated()) / 1000L;
                int intervalSeconds = machine.getStealIntervalSeconds();
                long batchesPassed = secondsPassed / (long) intervalSeconds;
                
                if (batchesPassed > 0) {
                    long activeBatches = Math.min(batchesPassed, machine.getFuelTicks() / (long) intervalSeconds);
                    
                    int capacity = machine.getCapacity();
                    double avgTop5Val = CryptoMachine.getAverageTop5ItemValue(pd);
                    double rewardMult = machine.getRewardMultiplier();
                    double qualityMult = machine.getQualityMultiplier();
                    int storeOrder = (pd.getKey() != null) ? pd.getKey().getOrder() : 1;
                    double storeEfficiency = CryptoMachine.getStoreEfficiencyMultiplier(storeOrder);

                    double moneyGenerated = activeBatches * capacity * avgTop5Val * rewardMult * qualityMult * storeEfficiency;
                    
                    machine.addUnclaimedMoney(moneyGenerated);
                    machine.setFuelTicks(machine.getFuelTicks() - (activeBatches * intervalSeconds));
                }
            }
            
            if (machine != null) {
                machine.setLastUpdated(System.currentTimeMillis());
                
                CryptoMachine existing = activeMachines.get(player.getUniqueId());
                if (existing != null && existing != machine) {
                    if (existing.getSpeedLevel() > machine.getSpeedLevel()) machine.setSpeedLevel(existing.getSpeedLevel());
                    if (existing.getCapacityLevel() > machine.getCapacityLevel()) machine.setCapacityLevel(existing.getCapacityLevel());
                    if (existing.getFuelTimeLevel() > machine.getFuelTimeLevel()) machine.setFuelTimeLevel(existing.getFuelTimeLevel());
                    if (existing.getRewardLevel() > machine.getRewardLevel()) machine.setRewardLevel(existing.getRewardLevel());
                    if (existing.getUnclaimedMoneyDouble() > machine.getUnclaimedMoneyDouble()) machine.setUnclaimedMoney(existing.getUnclaimedMoneyDouble());
                    if (existing.getStoredFuels().size() > machine.getStoredFuels().size()) {
                        machine.getStoredFuels().clear();
                        machine.getStoredFuels().addAll(existing.getStoredFuels());
                    }
                    if (existing.isPlaced()) {
                        machine.setLocation(existing.getLocation());
                    }
                }

                activeMachines.put(player.getUniqueId(), machine);
                dao.saveMachine(machine);
                
                if (machine.isPlaced()) {
                    Bukkit.getScheduler().runTask(plugin, machine::updateHologram);
                }
            }
        });
    }
    
    public void unloadPlayer(Player player) {
        if (player == null) return;
        CryptoMachine machine = activeMachines.get(player.getUniqueId());
        if (machine != null) {
            machine.removeHologram();
            dao.saveMachineSync(machine);
        }
    }
    
    public CryptoMachine getMachine(UUID uuid) {
        if (uuid == null) return null;
        CryptoMachine m = activeMachines.get(uuid);
        if (m == null) {
            m = dao.loadMachineSync(uuid);
            if (m != null) {
                activeMachines.put(uuid, m);
            }
        }
        return m;
    }

    public CryptoMachine getOrCreateMachine(UUID uuid) {
        if (uuid == null) return null;
        CryptoMachine m = activeMachines.get(uuid);
        if (m == null) {
            m = dao.loadMachineSync(uuid);
            if (m == null) {
                m = new CryptoMachine(uuid, null, null, null, null, 0L, 0L, 0.0, 0, 0, 0, System.currentTimeMillis());
                dao.saveMachine(m);
            }
            activeMachines.put(uuid, m);
        }
        return m;
    }

    public void saveMachine(CryptoMachine machine) {
        if (machine != null) {
            dao.saveMachine(machine);
        }
    }
    
    public Map<UUID, CryptoMachine> getActiveMachines() {
        return activeMachines;
    }
    
    public void saveAll() {
        for (CryptoMachine machine : activeMachines.values()) {
            dao.saveMachine(machine);
        }
    }

    public void saveAllSync() {
        for (CryptoMachine machine : activeMachines.values()) {
            if (machine != null) {
                machine.removeHologram();
                dao.saveMachineSync(machine);
            }
        }
    }

    private void startTask() {
        final int[] tickCount = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            tickCount[0]++;
            boolean autoSaveTime = (tickCount[0] % 60 == 0); // Auto-save every 60 seconds

            for (Map.Entry<UUID, CryptoMachine> entry : activeMachines.entrySet()) {
                CryptoMachine machine = entry.getValue();

                int intervalSeconds = machine.getStealIntervalSeconds();
                boolean isStealTick = (tickCount[0] % intervalSeconds == 0);

                if (isStealTick) {
                    if (machine.getFuelTicks() >= intervalSeconds && machine.isPlaced()) {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        PlayerData pd = (p != null && p.isOnline()) ? PlayerDataManager.getPlayerData(p) : null;

                        int capacity = machine.getCapacity();
                        double avgTop5Val = CryptoMachine.getAverageTop5ItemValue(pd);
                        double rewardMult = machine.getRewardMultiplier();
                        double qualityMult = machine.getQualityMultiplier();
                        double onlineBuff = (p != null && p.isOnline()) ? 1.20 : 1.0;
                        int storeOrder = (pd != null && pd.getKey() != null) ? pd.getKey().getOrder() : 1;
                        double storeEfficiency = CryptoMachine.getStoreEfficiencyMultiplier(storeOrder);

                        double moneyGenerated = capacity * avgTop5Val * rewardMult * qualityMult * onlineBuff * storeEfficiency;

                        machine.addUnclaimedMoney(moneyGenerated);
                        machine.setFuelTicks(machine.getFuelTicks() - intervalSeconds);
                        machine.setLastUpdated(now);

                        // Notify player if battery just depleted
                        if (machine.getFuelTicks() < intervalSeconds && p != null && p.isOnline()) {
                            if (pd == null || pd.isNotificationEnabled(NotificationType.CRYPTO_MACHINE)) {
                                Messages.send(p, "crypto.battery-stopped");
                            }
                        }
                    } else if (machine.isPlaced()) {
                        machine.setLastUpdated(now);
                    }
                }

                // ALWAYS update hologram if placed, so it doesn't disappear when battery is dead
                if (machine.isPlaced()) {
                    machine.updateHologram();
                }

                if (autoSaveTime) {
                    dao.saveMachine(machine);
                }
            }
        }, 20L, 20L); // Run every second
    }
    
    public double getBaseRatePerMinuteForPlayer(PlayerData pd) {
        if (pd == null) return 120.0;
        int currentOrder = (pd.getKey() != null) ? pd.getKey().getOrder() : 1;
        robbery.keys.Keys nextKey = robbery.keys.KeyManager.getKeyByOrder(currentOrder + 1);
        double storeCost;
        if (nextKey != null) {
            storeCost = nextKey.getPrice(pd);
        } else if (pd.getKey() != null) {
            storeCost = pd.getKey().getPrice(pd);
        } else {
            robbery.keys.Keys defaultKey = robbery.keys.KeyManager.getKeyByOrder(1);
            storeCost = (defaultKey != null) ? defaultKey.getPrice(pd) : 1000.0;
        }

        // Cap max hourly rate at full upgrades + all buffs (9.6x total multiplier):
        // Early stores (Store 1-5): 50% / hour max
        // Mid stores (Store 6-9): 25% / hour max
        // Late stores (Store 10+): 10% / hour max
        double targetHourlyPercent;
        if (currentOrder <= 5) {
            targetHourlyPercent = 0.50;
        } else if (currentOrder <= 9) {
            targetHourlyPercent = 0.25;
        } else {
            targetHourlyPercent = 0.10;
        }

        // 60 minutes * 9.6 (max total multiplier) = 576.0
        return (targetHourlyPercent / 576.0) * storeCost;
    }

    public double getBaseRateForPlayer(PlayerData pd) {
        return getBaseRatePerMinuteForPlayer(pd) / 60.0;
    }

    public double getBaseRateForStore(int storeTier) {
        robbery.keys.Keys storeKey = robbery.keys.KeyManager.getKeyByOrder(storeTier);
        if (storeKey == null) return 15.80;
        java.util.List<robbery.items.Items> storeItems = new java.util.ArrayList<>();
        if (robbery.core.Robbery.getItemsMap() != null) {
            for (java.util.Map.Entry<String, robbery.items.Items> entry : robbery.core.Robbery.getItemsMap().entrySet()) {
                String itemId = entry.getKey();
                robbery.items.Items itemObj = entry.getValue();
                if (itemId == null || itemObj == null) continue;
                int itemStoreNum = CryptoMachine.extractStoreNumStatic(itemId);
                if (itemStoreNum == storeTier || (storeTier == 12 && itemStoreNum == 13) || (storeTier == 13 && itemStoreNum == 12)) {
                    storeItems.add(itemObj);
                }
            }
        }
        if (storeItems.isEmpty()) return 15.80;
        storeItems.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int count = Math.min(5, storeItems.size());
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += storeItems.get(i).getValue();
        }
        return sum / count;
    }

    public double getBatchPayout(CryptoMachine machine) {
        if (machine == null) return 0.0;
        Player p = Bukkit.getPlayer(machine.getOwnerId());
        PlayerData pd = (p != null && p.isOnline()) ? PlayerDataManager.getPlayerData(p) : null;

        int capacity = machine.getCapacity();
        double avgTop5Val = CryptoMachine.getAverageTop5ItemValue(pd);
        double rewardMult = machine.getRewardMultiplier();
        double qualityMult = machine.getQualityMultiplier();
        double onlineBuff = (p != null && p.isOnline() && machine.getFuelTicks() > 0) ? 1.20 : 1.0;
        int storeOrder = (pd != null && pd.getKey() != null) ? pd.getKey().getOrder() : 1;
        double storeEfficiency = CryptoMachine.getStoreEfficiencyMultiplier(storeOrder);

        return capacity * avgTop5Val * rewardMult * qualityMult * onlineBuff * storeEfficiency;
    }

    public double getMoneyPerSecond(CryptoMachine machine) {
        if (machine == null) return 0.0;
        int intervalSeconds = machine.getStealIntervalSeconds();
        if (intervalSeconds <= 0) return 0.0;

        return getBatchPayout(machine) / (double) intervalSeconds;
    }

    public double getMultiplier(CryptoMachine machine) {
        if (machine == null) return 1.0;
        Player p = Bukkit.getPlayer(machine.getOwnerId());
        PlayerData pd = (p != null && p.isOnline()) ? PlayerDataManager.getPlayerData(p) : null;

        double rewardMult = machine.getRewardMultiplier();
        double qualityMult = machine.getQualityMultiplier();
        double onlineBuff = (p != null && p.isOnline() && machine.getFuelTicks() > 0) ? 1.20 : 1.0;
        int storeOrder = (pd != null && pd.getKey() != null) ? pd.getKey().getOrder() : 1;
        double storeEfficiency = CryptoMachine.getStoreEfficiencyMultiplier(storeOrder);

        return rewardMult * qualityMult * onlineBuff * storeEfficiency;
    }

    public void claimMoney(Player player) {
        if (player == null || !player.isOnline()) return;
        CryptoMachine machine = getMachine(player.getUniqueId());
        if (machine == null || machine.getUnclaimedMoneyDouble() <= 0) {
            Messages.send(player, "crypto.no-money");
            return;
        }

        double amount = machine.getUnclaimedMoneyDouble();
        machine.setUnclaimedMoney(0.0);
        saveMachine(machine);

        if (Robbery.getMoneyManager() != null) {
            Robbery.getMoneyManager().addMoney(player.getUniqueId(), amount);
        } else if (Robbery.getEconomy() != null) {
            Robbery.getEconomy().depositPlayer(player, amount);
        }

        player.sendMessage(Messages.colorize("&aClaimed &e$" + robbery.number.NumberFormatter.formatDoubleNumber(amount) + " &afrom your Crypto Machine!"));
    }

    public void pickupMachine(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!player.getWorld().getName().equalsIgnoreCase("SuperiorWorld")) {
            Messages.send(player, "global.not-here");
            return;
        }
        CryptoMachine machine = getMachine(player.getUniqueId());
        if (machine == null || !machine.isPlaced()) {
            Messages.send(player, "crypto.not-placed");
            return;
        }

        org.bukkit.Location loc = machine.getLocation();
        if (loc != null && loc.getWorld() != null) {
            loc.getBlock().setType(org.bukkit.Material.AIR);
        }

        machine.removeHologram();
        machine.setLocation(null);
        saveMachine(machine);

        org.bukkit.inventory.ItemStack item = CryptoItemHelper.createMachineItem(plugin);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
            player.sendMessage(Messages.colorize("&aYour &eCrypto Machine &ahas been picked up and dropped on the ground!"));
        } else {
            player.getInventory().addItem(item);
            player.sendMessage(Messages.colorize("&aYour &eCrypto Machine &ahas been picked up and added to your inventory!"));
        }
    }

    public void resetAllMachineLevels(org.bukkit.command.CommandSender sender) {
        for (CryptoMachine m : activeMachines.values()) {
            if (m != null) {
                m.setSpeedLevel(0);
                m.setCapacityLevel(0);
                m.setFuelTimeLevel(0);
                m.setRewardLevel(0);
            }
        }
        dao.resetAllMachineLevels().thenAccept(rows -> {
            sender.sendMessage(Messages.colorize("&aSuccessfully reset Crypto Machine levels to 0 for &e" + rows + " &aplayers/machines in the database!"));
        });
    }
}
