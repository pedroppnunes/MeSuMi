package robbery.storeMastery;

import me.clip.placeholderapi.PlaceholderAPI;
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
            {10, 50, 150, 300, 500, 3000, 4000, 5000, 7500, 10000},    // Stores 1-2
            {150, 300, 600, 1000, 1250, 4250, 5500, 7000, 9000, 12500},   // Stores 3-4
            {200, 400, 800, 1250, 1500, 5000, 6500, 8500, 11000, 15000},  // Stores 5-6
            {300, 750, 1250, 1500, 2000, 7000, 9000, 12500, 15000, 20000}, // Stores 7-8
            {400, 800, 1250, 1750, 2250, 8000, 10000, 14000, 17500, 22500},// Stores 9-10
            {500, 1000, 1500, 2000, 2500, 12500, 15000, 20000, 25000, 30000}// Stores 11-12
    };
    private final Map<String, String> storeTagIds = Map.ofEntries(
            Map.entry("store1", "MarketExpert"),
            Map.entry("store2", "AFamilyguy"),
            Map.entry("store3", "GymRat"),
            Map.entry("store4", "Tetris"),
            Map.entry("store5", "TheNerd"),
            Map.entry("store6", "TrueGambler"),
            Map.entry("store7", "AquaMan"),
            Map.entry("store8", "Cooked"),
            Map.entry("store9", "RareGem"),
            Map.entry("store10", "FashionModel"),
            Map.entry("store11", "TechGenius"),
            Map.entry("store12", "BankHeister")
    );

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
        Messages.sendFormatted(player, "events.mastery.level-up", Map.of("store", store.getName(), "level", String.valueOf(newLevel)));

        if (storeId.equalsIgnoreCase("store11")) {
            if (newLevel == 3) {
                unlockRegion(player, "store11_tier2");
                Messages.send(player, "events.mastery.store11-tier1");
            }
            if (newLevel == 5) {
                unlockRegion(player, "store11_tier3");
                Messages.send(player, "events.mastery.tier2");
            }
        } else if (newLevel == 5) {
            unlockRegion(player, storeId + "_tier2");
            Messages.send(player, "events.mastery.tier2");
        }

        if (newLevel == 10) {
            Messages.sendFormatted(player, "events.mastery.max", "store", store.getName());

            String tagId = storeTagIds.get(storeId);
            if (tagId != null) {
                String permission = "deluxetags.tag." + tagId.toLowerCase();

                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "lp user " + player.getName() + " permission set " + permission + " true"
                );
            }
            boolean allMax = true;
            for (Keys key : KeyManager.getAllStores()) {
                assert pd != null;
                int milestone = pd.getStoreMilestone(key.getId());
                if (milestone < 10) {
                    allMax = false;
                    break;
                }
            }

            if (allMax) {
                assert pd != null;
                if (!pd.hasGodOfRobbery()) {
                    pd.setGodOfRobbery(true);
                    String tag = "deluxetags.tag.godofrobbery";
                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "lp user " + player.getName() + " permission set " + tag + " true"
                    );
                    player.sendTitle(Messages.get("events.secrettag.title"),Messages.get("events.secrettag.subtitle"));
                }
            }
        }
    }

    private void unlockRegion(Player player, String regionId) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rg addmember " + regionId + " " + player.getName());
    }

    public String getRewardDisplay(String storeId, int milestone) {

        if (milestone == 5) {
            return "§aUnlock higher area of " + KeyManager.getStoreN(storeId);
        }

        if (storeId.equalsIgnoreCase("store11") && milestone == 3) {
            return "§aUnlock middle area of " + KeyManager.getStoreN(storeId);
        }

        if (milestone == 10) {
            String tagId = storeTagIds.get(storeId);
            if (tagId != null) {
                String rawPlaceholder = "%deluxetags_tag_" + tagId + "%";
                String parsed = PlaceholderAPI.setPlaceholders(null, rawPlaceholder);
                return parsed;
            }
        }

        return "";
    }
}