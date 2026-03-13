package robbery.skilltree;

// package robbery.skilltree;

import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.Map;

public class SkillService {
    private final SkillTreeConfig config;
    private final Robbery plugin;

    public SkillService(Robbery plugin, SkillTreeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean canUpgrade(Player player, String tierId) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return false;

        SkillPerk tier = config.getTier(tierId);
        if (tier == null) return false;

        int current = pd.getSkillTreeLevel(tierId);
        if (current >= tier.maxLevel()) return false;
        if (pd.getLevel() < tier.requiredLevel()) return false;
        int cost = tier.costForNext(current);
        return pd.getSkillPoints() >= cost;
    }

    public void upgrade(Player player, String tierId) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        synchronized (pd) {

            SkillPerk tier = config.getTier(tierId);
            if (tier == null) return;

            int cur = pd.getSkillTreeLevel(tierId);
            if (cur >= tier.maxLevel()) return;

            if (pd.getLevel() < tier.requiredLevel()) return;

            int cost = tier.costForNext(cur);
            if (pd.getSkillPoints() < cost) return;

            int newLevel = cur + 1;

            pd.setSkillTreeLevel(tierId, newLevel);
            pd.addSkillPoints(-cost);

            double value = tier.valueForLevel(newLevel);
            pd.setPerkValue(tierId, value);

            Messages.sendFormatted(player,
                    "events.skilltree.purchased",
                    Map.of("tiername", tier.name(), "cost", String.valueOf(cost)));

        }
    }
}
