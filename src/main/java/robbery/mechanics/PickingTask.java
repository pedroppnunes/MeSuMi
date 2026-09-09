package robbery.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import robbery.core.Robbery;
import robbery.booster.Booster;
import robbery.booster.BoosterManager;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.storeMastery.StoreMasteryManager;
import robbery.tool.Tools;

import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import robbery.notifications.NotificationType;

import static robbery.attribute.Attribute.*;

public class PickingTask extends BukkitRunnable {
    private final Player player;
    private final Items item;
    private final ArmorStand stand;
    private final Tools tool;
    private final Robbery main;
    private final Random random = new Random();
    private final Runnable onFinish;

    private boolean isFirstTick = true;

    public PickingTask(Player player, Items item, ArmorStand stand, Tools tool, Robbery main, Runnable onFinish) {
        this.player = player;
        this.item = item;
        this.stand = stand;
        this.tool = tool;
        this.main = main;
        this.onFinish = onFinish;
    }

    @Override
    public void run() {
        PlayerData p = PlayerDataManager.getPlayerData(player);
        if (item.getHp() <= 0) {
            this.cancel();
            item.resetspawn(item.getTime());

            int itemStoreNum = extractStoreNumber(item.getId());
            if (itemStoreNum > 11) itemStoreNum = 12;
            String storeId = getStoreId(itemStoreNum);

            // --- M8: Mastery Double Item Chance (store-specific) ---
            double masteryDoubleChance = p.getStoreMasteryDoubleItemChance(storeId);
            double masteryDoubleProb = masteryDoubleChance >= 0.1 ? masteryDoubleChance / 100.0 : masteryDoubleChance;

            double effectiveBoost = p.getBoost(storeId);
            p.addItemToBackpack(item, storeId);

            String itemName = item.getName().substring(0,1).toUpperCase() + item.getName().substring(1);
            double baseValue = item.getValue();
            double totalValue = baseValue * effectiveBoost;
            double bonus = totalValue - baseValue;
            if (effectiveBoost != 1.0) {
                Messages.sendActionBarFormatted(player, "events.picking.item_stolen_with_boost", Map.of(
                        "item", itemName,
                        "value", NumberFormatter.formatDoubleNumber(baseValue) + "$",
                        "bonus", NumberFormatter.formatDoubleNumber(bonus) + "$"
                ));
            } else {
                Messages.sendActionBarFormatted(player, "events.picking.item_stolen", Map.of(
                        "item", itemName,
                        "value", NumberFormatter.formatDoubleNumber(baseValue) + "$"
                ));
            }

            double doubleChance = p.getPerkValue(PERK_DOUBLE_ITEM1);
            double tripleChance = p.getPerkValue(PERK_TRIPLE_ITEM1);

            double tripleProb = tripleChance / 100.0;
            double doubleProb = doubleChance / 100.0;

            if (!p.getBackpack().isFull()) {
                if (tripleProb > 0 && random.nextDouble() < tripleProb) {
                    p.addItemToBackpack(item, storeId);
                    p.addItemToBackpack(item, storeId);
                    Messages.sendActionBarFormatted(player, "events.picking.triple_item", Map.of(
                            "item", itemName,
                            "value", NumberFormatter.formatDoubleNumber(baseValue * 2) + "$"
                    ));
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.sendFormatted(player, "events.picking.triple_item_chat", Map.of("item", itemName));
                    }
                } else if (doubleProb > 0 && random.nextDouble() < doubleProb) {
                    p.addItemToBackpack(item, storeId);
                    Messages.sendActionBarFormatted(player, "events.picking.double_item", Map.of(
                            "item", itemName,
                            "value", NumberFormatter.formatDoubleNumber(baseValue) + "$"
                    ));
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.sendFormatted(player, "events.picking.double_item_chat", Map.of("item", itemName));
                    }
                } else if (masteryDoubleProb > 0 && random.nextDouble() < masteryDoubleProb) {
                    p.addItemToBackpack(item, storeId);
                    Messages.sendActionBarFormatted(player, "events.picking.double_item", Map.of(
                            "item", itemName,
                            "value", NumberFormatter.formatDoubleNumber(baseValue) + "$"
                    ));
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.sendFormatted(player, "events.picking.double_item_chat", Map.of("item", itemName));
                    }
                }
            }

            main.getMasteryManager().incrementMastery(player, storeId);
            p.addItemsStolen(1);
            p.incrementItemStolenCount(item.getId(), storeId);

            Booster booster = BoosterManager.getRandomBoosterWithChance(itemStoreNum, p);
            if (booster != null) {
                if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                    Messages.sendFormatted(player, "events.picking.booster_reward", Map.of("booster", booster.getName()));
                }
                p.addBoosters(booster);
            }

            // --- Skill Point chance: base 0.1% (0.001) scaled multiplicatively by perk %, outpost %, store mastery % ---
            double spPerk = p.getPerkValue(PERK_CHANCE_SP1);
            double outSp = p.getOutSpChance();
            double masterySp = p.getStoreMasterySkillPointChance(storeId);

            double bonusMultiplier = 1.0 + (spPerk + outSp + masterySp) / 100.0;
            double baseSpProbability = 0.001; // 0.1% base (1 in 1000)
            double spProbability = Math.min(1.0, baseSpProbability * bonusMultiplier);

            if (random.nextDouble() < spProbability) {
                player.sendTitle(
                        Messages.get("events.picking.skillpoint_reward_title"),
                        Messages.get("events.picking.skillpoint_reward_subtitle"),
                        10, 60, 10
                );
                if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                    Messages.send(player, "events.picking.skillpoint_reward_chat");
                }
                p.addSkillPoints(1);
            }

            double abilityChance = p.getPerkValue(PERK_ABILITY_MONEYMULT1);
            if (abilityChance > 0) {
                if (random.nextDouble() < 0.05) {
                    p.setTemporaryPerk(PERK_ABILITY_MONEYMULT1, 10.0);
                    Messages.sendActionBar(player, "events.picking.boost_ability_proc");
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.send(player, "events.picking.boost_ability_proc_chat");
                    }
                }
            }

            double stealSpeedChance = p.getPerkValue(PERK_ABILITY_STEALSPEED1);
            if (stealSpeedChance > 0) {
                if (random.nextDouble() < 0.05) {
                    p.setTemporaryPerk(PERK_ABILITY_STEALSPEED1, 10.0);
                    Messages.sendActionBar(player, "events.picking.stealspeed_proc");
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.send(player, "events.picking.stealspeed_proc_chat");
                    }
                }
            }
            double streakIncrement = p.getPerkValue(PERK_ITEM_STREAK1);
            if (streakIncrement > 0) {
                p.addItemStreak(streakIncrement);
            }

            double keyChance = p.getPerkValue(PERK_SPECIAL_KEYCHANCE);
            double keyProb = keyChance / 100.0;
            if (keyProb > 0 && random.nextDouble() < keyProb) {
                double keyRoll = random.nextDouble() * 100.0;
                String keyType;
                if (keyRoll < 55.0) {
                    keyType = "vote";
                } else if (keyRoll < 80.0) {
                    keyType = "booster";
                } else if (keyRoll < 92.0) {
                    keyType = "epic";
                } else if (keyRoll < 98.0) {
                    keyType = "legendary";
                } else {
                    keyType = "tags";
                }

                player.sendTitle(
                        Messages.get("events.picking.keychance_title"),
                        Messages.get("events.picking.keychance_subtitle"),
                        10, 60, 10
                );
                if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                    Messages.send(player, "events.picking.keychance_chat");
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate key give " + player.getName() + " " + keyType + " 1");
            }

            main.getQuestService().onPlayerStealItem(p, storeId, 1);
            if (onFinish != null) onFinish.run();
            return;
        }

        if (!player.isOnline() || !isLookingAtStand(player, stand)) {
            this.cancel();
            item.setHp(item.getInitialhp());
            item.togglePickable();
            Messages.sendActionBar(player, "events.picking.canceled");
            if (onFinish != null) onFinish.run();
            return;
        }

        int itemStoreNumPick = extractStoreNumber(item.getId());
        if (itemStoreNumPick > 11) itemStoreNumPick = 12;
        String storeIdPick = getStoreId(itemStoreNumPick);

        // --- Check Insta-Steal ONLY on the first touch (first tick), AND only once per player per item spawn cycle ---
        if (isFirstTick) {
            isFirstTick = false;
            java.util.UUID playerUuid = player.getUniqueId();
            if (!item.hasAttemptedInstaSteal(playerUuid)) {
                item.recordInstaStealAttempt(playerUuid);

                double masteryInstaSteal = p.getStoreMasteryInstaStealChance(storeIdPick);
                double instaSteal = p.getPerkValue(PERK_INSTA_STEAL1);

                double instaProb = instaSteal / 100.0;
                double masteryInstaProb = masteryInstaSteal;

                if ((instaProb > 0 && random.nextDouble() < instaProb) ||
                        (masteryInstaProb > 0 && random.nextDouble() < masteryInstaProb)) {
                    item.setHp(0);
                    sendProgressBar(player, item.getHp(), item.getInitialhp());
                    if (p.isNotificationEnabled(NotificationType.ABILITY_PROCS)) {
                        Messages.send(player, "events.picking.insta_steal_chat");
                    }
                    return;
                }
            }
        }

        item.setHp(item.getHp() - (tool.getDamage() * (1.0 + p.getExtraDamage(storeIdPick) / 100.0)));
        sendProgressBar(player, item.getHp(), item.getInitialhp());

    }

    private boolean isLookingAtStand(Player player, ArmorStand stand) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        double maxDistance = 2.2;

        for (double distance = 0; distance <= maxDistance; distance += 0.1) {
            Location currentLocation = eyeLocation.clone().add(direction.clone().multiply(distance));

            if (currentLocation.distance(stand.getLocation()) <= 1.0) {
                return true;
            }
        }
        return false;
    }
    private void sendProgressBar(Player player, double currentHp, double maxHp) {
        int totalBars = 10;
        int filledBars = (int) ((1 - (currentHp / maxHp)) * totalBars);
        int percentage = (int) ((1 - (currentHp / maxHp)) * 100);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("§a█");
            } else {
                bar.append("§c█");
            }
        }

        Messages.sendActionBarFormatted(player, "events.picking.progress_bar", Map.of(
                "bar", bar.toString(),
                "percent", String.valueOf(percentage)
        ));

    }

    public void resetAndCancel() {
        this.cancel();
        item.setHp(item.getInitialhp());
        item.setPickable(true);
        if (onFinish != null) onFinish.run();
    }

    private int extractStoreNumber(String id) {
        Matcher matcher = Pattern.compile("\\d+").matcher(id);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 0;
    }

    private String getStoreId(int storeOrder){
        Keys k = KeyManager.getKeyByOrder(storeOrder);
        if(k != null)
            return k.getId();
        return "store1";
    }

}

