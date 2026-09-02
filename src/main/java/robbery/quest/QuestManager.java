package robbery.quest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.core.Robbery; // Assuming this is your main class path

import java.io.File;
import java.util.*;

public class QuestManager {
    private final Map<String, Quest> questMap = new HashMap<>();
    private final Map<String, Integer> storeXp = new HashMap<>();
    private final Robbery main;

    public QuestManager(Robbery main) {
        this.main = main;
        setupDefaultXp();
    }

    /**
     * Hardcoded XP values for each store level.
     */
    private void setupDefaultXp() {
        storeXp.put("store1", 20);
        storeXp.put("store2", 50);
        storeXp.put("store3", 80);
        storeXp.put("store4", 160);
        storeXp.put("store5", 220);
        storeXp.put("store6", 300);
        storeXp.put("store7", 350);
        storeXp.put("store8", 375);
        storeXp.put("store9", 400);
        storeXp.put("store10", 425);
        storeXp.put("store11", 450);
        storeXp.put("store12", 500);
        storeXp.put("store13", 550);
    }

    public void loadFromConfig(String fileName) {
        // Points to /plugins/Robbery/quests.yml correctly
        File file = new File(main.getDataFolder(), fileName);
        if (!file.exists()) {
            main.saveResource(fileName, false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        questMap.clear();

        List<Map<?, ?>> raw = cfg.getMapList("quests");
        for (Map<?, ?> m : raw) {
            String id = (String) m.get("id");
            String name = (String) m.get("name");

            // Safe parsing for the item count
            int items = (m.get("items") instanceof Number n) ? n.intValue() : 0;

            // Parse stores
            List<String> stores = new ArrayList<>();
            if (m.get("storeid") instanceof List<?> list) {
                for (Object o : list) stores.add(String.valueOf(o));
            }

            // Parse rewards
            RewardStoreItems rsi = null;
            if (m.get("reward_store_items") instanceof Map<?, ?> r) {
                String store = String.valueOf(r.get("store"));
                int amt = (r.get("amount") instanceof Number n) ? n.intValue() : 0;
                rsi = new RewardStoreItems(store, amt);
            }
            String description = (String) m.get("description");
            String typeStr = (String) m.get("type");
            if(typeStr == null) typeStr = "steal";
            Quest.QuestType type = Quest.QuestType.valueOf(typeStr.toUpperCase());

            questMap.put(id, new Quest(id, name, items, stores, rsi, description, 0,type));
        }
    }

    public Collection<Quest> getAllQuests() { return questMap.values(); }
    public Quest getQuest(String id) { return questMap.get(id); }

    /** Returns XP value for a store id (fallback 30) */
    public int getStoreXp(String storeId) {
        return storeXp.getOrDefault(storeId, 30);
    }
}