package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.messages.Messages;

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
            
            // Offline progress calculation
            if (pd != null && machine.getFuelTicks() > 0 && machine.getLastUpdated() > 0) {
                long now = System.currentTimeMillis();
                long secondsPassed = (now - machine.getLastUpdated()) / 1000L;
                
                if (secondsPassed > 0) {
                    long activeSeconds = Math.min(secondsPassed, machine.getFuelTicks());
                    
                    int storeTier = pd.getHighestOwnedStoreTier();
                    long baseRate = getBaseRateForStore(storeTier);
                    double prestigeMult = 1.0 + (pd.getPrestige() * 0.10);

                    double qualityMult = machine.getQualityMultiplier();
                    double speedMult = machine.getSpeedMultiplier();
                    double rewardMult = machine.getRewardMultiplier();

                    long moneyGenerated = (long) (activeSeconds * baseRate * prestigeMult * qualityMult * speedMult * rewardMult);
                    
                    machine.addUnclaimedMoney(moneyGenerated);
                    machine.setFuelTicks(machine.getFuelTicks() - activeSeconds);
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
                if (existing.getUnclaimedMoney() > machine.getUnclaimedMoney()) machine.setUnclaimedMoney(existing.getUnclaimedMoney());
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

                if (machine.getFuelTicks() > 0 && machine.isPlaced()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    int storeTier = 1;
                    int prestige = 0;
                    if (p != null && p.isOnline()) {
                        PlayerData pd = PlayerDataManager.getPlayerData(p);
                        if (pd != null) {
                            storeTier = pd.getHighestOwnedStoreTier();
                            prestige = pd.getPrestige();
                        }
                    }

                    long baseRate = getBaseRateForStore(storeTier);
                    double prestigeMult = 1.0 + (prestige * 0.10);
                    double qualityMult = machine.getQualityMultiplier();
                    double speedMult = machine.getSpeedMultiplier();
                    double rewardMult = machine.getRewardMultiplier();
                    double onlineBuff = (p != null && p.isOnline()) ? 1.20 : 1.0;

                    long moneyGenerated = (long) Math.max(1, baseRate * prestigeMult * qualityMult * speedMult * rewardMult * onlineBuff);

                    machine.addUnclaimedMoney(moneyGenerated);
                    machine.setFuelTicks(machine.getFuelTicks() - 1);
                    machine.setLastUpdated(now);

                    // Update hologram if still placed
                    machine.updateHologram();

                    // Notify player if battery just depleted
                    if (machine.getFuelTicks() <= 0 && p != null && p.isOnline()) {
                        Messages.send(p, "crypto.battery-stopped");
                    }
                } else if (machine.isPlaced()) {
                    machine.setLastUpdated(now);
                }

                if (autoSaveTime) {
                    dao.saveMachine(machine);
                }
            }
        }, 20L, 20L); // Run every second
    }
    
    public long getBaseRateForStore(int storeTier) {
        return CryptoUpgradeManager.getStoreBaseRate(storeTier);
    }
}
