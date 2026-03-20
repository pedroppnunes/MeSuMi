package robbery.quest;

import robbery.player.PlayerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static robbery.attribute.Attribute.PERK_ABILITY_SPQUEST1;
import static robbery.attribute.Attribute.PERK_ABILITY_SPQUEST2;

public class Quest {
    public enum QuestType { STEAL, BUSTED, OUTPOST }
    public final String id;
    public final String name;
    public final int itemsRequired;
    public final List<String> storeIds;
    public final RewardStoreItems rewardStoreItems;
    public final String description;
    public final int rewardSkillpoints;
    public final QuestType type;

    public Quest(String id, String name, int itemsRequired, List<String> storeIds, RewardStoreItems rewardStoreItems, String description, int rewardSkillpoints, QuestType type) {
        this.id = id;
        this.name = name;
        this.itemsRequired = itemsRequired;
        this.storeIds = List.copyOf(storeIds);
        this.rewardStoreItems = rewardStoreItems;
        this.description = description;
        this.rewardSkillpoints = rewardSkillpoints;
        this.type = type;
    }

    public boolean isStoreSpecific(){ return storeIds != null && !storeIds.isEmpty(); }

    public int getSkillPointRewards(PlayerData pd) {
        int sp = 0;
        if(pd.getPerkValue(PERK_ABILITY_SPQUEST1) > 0)
            sp += 1;
        if(pd.getPerkValue(PERK_ABILITY_SPQUEST2) > 0)
            sp += 2;
        return sp;
    }
}

