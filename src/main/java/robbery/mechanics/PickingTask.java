package robbery.mechanics;

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

public class PickingTask extends BukkitRunnable {
    private final Player player;
    private final Items item;
    private final ArmorStand stand;
    private final Tools tool;
    private final boolean chanceProc;
    private final Robbery main;
    private final Random random = new Random();
    private final Runnable onFinish;

    public PickingTask(Player player, Items item, ArmorStand stand, Tools tool, Robbery main, boolean chanceProc, Runnable onFinish) {
        this.player = player;
        this.item = item;
        this.stand = stand;
        this.tool = tool;
        this.main = main;
        this.chanceProc = chanceProc;
        this.onFinish = onFinish;
    }

    @Override
    public void run() {
        PlayerData p = PlayerDataManager.getPlayerData(player);
        if (item.getHp() <= 0) {
            this.cancel();
            item.resetspawn(item.getTime());
            p.addItemToBackpack(item);
            String itemName = item.getName().substring(0,1).toUpperCase() + item.getName().substring(1);
            double value = item.getValue();
            double bonus = (value * p.getBoost()) - value;
            if (p.getBoost() != 1.0) {
                Messages.sendActionBarFormatted(player, "events.picking.item_stolen_with_boost", Map.of(
                        "item", itemName,
                        "value", NumberFormatter.formatDoubleNumber(value) + "$",
                        "bonus", NumberFormatter.formatDoubleNumber(bonus) + "$"
                ));
            } else {
                Messages.sendActionBarFormatted(player, "events.picking.item_stolen", Map.of(
                        "item", itemName,
                        "value", NumberFormatter.formatDoubleNumber(value) + "$"
                ));
            }
            if(p.getSPShop().doubleItemChance() != 0){
                int roll = random.nextInt((int) (1/p.getSPShop().doubleItemChance())) + 1;
                if(roll == 1 && !p.getBackpack().isFull()) {
                    p.addItemToBackpack(item);
                    Messages.sendActionBarFormatted(player, "events.picking.double_item", Map.of(
                            "item", itemName,
                            "value", NumberFormatter.formatDoubleNumber(value) + "$"
                    ));

                }
            }
            int itemStoreNum = extractStoreNumber(item.getId());
            String storeId = getStoreId(itemStoreNum);
            main.getMasteryManager().incrementMastery(player, storeId);
            p.addItemsStolen(1);
            Booster booster = BoosterManager.getRandomBoosterWithChance(itemStoreNum,p);
            if(booster != null) {
                Messages.sendFormatted(player, "events.picking.booster_reward", Map.of(
                                    "booster", booster.getName()));
                p.addBoosters(booster);
            }
            int roll = random.nextInt(1000) + 1;
            if(p.getSPShop().skillpointChance() + p.getOutSpChance() != 0)
                roll = random.nextInt((int) (1000*(1-p.getSPShop().skillpointChance()-p.getOutSpChance()))) + 1;
            if(roll == 11){
                player.sendTitle(Messages.get("events.picking.skillpoint_reward_title"), Messages.get("events.picking.skillpoint_reward_subtitle"), 10, 60, 10);
                p.addSkillpoint();
            }
            if (onFinish != null) onFinish.run();
            return;
        }

        if (!isLookingAtStand(player, stand) || !player.isOnline()) {
            this.cancel();
            item.setHp(item.getInitialhp());
            item.togglePickable();
            Messages.sendActionBar(player, "events.picking.canceled");
            if (onFinish != null) onFinish.run();
            return;
        }
        if(chanceProc)
            item.setHp(0);
        else
            item.setHp(item.getHp() - (tool.getDamage() + tool.getDamage()*p.getExtraDamage()));
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

