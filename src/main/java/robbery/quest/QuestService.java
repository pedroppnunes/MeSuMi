package robbery.quest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import java.util.*;

import static robbery.attribute.Attribute.PERK_ABILITY_SPQUEST1;
import static robbery.attribute.Attribute.PERK_ABILITY_SPQUEST2;

public class QuestService {
    private final QuestManager questManager;
    private final Robbery main;
    private final Random rng = new Random();

    public QuestService(QuestManager qm, Robbery main){
        this.questManager = qm;
        this.main = main;
        Bukkit.getScheduler().runTaskTimer(main, () -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            int currentDay = cal.get(Calendar.DAY_OF_YEAR);

            for (PlayerData pd : PlayerDataManager.getAllPlayers().values()) {
                if (pd.getLastResetDay() != currentDay) {
                    pick3DailyQuestsFor(pd);
                }
            }
        }, 20L, 20*60*60L);
    }

    /**
     * Pick 3 random offered quests for the player respecting the rule:
     * "player must have that store or one above" — we interpret as:
     * if quest.storeIds contains storeN, player must have highestOwnedStoreTier >= N
     * <p>
     * We try to select 3 eligible quests. If not enough eligible, we fall back to any quests.
     */
    public void pick3DailyQuestsFor(PlayerData pd){
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int currentDay = cal.get(Calendar.DAY_OF_YEAR);
        if (pd.getLastResetDay() == currentDay) {
            if (!pd.getOfferedDailyQuests().isEmpty() || pd.getDailyQuestsCompleted() >= 3) {
                return;
            }
        }

        pd.getAcceptedDailyQuests().clear();
        pd.getQuestProgressMap().clear();
        pd.setDailyQuestsCompleted(0);
        pd.getOfferedDailyQuests().clear();

        List<Quest> all = new ArrayList<>(questManager.getAllQuests());
        if (all.isEmpty()) {
            Bukkit.getLogger().warning("[Robbery] No quests found in QuestManager! Check quests.yml.");
            return;
        }
        Collections.shuffle(all, rng);

        int playerMaxTier = pd.getHighestOwnedStoreTier();
        List<String> chosen = new ArrayList<>(3);

        for (Quest q : all){
            if (chosen.size() >= 3) break;
            if (q.isStoreSpecific()){
                boolean eligible = false;
                for (String sid : q.storeIds){
                    int req = parseStoreTier(sid);
                    if (playerMaxTier >= req) { eligible = true; break; }
                }
                if (!eligible) continue;
            }
            chosen.add(q.id);
        }

        if (chosen.size() < 3){
            for (Quest q : all){
                if (chosen.size() >= 3) break;
                if (chosen.contains(q.id)) continue;
                chosen.add(q.id);
            }
        }


        pd.setOfferedDailyQuests(chosen);
        pd.setLastResetDay(currentDay);
    }

    /** Player accepts a quest (call from command /menu button) */
    public void playerAcceptQuest(PlayerData pd, String questId){
        if (pd.getDailyQuestsCompleted() >= 3 || !pd.getOfferedDailyQuests().contains(questId) || pd.getAcceptedDailyQuests().contains(questId)) {
            return;
        }

        pd.acceptDailyQuest(questId);
    }
    private void progressQuest(PlayerData pd, Quest.QuestType type, int amount) {
        for (String questId : new ArrayList<>(pd.getAcceptedDailyQuests())) {
            Quest q = questManager.getQuest(questId);
            if (q == null || q.type != type) continue;

            QuestProgress pr = pd.getQuestProgressMap().computeIfAbsent(questId, QuestProgress::new);
            if (pr.completed) continue;

            pr.incrementBy(amount);

            if (pr.isCompleted(q.itemsRequired)) {
                pr.markCompleted();
                pd.incrementDailyQuestsCompleted();
                giveHalfReward(pd, q, pr);
                if (areAllThreeAcceptedCompleted(pd)) {
                    giveRemainingRewardsForAllThree(pd);
                }
            }
        }
    }

    /**
     * Called when player steals an item.
     * Provide the storeId of the stolen item; amount=1 for each item or aggregate.
     */
    public void onPlayerStealItem(PlayerData pd, String storeId, int amount) {
        for (String questId : new ArrayList<>(pd.getAcceptedDailyQuests())) {
            Quest q = questManager.getQuest(questId);
            QuestProgress qp = pd.getQuestProgressMap().get(questId);
            if (q == null || q.type != Quest.QuestType.STEAL ||(qp != null && qp.completed)) continue;

            boolean counts = !q.isStoreSpecific() || q.storeIds.stream().anyMatch(sid -> sid.equalsIgnoreCase(storeId));
            if (counts) {
                progressQuest(pd, Quest.QuestType.STEAL, amount);
            }
        }
    }

    public void onPlayerBusted(PlayerData pd) {
        progressQuest(pd, Quest.QuestType.BUSTED, 1);
    }

    public void onPlayerCaptureOutpost(PlayerData pd) {
        progressQuest(pd, Quest.QuestType.OUTPOST, 1);
    }

    private void giveHalfReward(PlayerData pd, Quest q, QuestProgress pr){
        Player p  = pd.getPlayer();
        int xpPerItem = computeQuestXpPerItem(q, pd);
        long totalXp = (long) xpPerItem * q.itemsRequired;
        long halfXp = totalXp / 2L;

        int storeItemAmount = (q.rewardStoreItems != null) ? q.rewardStoreItems.amount : 0;
        int halfItems = storeItemAmount / 2;

        if (totalXp > 0) {
            main.getXpManager().addXP(p,halfXp);
        }
        if (storeItemAmount > 0){
            pd.addStoreItems(q.rewardStoreItems.store,halfItems);
        }
        pr.setHalfRewardGiven(true);
        xpMessage(pd, p, totalXp);
        Messages.sendFormatted(p,"events.quests.quest-reward","quest",q.name);
    }

    private void xpMessage(PlayerData pd, Player p, long totalXp) {
        long playerXP = pd.getXp();
        int level = pd.getLevel();
        long xpNeeded = main.getXpManager().xpNext(level);
        long xpRemaining = main.getXpManager().xpRemainingForNextLevel(playerXP, level);
        long xpIntoLevel = xpNeeded - xpRemaining;
        p.sendActionBar(
                Component.text("+" + NumberFormatter.formatDoubleNumber(totalXp) + " Robbery XP (" + NumberFormatter.formatDoubleNumber(xpIntoLevel) + "/" + NumberFormatter.formatDoubleNumber(xpNeeded) + " XP)")
                        .color(NamedTextColor.DARK_AQUA));
    }

    private boolean areAllThreeAcceptedCompleted(PlayerData pd){
        int accepted = pd.getAcceptedDailyQuests().size();
        if (accepted == 0) return false;
        int completed = 0;
        for (String qid : pd.getAcceptedDailyQuests()){
            QuestProgress pr = pd.getQuestProgressMap().get(qid);
            if (pr != null && pr.isCompleted(questManager.getQuest(qid).itemsRequired)) completed++;
        }
        return completed > 0 && completed == accepted;
    }

    private void giveRemainingRewardsForAllThree(PlayerData pd){
        List<String> toRemove = new ArrayList<>(pd.getAcceptedDailyQuests());
        long totalXpToGive = 0;
        for (String qid : toRemove){
            Quest q = questManager.getQuest(qid);
            QuestProgress pr = pd.getQuestProgressMap().get(qid);
            if (q == null || pr == null) continue;
            int xpPerItem = computeQuestXpPerItem(q, pd);
            long totalXp = (long) xpPerItem * q.itemsRequired;
            long remainder = totalXp;
            if (pr.isHalfRewardGiven()) remainder = totalXp - (totalXp / 2L);

            if (remainder > 0) main.getXpManager().addXP(pd.getPlayer(),remainder);
            totalXpToGive += remainder;

            if (q.rewardStoreItems != null){
                int total = q.rewardStoreItems.amount;
                int already = (pr.isHalfRewardGiven() ? total/2 : 0);
                int give = total - already;
                if (give > 0) pd.addStoreItems(q.rewardStoreItems.store,give);
            }
            pr.markCompleted();
        }
        if (pd.getPerkValue(PERK_ABILITY_SPQUEST1) > 0){
            pd.addSkillPoints(1);
        }
        if(pd.getPerkValue(PERK_ABILITY_SPQUEST2) > 0){
            pd.addSkillPoints(2);
        }
        Player p  = pd.getPlayer();
        if(totalXpToGive > 0)
            xpMessage(pd, p, totalXpToGive);
        pd.setDailyQuestsCompleted(3);
        Messages.send(pd.getPlayer(),"events.quests.all-completed");
    }

    /**
     * computeQuestXpPerItem: chooses the XP-per-item used for quest-level calculation.
     * Strategy: take the highest store XP among quest.storeIds (if any).
     */
    public int computeQuestXpPerItem(Quest q, PlayerData pd) {
        if (q.type == Quest.QuestType.BUSTED) {
            return 1000;
        } else if(q.type == Quest.QuestType.OUTPOST){
            return 25000;
        }

        if (!q.isStoreSpecific()) {
            int tier = pd.getHighestOwnedStoreTier();
            return questManager.getStoreXp("store" + Math.max(1, tier));
        }

        int best = 0;
        for (String s : q.storeIds) best = Math.max(best, questManager.getStoreXp(s));
        return Math.max(best, questManager.getStoreXp("store1"));
    }

    private int parseStoreTier(String storeId){
        if (storeId == null) return 0;
        String numeric = storeId.toLowerCase().replace("store", "").trim();
        try { return Integer.parseInt(numeric); }
        catch (NumberFormatException e){ return 0; }
    }
}
