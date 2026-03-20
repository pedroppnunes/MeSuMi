package robbery.skilltree;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import robbery.core.Robbery;
import robbery.player.PlayerData;

import java.io.File;
import java.util.*;

public class SkillTreeConfig {
    private final Robbery main;
    private final Map<String, SkillPerk> tiers = new LinkedHashMap<>();
    private final File skillTreeFile;
    private FileConfiguration rewardsCfg;

    public SkillTreeConfig(Robbery plugin) {
        this.main = plugin;
        this.skillTreeFile = new File(plugin.getDataFolder(), "skilltree.yml");
        if (!skillTreeFile.exists()) {
            plugin.saveResource("skilltree.yml", false);
        }
        load();
    }

    public void load() {
        this.rewardsCfg = YamlConfiguration.loadConfiguration(skillTreeFile);
        tiers.clear();

        List<Map<String, Object>> raw = (List<Map<String, Object>>) rewardsCfg.getList("perks");

        assert raw != null;
        for (Map<String, Object> m : raw) {
            String id = String.valueOf(m.get("id"));
            String name = String.valueOf(m.getOrDefault("name", id));

            int required = getInt(m.getOrDefault("required_level", 1));
            int maxLevel = getInt(m.getOrDefault("max_level", 1));
            String desc = String.valueOf(m.getOrDefault("description", ""));

            List<Integer> costs = new ArrayList<>();
            Object costsObj = m.get("costs");

            List<Double> values = new ArrayList<>();
            Object valuesObj = m.get("values");

            List<String> requiredPerks = new ArrayList<>();
            Object reqObj = m.get("requiredPerks");

            if (reqObj instanceof List) {
                for (Object o : (List<?>) reqObj) {
                    requiredPerks.add(String.valueOf(o));
                }
            }
            if(valuesObj instanceof List){
                for(Object o : (List<?>) valuesObj)
                    values.add(getDouble(o));
            } else {
                double base = getDouble(m.getOrDefault("base_value", 1));
                double inc = getDouble(m.getOrDefault("value_increment", 0));
                for (int i = 0; i < maxLevel; i++) {
                    values.add(base + (inc * i));
                }
            }
            if (costsObj instanceof List) {
                for (Object o : (List<?>) costsObj) {
                    costs.add(getInt(o));
                }
            } else {
                int base = getInt(m.getOrDefault("base_cost", 1));
                int inc = getInt(m.getOrDefault("increment", 0));
                for (int i = 0; i < maxLevel; i++) {
                    costs.add(base + (inc * i));
                }
            }

            while (costs.size() < maxLevel && !costs.isEmpty()) {
                costs.add(costs.getLast());
            }

            tiers.put(id, new SkillPerk(id, name, required, maxLevel, costs, values, desc, requiredPerks));
        }
    }

    /**
     * Calculates how many skill points a player should get back.
     */
    public int calculateTotalRefund(PlayerData pd) {
        int totalRefund = 0;
        Map<String, Integer> levels = pd.getAllSkillTreeLevels();

        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            String perkId = entry.getKey();
            int currentLevel = entry.getValue();
            if (currentLevel <= 0) continue;

            SkillPerk perk = getTier(perkId); // Uses the method already in SkillTreeConfig
            if (perk == null) continue;

            for (int lvl = 0; lvl < currentLevel; lvl++) {
                try {
                    totalRefund += perk.costForNext(lvl);
                } catch (Exception ignored) {}
            }
        }
        return totalRefund;
    }

    /**
     * Resets all perks to level 0 and updates the player's attribute values.
     */
    public void performReset(PlayerData pd) {
        Map<String, Integer> levels = new HashMap<>(pd.getAllSkillTreeLevels());

        for (String perkId : levels.keySet()) {
            pd.setSkillTreeLevel(perkId, 0);
            SkillPerk perk = getTier(perkId);

            try {
                double defaultValue = (perk != null) ? perk.valueForLevel(0) : 0.0;
                pd.setPerkValue(perkId, defaultValue);
            } catch (Exception ignored) {
                pd.setPerkValue(perkId, 0.0);
            }
        }
    }

    public Collection<SkillPerk> getTiers() { return tiers.values(); }
    public SkillPerk getTier(String id) { return tiers.get(id); }
    private int getInt(Object o) {
        return (o instanceof Number) ? ((Number) o).intValue() : 0;
    }
    private double getDouble(Object o){
        return (o instanceof Number) ? ((Number) o).doubleValue() : 0;
    }
}
