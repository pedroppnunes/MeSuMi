package robbery.skilltree;
import java.util.*;

public record SkillPerk(String id, String name, int requiredLevel, int maxLevel, List<Integer> costs, List<Double> values, String description,List<String> requiredPerks) {
    public SkillPerk(String id, String name, int requiredLevel, int maxLevel, List<Integer> costs, List<Double> values, String description,List<String> requiredPerks) {
        this.id = id;
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.maxLevel = maxLevel;
        this.costs = List.copyOf(costs);
        this.values = List.copyOf(values);
        this.description = description;
        this.requiredPerks = requiredPerks == null ? List.of() : List.copyOf(requiredPerks);
    }

    public int costForNext(int currentLevel) {
        if (currentLevel < 0) currentLevel = 0;
        if (currentLevel >= maxLevel) return Integer.MAX_VALUE; // cannot upgrade
        return costs.get(Math.min(currentLevel, costs.size() - 1));
    }

    public double valueForLevel(int level) {
        if (values == null || values.isEmpty()) return 0;
        int index = Math.min(level - 1, values.size() - 1);
        return values.get(index);
    }


}
