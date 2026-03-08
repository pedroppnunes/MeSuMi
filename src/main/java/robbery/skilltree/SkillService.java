package robbery.skilltree;

// package robbery.skilltree;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

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
        if (current >= tier.getMaxLevel()) return false;
        if (pd.getLevel() < tier.getRequiredLevel()) return false; // robbery level requirement
        int cost = tier.costForNext(current);
        return pd.getSkillPoints() >= cost;
    }

    public boolean upgrade(Player player, String tierId) {
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return false;

        synchronized (player.getUniqueId().toString().intern()) {
            SkillPerk tier = config.getTier(tierId);
            if (tier == null) return false;
            int cur = pd.getSkillTreeLevel(tierId);
            if (cur >= tier.getMaxLevel()) return false;
            if (pd.getLevel() < tier.getRequiredLevel()) return false;
            int cost = tier.costForNext(cur);
            if (pd.getSkillPoints() < cost) return false;

            pd.setSkillTreeLevel(tierId, cur + 1);
            pd.addSkillPoints(-cost);

            // apply effect for the new level (if you have a registry)
            PerkEffectManager.applyEffect(player, tierId, cur + 1);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§aSkill purchased: " + tier.getName() + " §7(§e" + (cur+1) + "§7/§e" + tier.getMaxLevel() + "§7)");
            });
            return true;
        }
    }
}
