package robbery.storeMastery;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

import static robbery.keys.KeyManager.STORE1;

public class StoreMasteryManager {
    private final Robbery main;

    private final int[][] masteryCurves = {
            {10, 50, 100, 150, 200, 300, 400, 500, 750, 1000},    // Stores 1-2
            {15, 100, 175, 225, 300, 425, 550, 700, 900, 1250},   // Stores 3-4
            {20, 125, 200, 275, 350, 500, 650, 850, 1100, 1500},  // Stores 5-6
            {25, 150, 250, 350, 450, 700, 900, 1250, 1500, 2000}, // Stores 7-8
            {30, 175, 300, 450, 550, 800, 1000, 1400, 1750, 2250},// Stores 9-10
            {35, 200, 425, 575, 750, 1250, 1500, 2000, 2500, 3000}// Stores 11-12
    };

    public StoreMasteryManager(Robbery plugin) {
        this.main = plugin;
    }

    private int getCurveIndex(int order) {
        if (order <= 2) return 0;
        if (order <= 4) return 1;
        if (order <= 6) return 2;
        if (order <= 8) return 3;
        if (order <= 10) return 4;
        return 5;
    }

    public int getItemsRequiredForLevel(String storeId, int targetLevel) {
        if (targetLevel <= 0) return 0;
        if (targetLevel > 10) targetLevel = 10;
        Keys store = KeyManager.getStoreName(storeId);
        if (store == null) return 0;
        int curveIndex = getCurveIndex(store.getOrder());
        return masteryCurves[curveIndex][targetLevel - 1]; // milestone 0 = index 0
    }

    public void incrementMastery(Player player, String storeId) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;
        Keys store = KeyManager.getStoreName(storeId);
        if (store == null) store = STORE1;

        pd.addStoreItems(storeId, 1);
        int currentItems = pd.getStoreItems(storeId);
        int oldLevel = getLevelFromItems(storeId, currentItems - 1);
        int newLevel = getLevelFromItems(storeId, currentItems);

        if (newLevel > oldLevel) {
            pd.setStoreMilestone(storeId, newLevel); // update milestone level
            handleLevelUp(player, store, newLevel);
        }
    }

    public int getNextStoreMilestone(PlayerData pd, String storeId) {
        int currentItems = pd.getStoreItems(storeId);
        int nextMilestone = 1;
        while(nextMilestone <= 10 && currentItems >= main.getMasteryManager().getItemsRequiredForLevel(storeId, nextMilestone)) {
            nextMilestone++;
        }
        return nextMilestone;
    }

    public int getLevelFromItems(String storeId, int totalItems) {
        Keys store = KeyManager.getStoreName(storeId);
        if (store == null) return 0;
        int curveIndex = getCurveIndex(store.getOrder());
        int[] milestones = masteryCurves[curveIndex];
        for (int i = milestones.length - 1; i >= 0; i--) {
            if (totalItems >= milestones[i]) return i + 1;
        }
        return 0;
    }

    private void handleLevelUp(Player player, Keys store, int newLevel) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        String storeId = store.getId();
        if (pd != null) pd.setStoreMilestone(store.getId(), newLevel);
        Messages.sendFormatted(player, "events.mastery.level-up", Map.of("store", store.getColorname(), "level", String.valueOf(newLevel)));

        if (storeId.equalsIgnoreCase("store11")) {
            if (newLevel == 2) {
                unlockRegion(player, "store11_tier2");
                Messages.send(player, "events.mastery.store11-tier1");
            }
            if (newLevel == 4) {
                unlockRegion(player, "store11_tier3");
                Messages.send(player, "events.mastery.tier2");
            }
        } else if (newLevel == 4) {
            unlockRegion(player, storeId + "_tier2");
            Messages.send(player, "events.mastery.tier2");
        }

        if (newLevel == 10) {
            Messages.sendFormatted(player, "events.mastery.max", "store", store.getName());
        }

        //Apply Rewards TODO
    }

    private void unlockRegion(Player player, String regionId) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rg addmember " + regionId + " " + player.getName());
    }

    public String getRewardDisplay(String storeId, int i) {
        //TODO
        return "";
    }
}