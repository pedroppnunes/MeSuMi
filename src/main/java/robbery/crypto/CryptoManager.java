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
        dao.loadMachine(player.getUniqueId()).thenAccept(machine -> {
            
            PlayerData pd = PlayerDataManager.getPlayerData(player);
            
            // Offline progress calculation (item stealing batches)
            if (pd != null && machine.getFuelTicks() > 0 && machine.getLastUpdated() > 0) {
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
            
            // Update last updated to now so we don't double count
            machine.setLastUpdated(System.currentTimeMillis());
            
            // Preserve existing higher levels/progress if machine was already in memory
            CryptoMachine existing = activeMachines.get(player.getUniqueId());
            if (existing != null) {
                if (existing.getSpeedLevel() > machine.getSpeedLevel()) machine.setSpeedLevel(existing.getSpeedLevel());
                if (existing.getFuelTimeLevel() > machine.getFuelTimeLevel()) machine.setFuelTimeLevel(existing.getFuelTimeLevel());
                if (existing.getRewardLevel() > machine.getRewardLevel()) machine.setRewardLevel(existing.getRewardLevel());
                if (existing.getUnclaimedMoneyDouble() > machine.getUnclaimedMoneyDouble()) machine.setUnclaimedMoney(existing.getUnclaimedMoneyDouble());
                if (existing.getStoredFuels().size() > machine.getStoredFuels().size()) {
                    machine.getStoredFuels().clear();
                    machine.getStoredFuels().addAll(existing.getStoredFuels());
                }
            }

            activeMachines.put(player.getUniqueId(), machine);
            dao.saveMachine(machine);
            
            if (machine.isPlaced()) {
                Bukkit.getScheduler().runTask(plugin, machine::updateHologram);
            }
        });
    }
    
    public void unloadPlayer(Player player) {
        CryptoMachine machine = activeMachines.remove(player.getUniqueId());
        if (machine != null) {
            machine.removeHologram();
            dao.saveMachineSync(machine);
        }
    }
    
    public CryptoMachine getMachine(UUID uuid) {
        return activeMachines.get(uuid);
    }

    public CryptoMachine getOrCreateMachine(UUID uuid) {
        CryptoMachine m = activeMachines.get(uuid);
        if (m == null) {
            m = new CryptoMachine(uuid, null, null, null, null, 0L, 0L, 0.0, 0, 0, 0, System.currentTimeMillis());
            activeMachines.put(uuid, m);
            dao.saveMachine(m);
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

    public long getBaseRateForStore(int storeTier) {
        return CryptoUpgradeManager.getStoreBaseRate(storeTier);
    }
}
