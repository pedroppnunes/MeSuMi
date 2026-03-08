package robbery.skilltree;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.BiConsumer;

public class PerkEffectManager {
    private static final Map<String, PerkEffect> registry = new ConcurrentHashMap<>();

    public static void register(String tierId, PerkEffect effect) {
        registry.put(tierId, effect);
    }

    public static PerkEffect get(String tierId) {
        return registry.get(tierId);
    }

    public static void applyEffect(Player player, String tierId, int level) {
        PerkEffect p = registry.get(tierId);
        if (p != null) p.apply(player, level);
    }

    public static void removeEffect(Player player, String tierId) {
        PerkEffect p = registry.get(tierId);
        if (p != null) p.remove(player);
    }
}
