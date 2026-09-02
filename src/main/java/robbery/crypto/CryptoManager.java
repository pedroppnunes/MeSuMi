package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

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
                    
                    double qualityMult = machine.getQualityMultiplier();
                    double speedMult = machine.getSpeedMultiplier();
                    double rewardMult = machine.getRewardMultiplier();
                    
                    long moneyGenerated = (long) (activeSeconds * baseRate * qualityMult * speedMult * rewardMult);
                    
                    machine.addUnclaimedMoney(moneyGenerated);
                    machine.setFuelTicks(machine.getFuelTicks() - activeSeconds);
                }
            }
            
            // Update last updated to now so we don't double count
            machine.setLastUpdated(System.currentTimeMillis());
            
            activeMachines.put(player.getUniqueId(), machine);
            
            if (machine.isPlaced()) {
                Bukkit.getScheduler().runTask(plugin, machine::updateHologram);
            }
        });
    }
    
    public void unloadPlayer(Player player) {
        CryptoMachine machine = activeMachines.remove(player.getUniqueId());
        if (machine != null) {
            machine.removeHologram();
            dao.saveMachine(machine);
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
        }
        return m;
    }
    
    public Map<UUID, CryptoMachine> getActiveMachines() {
        return activeMachines;
    }
    
    public void saveAll() {
        for (CryptoMachine machine : activeMachines.values()) {
            dao.saveMachine(machine);
        }
    }

    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, CryptoMachine> entry : activeMachines.entrySet()) {
                CryptoMachine machine = entry.getValue();
                
                // If it has active fuel and is placed
                if (machine.getFuelTicks() > 0 && machine.isPlaced()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) {
                        PlayerData pd = PlayerDataManager.getPlayerData(p);
                        if (pd != null) {
                            int storeTier = pd.getHighestOwnedStoreTier();
                            
                            // Base rate per second
                            long baseRate = getBaseRateForStore(storeTier);
                            
                            // Multipliers
                            double qualityMult = machine.getQualityMultiplier();
                            double speedMult = machine.getSpeedMultiplier();
                            double rewardMult = machine.getRewardMultiplier();
                            
                            double onlineBuff = 1.0;
                            if (p.isOnline() && machine.getFuelTicks() > 0) {
                                onlineBuff = 1.20; // 20% online buff
                            }
                            
                            long moneyGenerated = (long) (baseRate * qualityMult * speedMult * rewardMult * onlineBuff);
                            
                            machine.addUnclaimedMoney(moneyGenerated);
                            machine.setFuelTicks(machine.getFuelTicks() - 1);
                            machine.setLastUpdated(now);
                            machine.updateHologram();
                        }
                    }
                } else if (machine.isPlaced()) {
                    // Just update lastUpdated so it doesn't try to generate money offline when they log in 
                    // if it had no fuel before
                    machine.setLastUpdated(now);
                }
            }
        }, 20L, 20L); // Run every second (20 ticks)
    }
    
    public long getBaseRateForStore(int storeTier) {
        switch (storeTier) {
            case 1: return 1;           // Store 1 (Supermarket)
            case 2: return 1;           // Store 2 (The Griffin's)
            case 3: return 1;           // Store 3 (Gym)
            case 4: return 1;           // Store 4 (Arcade)      
            case 5: return 8;           // Store 5 (School)      
            case 6: return 55;          // Store 6 (Casino)      
            case 7: return 220;         // Store 7 (Oceanarium)  
            case 8: return 820;         // Store 8 (Steakhouse)  
            case 9: return 1350;        // Store 9 (Diamond)     
            case 10: return 2200;       // Store 10 (Balenziaga) 
            case 11: return 4650;       // Store 11 (Samzung)    
            case 12: return 5500;       // Store 12 (The Bank)   
            case 13: return 7300;       // Store 13 (The Vault)  
            default: return 1;
        }
    }
}
