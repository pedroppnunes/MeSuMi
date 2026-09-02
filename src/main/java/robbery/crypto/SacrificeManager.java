package robbery.crypto;

import org.bukkit.entity.Player;
import robbery.items.Items;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SacrificeManager {

    private final Map<UUID, Map<String, Integer>> selectedAmounts = new ConcurrentHashMap<>();

    public Map<String, Integer> getSelectedMap(UUID uuid) {
        return selectedAmounts.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public int getSelectedAmount(UUID uuid, String itemId) {
        return getSelectedMap(uuid).getOrDefault(itemId, 0);
    }

    public void setSelectedAmount(UUID uuid, String itemId, int amount) {
        Map<String, Integer> map = getSelectedMap(uuid);
        if (amount <= 0) {
            map.remove(itemId);
        } else {
            map.put(itemId, amount);
        }
    }

    public void addSelectedAmount(UUID uuid, String itemId, int amountToAdd, int available) {
        int current = getSelectedAmount(uuid, itemId);
        int target = Math.min(available, Math.max(0, current + amountToAdd));
        setSelectedAmount(uuid, itemId, target);
    }

    public void clear(UUID uuid) {
        selectedAmounts.remove(uuid);
    }

    public int getAvailableAmountInBackpack(Player player, String itemId) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return 0;
        int count = 0;
        for (Items item : pd.getBackpack().getItems()) {
            if (item != null && item.getId() != null && item.getId().equalsIgnoreCase(itemId)) {
                count++;
            }
        }
        return count;
    }

    public long getTotalSacrificeValue(Player player) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return 0L;

        Map<String, Integer> map = getSelectedMap(player.getUniqueId());
        long total = 0L;

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String itemId = entry.getKey();
            int amount = entry.getValue();
            if (amount <= 0) continue;

            for (Items item : pd.getBackpack().getItems()) {
                if (item != null && item.getId() != null && item.getId().equalsIgnoreCase(itemId)) {
                    total += (long) item.getValue() * amount;
                    break;
                }
            }
        }
        return total;
    }

    public Map<String, Double> getCalculatedChances(Player player) {
        long totalVal = getTotalSacrificeValue(player);
        return getCalculatedChancesByValue(totalVal);
    }

    public static Map<String, Double> getCalculatedChancesByValue(long totalVal) {
        Map<String, Double> map = new HashMap<>();
        
        // Map totalVal to a luckFactor between 0.0 and 1.0 based on economy milestones
        double luckFactor;
        if (totalVal <= 0) {
            luckFactor = 0.0;
        } else if (totalVal <= 1_000_000) {
            // 0 to 1M -> luck 0.0 to 0.1 (Starts introducing Copper/Iron)
            luckFactor = 0.0 + (totalVal / 1_000_000.0) * 0.1;
        } else if (totalVal <= 10_000_000) {
            // 1M to 10M -> luck 0.1 to 0.3 (Strong Copper/Iron, starts introducing Diamond)
            luckFactor = 0.1 + ((totalVal - 1_000_000) / 9_000_000.0) * 0.2;
        } else if (totalVal <= 50_000_000) {
            // 10M to 50M -> luck 0.3 to 0.6 (Strong Diamond, starts introducing Emerald)
            luckFactor = 0.3 + ((totalVal - 10_000_000) / 40_000_000.0) * 0.3;
        } else if (totalVal <= 150_000_000) {
            // 50M to 150M -> luck 0.6 to 1.0 (End game: transitions to mostly Diamond/Emerald)
            luckFactor = 0.6 + ((totalVal - 50_000_000) / 100_000_000.0) * 0.4;
        } else {
            // 150M+ -> max luck (Maxes out at ~138M which is 60 slots of 2.3M items)
            luckFactor = 1.0;
        }

        double coal = 100.0;
        double copper = 0.0;
        double iron = 0.0;
        double gold = 0.0;
        double diamond = 0.0;
        double emerald = 0.0;

        if (luckFactor < 0.2) {
            // 0 to ~100M
            double progress = luckFactor / 0.2; // 0 to 1
            coal = 100.0 - (progress * 80.0); // 100 -> 20
            copper = progress * 60.0; // 0 -> 60
            iron = progress * 15.0; // 0 -> 15
            gold = progress * 5.0; // 0 -> 5
        } else if (luckFactor < 0.5) {
            // ~100M to ~250M
            double progress = (luckFactor - 0.2) / 0.3; // 0 to 1
            coal = 20.0 - (progress * 20.0); // 20 -> 0
            copper = 60.0 - (progress * 40.0); // 60 -> 20
            iron = 15.0 + (progress * 25.0); // 15 -> 40
            gold = 5.0 + (progress * 25.0); // 5 -> 30
            diamond = progress * 10.0; // 0 -> 10
        } else {
            // ~250M to 500M+
            double progress = (luckFactor - 0.5) / 0.5; // 0 to 1
            coal = 0;
            copper = 20.0 - (progress * 20.0); // 20 -> 0
            iron = 40.0 - (progress * 40.0); // 40 -> 0
            gold = 30.0 - (progress * 20.0); // 30 -> 10
            diamond = 10.0 + (progress * 45.0); // 10 -> 55
            emerald = progress * 35.0; // 0 -> 35
        }

        map.put("coal", coal);
        map.put("copper", copper);
        map.put("iron", iron);
        map.put("gold", gold);
        map.put("diamond", diamond);
        map.put("emerald", emerald);
        return map;
    }
}
