package robbery.skilltree;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
public class SkillPerk {
        private final String id;
        private final String name;
        private final int requiredLevel;
        private final int maxLevel;
        private final List<Integer> costs;
        private final String description;

        public SkillPerk(String id, String name, int requiredLevel, int maxLevel, List<Integer> costs, String description) {
            this.id = id;
            this.name = name;
            this.requiredLevel = requiredLevel;
            this.maxLevel = maxLevel;
            this.costs = Collections.unmodifiableList(new ArrayList<>(costs));
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getRequiredLevel() { return requiredLevel; }
        public int getMaxLevel() { return maxLevel; }
        public List<Integer> getCosts() { return costs; }
        public String getDescription() { return description; }
    
        public int costForNext(int currentLevel) {
            if (currentLevel < 0) currentLevel = 0;
            if (currentLevel >= maxLevel) return Integer.MAX_VALUE; // cannot upgrade
            return costs.get(Math.min(currentLevel, costs.size() - 1));
        }
}
