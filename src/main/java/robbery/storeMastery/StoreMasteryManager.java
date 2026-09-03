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
            {10,  25,  50,  75,  100,  150,  250,  450,  800,  1500},  // Stores 1-2
            {25,  75,  150, 300, 500,  750,  1200, 1800, 2800, 4000},  // Stores 3-4
            {50,  150, 300, 500, 750,  1100, 1600, 2500, 4000, 6000},  // Stores 5-6
            {75,  200, 400, 650, 900,  1300, 1900, 3000, 4800, 7500},  // Stores 7-8
            {100, 250, 500, 800, 1100, 1600, 2400, 3700, 5500, 8500},  // Stores 9-10
            {150, 350, 700, 1100,1500, 2200, 3200, 4800, 7000, 10000}  // Stores 11-12
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
            if (newLevel == 2) {
                unlockRegion(player, "store11_tier2");
                Messages.send(player, "events.mastery.store11-tier1");
            }
            if (newLevel == 5) {
                unlockRegion(player, "store11_tier3");
                Messages.send(player, "events.mastery.tier2");
            }
        } else if(storeId.equalsIgnoreCase("store12")){
            if (newLevel == 2) {
                unlockRegion(player, "store12_tier2");
                Messages.send(player, "events.mastery.tier2");
            }
            if (newLevel == 5) {
                assert pd != null;
                if (pd.getPrestige() >= 3) {
                    unlockRegion(player, "store13");
                }
                Messages.send(player, "events.mastery.vault");
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
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rg addmember -w world " + regionId + " " + player.getName());
    }

    public String getRewardDisplay(String storeId, int milestone) {
        String storeName = KeyManager.getStoreN(storeId);
        if (storeName == null) storeName = storeId;

        return switch (milestone) {
            case 1 -> "§a+10% Money Multiplier";
            case 2 -> {
                if (storeId.equalsIgnoreCase("store11")) {
                    yield "§aUnlock middle area of " + storeName + " & +10% Steal Speed";
                } else if (storeId.equalsIgnoreCase("store12")) {
                    yield "§aUnlock higher area of " + storeName + " & +10% Steal Speed";
                } else {
                    yield "§a+10% Steal Speed";
                }
            }
            case 3 -> "§a+5% Robbery XP";
            case 4 -> "§a+1% Skill Point Chance";
            case 5 -> {
                if (storeId.equalsIgnoreCase("store12")) {
                    yield "§aUnlock The Vault";
                } else {
                    yield "§aUnlock higher area of " + storeName;
                }
            }
            case 6 -> "§a+15% Money Multiplier (+25% Total)";
            case 7 -> "§a+15% Steal Speed (+25% Total)";
            case 8 -> "§a+1% Double Item Chance";
            case 9 -> "§a+1% Insta-Steal Chance";
            case 10 -> {
                String tagId = storeTagIds.get(storeId);
                if (tagId != null) {
                    String rawPlaceholder = "%deluxetags_tag_" + tagId + "%";
                    String parsed = PlaceholderAPI.setPlaceholders(null, rawPlaceholder);
                    if (parsed != null && !parsed.isEmpty() && !parsed.equals(rawPlaceholder)) {
                        yield parsed;
                    }
                    yield "§d[" + tagId + "] Vanity Tag";
                }
                yield "§dExclusive Vanity Tag";
            }
            default -> "";
        };
    }
}