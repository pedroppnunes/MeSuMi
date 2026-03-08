package robbery.skilltree;

import org.bukkit.entity.Player;

public interface PerkEffect {
    /** apply or re-apply effects for player at specified level */
    void apply(Player player, int level);

    /** remove effect (used if you downgrade — optional) */
    void remove(Player player);
}
