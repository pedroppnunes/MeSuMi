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
            case 4: return 1;           // Store 4 (Arcade)      -> $3,600 / hr (0.72% of School)
            case 5: return 7;           // Store 5 (School)      -> $25,200 / hr (0.84% of Casino)
            case 6: return 45;          // Store 6 (Casino)      -> $162,000 / hr (0.81% of Oceanarium)
            case 7: return 180;         // Store 7 (Oceanarium)  -> $648,000 / hr (0.81% of Steakhouse)
            case 8: return 670;         // Store 8 (Steakhouse)  -> $2.41M / hr (0.80% of Diamond Store)
            case 9: return 1100;        // Store 9 (Diamond)     -> $3.96M / hr (0.79% of Balenziaga)
            case 10: return 1800;       // Store 10 (Balenziaga) -> $6.48M / hr (0.81% of Samzung)
            case 11: return 3800;       // Store 11 (Samzung)    -> $13.68M / hr (0.80% of The Bank)
            case 12: return 4500;       // Store 12 (The Bank)   -> $16.2M / hr (0.95% of The Bank)
            case 13: return 6000;       // Store 13 (The Vault)  -> $21.6M / hr
            default: return 1;
        }
    }
}
