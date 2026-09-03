package robbery.core;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.backpacks.BackpackManager;
import robbery.booster.BoosterManager;
import robbery.chat.ChatStyleManager;
import robbery.keys.KeyManager;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.player.PrestigeLeaderboard;
import robbery.quest.Quest;
import robbery.quest.QuestProgress;
import robbery.robberyLevel_XP.XPManager;
import robbery.skilltree.SkillPerk;
import robbery.skilltree.SkillTreeConfig;
import robbery.tool.ToolManager;

import java.util.Arrays;
import java.util.Optional;

import static robbery.attribute.Attribute.*;

public class RobberyPlaceholderExpansion extends PlaceholderExpansion {

    private final Robbery main;

    public RobberyPlaceholderExpansion(Robbery main) {
        this.main = main;
    }

    @Override
    public @NotNull String getIdentifier() { return "robbery"; }

    @Override
    public @NotNull String getAuthor() { return "Mesumi"; }

    @Override
    public @NotNull String getVersion() { return "1.1"; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        String id = identifier.toLowerCase();

        // 0. Handle Global (Player-Independent) Placeholders first
        String[] parts = id.split("_");
        if (parts.length >= 3 && parts[0].equals("prestige")) {
            try {
                int rank = Integer.parseInt(parts[2]);
                if (parts[1].equals("name")) return robbery.leaderboard.DatabaseLeaderboard.getTopPrestigeName(rank);
                if (parts[1].equals("top") || parts[1].equals("value")) return robbery.leaderboard.DatabaseLeaderboard.getTopPrestigeValue(rank);
            } catch (NumberFormatException ignored) {}
        }

        // %robbery_top_prestige_1_name%, %robbery_top_prestige_1_value%
        // %robbery_top_stolen_1_name%,   %robbery_top_stolen_1_value%
        if (parts.length >= 3 && parts[0].equals("top")) {
            try {
                int pos = Integer.parseInt(parts[2]);
                String field = (parts.length >= 4) ? parts[3] : "value";
                if (parts[1].equals("prestige")) {
                    return field.equalsIgnoreCase("name")
                            ? robbery.leaderboard.DatabaseLeaderboard.getTopPrestigeName(pos)
                            : robbery.leaderboard.DatabaseLeaderboard.getTopPrestigeValue(pos);
                }
                if (parts[1].equals("stolen") || parts[1].equals("itemsstolen")) {
                    return field.equalsIgnoreCase("name")
                            ? robbery.leaderboard.DatabaseLeaderboard.getTopItemsStolenName(pos)
                            : robbery.leaderboard.DatabaseLeaderboard.getTopItemsStolenValue(pos);
                }
            } catch (NumberFormatException ignored) {}
        }

        // %robbery_target_<player>_<placeholder>% or %robbery_other_<player>_<placeholder>%
        if (id.startsWith("target_") || id.startsWith("other_")) {
            int firstUnderscore = id.indexOf('_');
            int secondUnderscore = id.indexOf('_', firstUnderscore + 1);
            if (secondUnderscore != -1) {
                String targetName = identifier.substring(firstUnderscore + 1, secondUnderscore);
                String subIdentifier = identifier.substring(secondUnderscore + 1);

                org.bukkit.OfflinePlayer targetOff = org.bukkit.Bukkit.getOfflinePlayer(targetName);
                if (targetOff != null) {
                    PlayerData targetPd = null;
                    if (targetOff.isOnline() && targetOff.getPlayer() != null) {
                        targetPd = PlayerDataManager.getPlayerData(targetOff.getPlayer());
                    } else {
                        org.bukkit.configuration.file.YamlConfiguration tCfg = main.getPlayerDataDao().loadPlayerData(targetOff.getUniqueId());
                        if (tCfg != null) {
                            targetPd = new PlayerData(null);
                            main.getPlayerEventListener().loadPlayerDataFromDB(null, targetPd, tCfg);
                        }
                    }
                    if (targetPd != null) {
                        Player targetP = targetOff.isOnline() ? targetOff.getPlayer() : null;
                        String subId = subIdentifier.toLowerCase();
                        String match = handleStaticIdentifier(targetPd, subId, targetP);
                        if (match != null) return match;
                        String[] subParts = subId.split("_");
                        if (subParts.length >= 2) {
                            return handleDynamicIdentifier(targetPd, subParts, subId);
                        }
                    }
                }
            }
        }

        if (offlinePlayer == null) return null;
        Player player = offlinePlayer.getPlayer();

        PlayerData pd = null;
        if (player != null) {
            pd = PlayerDataManager.getPlayerData(player);
        } else {
            org.bukkit.configuration.file.YamlConfiguration cfg = main.getPlayerDataDao().loadPlayerData(offlinePlayer.getUniqueId());
            if (cfg != null) {
                pd = new PlayerData(null);
                main.getPlayerEventListener().loadPlayerDataFromDB(null, pd, cfg);
            }
        }
        if (pd == null) return null;

        // 1. Try static/exact matches first
        String staticMatch = handleStaticIdentifier(pd, id, player);
        if (staticMatch != null) return staticMatch;

        // 2. Fallback to Dynamic (Split) Logic
        parts = id.split("_");
        if (parts.length >= 2) {
            return handleDynamicIdentifier(pd, parts, id);
        }

        return null;
    }

    /**
     * Handles exact string matches.
     * Put anything here that doesn't need to be parsed by index/number.
     */
    private String handleStaticIdentifier(PlayerData pd, String id, Player p) {
        XPManager xp = main.getXpManager();
        return switch (id) {
            case "backpack_name" -> pd.getBackpack().getColorname();
            case "backpack_capacity" -> String.valueOf(pd.getBackpack().getcapacity());
            case "backpack_size" -> String.valueOf(pd.getBackpack().getSize());
            case "backpack_total" -> NumberFormatter.formatDoubleNumber(pd.getBackpack().getTotal());
            case "tool_name" -> pd.getTool().getColorname();
            case "key_name" -> pd.getKey().getColorname();
            case "tool_n" -> String.valueOf(ToolManager.getToolsNameR(pd.getTool().getName()));
            case "back_n" -> String.valueOf(BackpackManager.getBackpackNameR(pd.getBackpack().getName()));
            case "prestige" -> String.valueOf(pd.getPrestige());
            case "prestige_price" -> String.valueOf(robbery.prestige.Prestige.getPrestigeValue(pd));
            case "prestige_priceshort" -> NumberFormatter.formatDoubleNumber(robbery.prestige.Prestige.getPrestigeValue(pd));
            case "next_store" -> nextStorePercentage(pd, p);
            case "level" -> String.valueOf(pd.getLevel());
            case "xp" -> String.valueOf(pd.getXp());
            case "xp_formatted" -> robbery.number.NumberFormatter.formatDoubleNumber((double) pd.getXp());
            case "xp_in_level_formatted" -> robbery.number.NumberFormatter.formatDoubleNumber((double) Math.max(0L, pd.getXp() - xp.getCumulativeForLevel(pd.getLevel())));
            case "level_req_formatted" -> robbery.number.NumberFormatter.formatDoubleNumber((double) xp.xpNext(pd.getLevel()));
            case "xp_percentage" -> {
                long currentXpInLevel = Math.max(0L, pd.getXp() - xp.getCumulativeForLevel(pd.getLevel()));
                long xpNeeded = xp.xpNext(pd.getLevel());
                double progress = Math.min(100.0, Math.max(0.0, ((double) currentXpInLevel / xpNeeded) * 100.0));
                yield String.format("%.1f%%", progress);
            }
            case "xptonext" -> String.valueOf(xp.xpRemainingForNextLevel(pd.getXp(), pd.getLevel()));
            case "xptonext_formatted" -> robbery.number.NumberFormatter.formatDoubleNumber((double) xp.xpRemainingForNextLevel(pd.getXp(), pd.getLevel()));
            case "xp_progressbar" -> {
                long currentXpInLevel = Math.max(0L, pd.getXp() - xp.getCumulativeForLevel(pd.getLevel()));
                long xpNeeded = xp.xpNext(pd.getLevel());
                double progress = Math.min(1.0, Math.max(0.0, (double) currentXpInLevel / xpNeeded));
                int bars = 20;
                int coloredBars = (int) (progress * bars);
                yield "&a" + "|".repeat(coloredBars) + "&7" + "|".repeat(bars - coloredBars);
            }
            case "reward_sp" -> String.valueOf(xp.skillPointsForLevel(pd.getLevel()));
            case "reward_color" -> xp.getLevelColorName(pd.getLevel());
            case "extraxp" -> String.format("%.1f", pd.getXPBoost() * 100);
            case "extraxp_formatted" -> String.format("%.1f%%", pd.getXPBoost() * 100);
            case "levelcolored" -> xp.colorizeLevel(pd.getLevel());
            case "levelcolor" -> xp.getLevelHexColor(pd.getLevel());
            case "boosterx" -> String.format("%.3f",pd.getBoost());
            case "stealspeed" -> String.valueOf(pd.getExtraDamage());
            case "backpackslots" -> String.valueOf(pd.getExtraSlots());
            case "backpackunlocked" ->  String.valueOf(pd.getBackpackUnlocked());
            case "toolunlocked" ->  String.valueOf(pd.getToolsUnlocked());
            case "booster_name" -> pd.getActiveboost().getName();
            case "booster_time" -> String.valueOf(pd.getActiveboost().getSeconds());
            case "booster_paused" -> String.valueOf(pd.isBoostersPaused());
            case "booster_priority" -> String.valueOf(pd.getActiveboost().getPriority());
            case "skillpoints" -> String.valueOf(pd.getSkillPoints());

            // Chance & Perk Placeholders
            case "double_item_chance", "doubleitem_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_DOUBLE_ITEM1));
            case "triple_item_chance", "tripleitem_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_TRIPLE_ITEM1));
            case "insta_steal_chance", "instasteal_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_INSTA_STEAL1));
            case "double_inventory_chance", "doubleinventory_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_DOUBLE_INV1));
            case "avoid_caught_chance", "avoidbeingcaught_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_AVOID_CAUGHT1));
            case "booster_chance" -> String.format("%.1f%%", pd.getPerkValue(PERK_BOOST1));
            case "outpost_buff", "outpost_buff_percent" -> String.format("%.1f%%", pd.getPerkValue(PERK_OUT_BUFF1));

            case "outmaterial" -> main.getOutpostManager().getOutpostMaterial(p);
            case "outstatustitle" -> main.getOutpostManager().getStatusTitle();
            case "outtime" -> main.getOutpostManager().getStatusLoreLine1();
            case "outholderhead" -> main.getOutpostManager().getHolderLeaderName();
            case "outholdername" -> main.getOutpostManager().getHolderIslandName();
            case "outperk1" -> String.valueOf(main.getOutpostManager().getPerk1());
            case "outperk2" -> main.getOutpostManager().getPerk2();
            case "chatstyle" -> getColor(p);
            case "total_items_stolen" -> String.valueOf(pd.getItemsStolen());
            case "voteparty_required" -> String.valueOf(main.getVotePartyManager().getDisplayRequiredVotes());
            case "voteparty_current" -> String.valueOf(main.getVotePartyManager().getDisplayCurrentVotes());
            case "quests_completed" -> String.valueOf(pd.getDailyQuestsCompleted());
            case "quests_total" -> "3";
            case "quests_accepted" -> String.valueOf(!pd.getAcceptedDailyQuests().isEmpty());
            case "total_rewards" -> getTotalRewards(pd);
            case "skilltreereset_points" -> String.valueOf(pd.getResetSkillTreePoints());
            case "skilltreereset_skillpoints" -> String.valueOf(Robbery.getSkillTreeConfig().calculateTotalRefund(pd));
            case "skillpoint_chance" -> {
                double totalPercent = 1.0 + pd.getPerkValue(PERK_CHANCE_SP1) + pd.getOutSpChance();
                if (totalPercent >= 100.0) {
                    yield "1/1";
                } else {
                    int denominator = (int) Math.max(1, Math.round(100.0 / totalPercent));
                    yield "1/" + denominator;
                }
            }
            case "skillpoint_chance_percent" -> String.format("%.1f%%", 1.0 + pd.getPerkValue(PERK_CHANCE_SP1) + pd.getOutSpChance());

            case "crypto_has_machine" -> String.valueOf(robbery.crypto.CryptoItemHelper.playerAlreadyHasMachine(p, main));
            case "crypto_is_placed" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                yield String.valueOf(machine != null && machine.isPlaced());
            }
            case "crypto_status" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null || !machine.isPlaced()) yield "&cNot Placed";
                if (machine.getFuelTicks() > 0) yield "&aOnline";
                yield "&cOffline (Needs Battery)";
            }
            case "crypto_money" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield NumberFormatter.formatDoubleNumber((double) machine.getUnclaimedMoney());
            }
            case "crypto_money_raw" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(machine.getUnclaimedMoney());
            }
            case "crypto_money_ps" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long baseRate = main.getCryptoManager().getBaseRateForStore(pd.getHighestOwnedStoreTier());
                double qualityMult = machine.getQualityMultiplier();
                double speedMult = machine.getSpeedMultiplier();
                double rewardMult = machine.getRewardMultiplier();
                double onlineBuff = 1.0;
                if (p.isOnline() && machine.getFuelTicks() > 0) onlineBuff = 1.20; // 20% online buff
                
                long totalPs = (long) (baseRate * qualityMult * speedMult * rewardMult * onlineBuff);
                yield NumberFormatter.formatDoubleNumber((double) totalPs);
            }
            case "crypto_money_ps_raw" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long baseRate = main.getCryptoManager().getBaseRateForStore(pd.getHighestOwnedStoreTier());
                double qualityMult = machine.getQualityMultiplier();
                double speedMult = machine.getSpeedMultiplier();
                double rewardMult = machine.getRewardMultiplier();
                double onlineBuff = 1.0;
                if (p.isOnline() && machine.getFuelTicks() > 0) onlineBuff = 1.20;
                
                long totalPs = (long) (baseRate * qualityMult * speedMult * rewardMult * onlineBuff);
                yield String.valueOf(totalPs);
            }
            case "crypto_quality" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null || machine.getFuelTicks() <= 0) yield "0";
                yield String.format("%.1f", machine.getFuelQuality());
            }
            case "crypto_battery" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null || machine.getFuelTicks() <= 0) yield "&cNo Battery";
                long totalSeconds = machine.getFuelTicks();
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                if (hours > 0) yield String.format("%02d:%02d:%02d", hours, minutes, seconds);
                yield String.format("%02d:%02d", minutes, seconds);
            }
            case "crypto_battery_raw" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(machine.getFuelTicks());
            }
            case "crypto_battery_time_formatted" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0s";
                yield robbery.crypto.CryptoMachine.getFuelDurationFormattedForLevel(machine.getFuelTimeLevel());
            }
            case "crypto_speed_level" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "1";
                yield String.valueOf(machine.getSpeedLevel());
            }
            case "crypto_battery_time_level", "crypto_fueltime_level" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "1";
                yield String.valueOf(machine.getFuelTimeLevel());
            }
            case "crypto_reward_level" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "1";
                yield String.valueOf(machine.getRewardLevel());
            }
            case "crypto_multiplier" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "1.00";
                double mult = machine.getQualityMultiplier() * machine.getSpeedMultiplier() * machine.getRewardMultiplier();
                if (p.isOnline() && machine.getFuelTicks() > 0) mult *= 1.20;
                yield String.format("%.2f", mult);
            }
            case "crypto_stored_batteries", "crypto_stored_fuels" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(machine.getStoredFuels().size());
            }
            case "crypto_speed_cost" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getSpeedLevel());
                yield cost < 0 ? "MAX" : String.valueOf(cost);
            }
            case "crypto_speed_cost_formatted" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getSpeedLevel());
                yield cost < 0 ? "MAX" : NumberFormatter.formatDoubleNumber((double) cost);
            }
            case "crypto_speed_req_prestige" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(robbery.crypto.CryptoUpgradeManager.getRequiredPrestige(machine.getSpeedLevel() + 1));
            }
            case "crypto_battery_time_cost", "crypto_fueltime_cost" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getFuelTimeLevel());
                yield cost < 0 ? "MAX" : String.valueOf(cost);
            }
            case "crypto_battery_time_cost_formatted", "crypto_fueltime_cost_formatted" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getFuelTimeLevel());
                yield cost < 0 ? "MAX" : NumberFormatter.formatDoubleNumber((double) cost);
            }
            case "crypto_battery_time_req_prestige", "crypto_fueltime_req_prestige" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(robbery.crypto.CryptoUpgradeManager.getRequiredPrestige(machine.getFuelTimeLevel() + 1));
            }
            case "crypto_reward_cost" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getRewardLevel());
                yield cost < 0 ? "MAX" : String.valueOf(cost);
            }
            case "crypto_reward_cost_formatted" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                long cost = robbery.crypto.CryptoUpgradeManager.getUpgradeCost(machine.getRewardLevel());
                yield cost < 0 ? "MAX" : NumberFormatter.formatDoubleNumber((double) cost);
            }
            case "crypto_reward_req_prestige" -> {
                robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(p.getUniqueId());
                if (machine == null) yield "0";
                yield String.valueOf(robbery.crypto.CryptoUpgradeManager.getRequiredPrestige(machine.getRewardLevel() + 1));
            }
            case "crypto_dealer_talked", "crypto_talked_npc", "crypto_talked_dealer", "crypto_dealer_unlocked", "has_talked_crypto_dealer" -> String.valueOf(pd.hasTalkedToCryptoNPC());
            case "crypto_battery_talked", "crypto_talked_battery_npc", "crypto_talked_battery", "crypto_battery_unlocked", "has_talked_crypto_battery" -> String.valueOf(pd.hasTalkedToCryptoBatteryNPC());

            default -> null;
        };
    }



    /**
     * Handles patterns like has_back_1, price_tool_2, store_items_1
     */
    private String handleDynamicIdentifier(PlayerData pd, String[] parts, String fullId) {
        String type = parts[0];
        try {
            switch (type) {
                case "crypto":
                    if (parts.length >= 4 && parts[1].equalsIgnoreCase("fuel")) {
                        robbery.crypto.CryptoMachine machine = main.getCryptoManager().getMachine(pd.getPlayer().getUniqueId());
                        if (machine == null) return "0";
                        try {
                            int idx = Integer.parseInt(parts[3]) - 1; // 1-based index
                            if (idx >= 0 && idx < machine.getStoredFuels().size()) {
                                robbery.crypto.StoredFuel fuel = machine.getStoredFuels().get(idx);
                                if (parts[2].equalsIgnoreCase("quality")) {
                                    return String.format("%.1f", fuel.getQuality());
                                } else if (parts[2].equalsIgnoreCase("duration")) {
                                    long baseDur = machine.getFuelDurationTicks();
                                    long scaled = (long) (baseDur * (fuel.getQuality() / 100.0));
                                    return robbery.crypto.CryptoMachine.getFuelDurationFormattedForTicks(scaled);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                        return "0";
                    }
                    return null;

                case "has":
                    if (parts.length < 3) return null;
                    return switch (parts[1]) {
                        case "back" -> String.valueOf(pd.hasBackpackName("back" + parts[2]));
                        case "tool" -> String.valueOf(pd.hasToolName("tool" + parts[2]));
                        case "key" -> String.valueOf(pd.hasKey("store" + parts[2]));
                        default -> null;
                    };

                case "price":
                    if (parts.length < 3) return null;
                    String num = parts[2];
                    return switch (parts[1]) {
                        case "back" -> String.valueOf(BackpackManager.getBackpackName("back" + num, 0).getPrice());
                        case "tool" -> String.valueOf(ToolManager.getToolsName("tool" + num).getPrice());
                        case "key" -> String.valueOf(KeyManager.getStoreName("store" + num).getPrice(pd));
                        case "backshort" -> BackpackManager.getBackpackName("back" + num, 0).getPriceformatted();
                        case "toolshort" -> ToolManager.getToolsName("tool" + num).getPriceformatted();
                        case "keyshort" -> KeyManager.getStoreName("store" + num).getPriceformatted(pd);
                        default -> null;
                    };

                case "colorname":
                case "name":
                    if (parts.length < 3) return null;
                    boolean isColor = type.equals("colorname");
                    return switch (parts[1]) {
                        case "back" -> isColor ? BackpackManager.getBackpackName("back" + parts[2], 0).getColorname() : BackpackManager.getBackpackName("back" + parts[2], 0).getName();
                        case "tool" -> isColor ? ToolManager.getToolsName("tool" + parts[2]).getColorname() : ToolManager.getToolsName("tool" + parts[2]).getName();
                        case "key" -> isColor ? KeyManager.getStoreName("store" + parts[2]).getColorname() : KeyManager.getStoreName("store" + parts[2]).getName();
                        default -> null;
                    };

                case "damage":
                    if (parts.length < 3 || !parts[1].equals("tool")) return null;
                    return String.valueOf((int) (ToolManager.getToolsName("tool" + parts[2]).getDamage() * 10));

                case "size":
                    if (parts.length < 3 || !parts[1].equals("back")) return null;
                    return String.valueOf(BackpackManager.getBackpackName("back" + parts[2], 0).getcapacity());

                case "material":
                    if (parts.length < 3 || !parts[1].equals("tool")) return null;
                    return String.valueOf(ToolManager.getToolsName("tool" + parts[2]).getMaterial());

                case "mastery":
                case "store":
                    if (parts.length < 3) return null;
                    String stat = parts[1].toLowerCase();
                    String rawStore = parts[2].toLowerCase().replace("store", "");
                    String storeId = "store" + rawStore;

                    // Support format: %robbery_store_<store>_reward_<milestone>%
                    if (rawStore.matches("\\d+") && parts.length >= 4 && parts[2].matches("\\d+") && !stat.equals("reward")) {
                        // handled by standard switch below
                    }

                    return switch (stat) {
                        case "items" -> String.valueOf(pd.getStoreItems(storeId));
                        case "milestone" -> String.valueOf(pd.getStoreMilestone(storeId));
                        case "level" -> String.valueOf(main.getMasteryManager().getLevelFromItems(storeId, pd.getStoreItems(storeId)));
                        case "nextmilestone" -> String.valueOf(main.getMasteryManager().getNextStoreMilestone(pd, storeId));
                        case "nextreward" -> {
                            int nm = main.getMasteryManager().getNextStoreMilestone(pd, storeId);
                            yield main.getMasteryManager().getRewardDisplay(storeId, nm);
                        }
                        case "nextrequired" -> {
                            int nm = main.getMasteryManager().getNextStoreMilestone(pd, storeId);
                            yield String.valueOf(main.getMasteryManager().getItemsRequiredForLevel(storeId, nm));
                        }
                        case "nextremaining" -> {
                            int nm = main.getMasteryManager().getNextStoreMilestone(pd, storeId);
                            int req = main.getMasteryManager().getItemsRequiredForLevel(storeId, nm);
                            yield String.valueOf(Math.max(req - pd.getStoreItems(storeId), 0));
                        }
                        case "reward" -> {
                            if (parts.length < 4) yield null;
                            try {
                                int milestone = Integer.parseInt(parts[3]);
                                yield main.getMasteryManager().getRewardDisplay(storeId, milestone);
                            } catch (NumberFormatException e) {
                                yield null;
                            }
                        }
                        case "required" -> {
                            if (parts.length < 4) yield null;
                            try {
                                int milestone = Integer.parseInt(parts[3]);
                                yield String.valueOf(main.getMasteryManager().getItemsRequiredForLevel(storeId, milestone));
                            } catch (NumberFormatException e) {
                                yield null;
                            }
                        }
                        case "remaining" -> {
                            if (parts.length < 4) yield null;
                            try {
                                int milestone = Integer.parseInt(parts[3]);
                                int req = main.getMasteryManager().getItemsRequiredForLevel(storeId, milestone);
                                yield String.valueOf(Math.max(req - pd.getStoreItems(storeId), 0));
                            } catch (NumberFormatException e) {
                                yield null;
                            }
                        }
                        case "bonus_money", "money_bonus" -> "+" + (int)(pd.getStoreMasteryMoneyMultiplier(storeId) * 100) + "%";
                        case "bonus_speed", "speed_bonus" -> "+" + (int)pd.getStoreMasteryStealSpeed(storeId) + "%";
                        case "bonus_xp", "xp_bonus" -> "+" + (int)(pd.getStoreMasteryRobberyXp(storeId) * 100) + "%";
                        case "bonus_sp", "sp_bonus" -> "+" + (int)(pd.getStoreMasterySkillPointChance(storeId) * 100) + "%";
                        case "bonus_double", "double_bonus" -> "+" + (int)(pd.getStoreMasteryDoubleItemChance(storeId) * 100) + "%";
                        case "bonus_instasteal", "instasteal_bonus" -> "+" + (int)(pd.getStoreMasteryInstaStealChance(storeId) * 100) + "%";
                        default -> {
                            // If structured as %robbery_store_1_reward_2%
                            if (parts.length >= 4 && parts[1].matches("\\d+") && parts[2].equalsIgnoreCase("reward")) {
                                String sId = "store" + parts[1];
                                try {
                                    int ms = Integer.parseInt(parts[3]);
                                    yield main.getMasteryManager().getRewardDisplay(sId, ms);
                                } catch (NumberFormatException ignored) {}
                            }
                            yield null;
                        }
                    };

                case "booster":
                    if (parts.length < 3) return null;
                    var b = BoosterManager.getBooster("boost" + parts[2]);
                    return switch (parts[1]) {
                        case "quantity" -> String.valueOf(pd.getBoosterQuantity("boost" + parts[2]));
                        case "name" -> b.getName();
                        case "time" -> String.valueOf(b.getSeconds() / 60);
                        case "priority" -> String.valueOf(b.getPriority());
                        default -> null;
                    };

                case "skilltree":
                    if (parts.length < 3) return null;

                    String metric = parts[1];
                    String perkId = parts[2];

                    SkillTreeConfig cfg = Robbery.getSkillTreeConfig();
                    if (cfg == null) return null;


                    SkillPerk perk = cfg.getTier(perkId);
                    if (perk == null) return null;

                    int current = pd.getSkillTreeLevel(perkId);
                    int max = perk.maxLevel();
                    int requiredLevel = perk.requiredLevel();
                    int costNext = current < max ? perk.costForNext(current) : 0;
                    double currentValue = pd.getPerkValue(perkId);
                    double nextValue = current < max ? perk.valueForLevel(current + 1) : perk.valueForLevel(current);
                    boolean meetReq = pd.canBuyPerk(perk);

                    return switch (metric) {
                        case "currentlevel" -> String.valueOf(current);
                        case "maxlevel" -> String.valueOf(max);
                        case "costnextlevel" -> String.valueOf(costNext);
                        case "currentvalue" -> String.format("%.1f", currentValue);
                        case "nextvalue" -> String.format("%.1f", nextValue);
                        case "canupgrade" -> {
                            if (pd.getLevel() < requiredLevel) yield "2";
                            if (!meetReq) yield "4";
                            if (current >= max) yield "3";
                            if (pd.getSkillPoints() >= costNext) yield "1";
                            yield "0";
                        }
                        case "displayname" -> perk.name();
                        case "requiredlevel" -> String.valueOf(requiredLevel);
                        case "description" -> perk.description();
                        default -> null;
                    };

                case "quest":
                    if (parts.length < 3) return null;

                    int index = Integer.parseInt(parts[1]) - 1;
                    if (pd.getOfferedDailyQuests().size() <= index) return "";

                    String questId = pd.getOfferedDailyQuests().get(index);
                    Quest quest = main.getQuestManager().getQuest(questId);
                    if (quest == null) return "";

                    QuestProgress progress = pd.getQuestProgressMap().get(questId);
                    int currentProgress = (progress != null) ? progress.getItemsCompleted() : 0;

                    return switch (parts[2]) {
                        case "name" -> quest.name;
                        case "required" -> String.valueOf(quest.itemsRequired);
                        case "progress" -> String.valueOf(currentProgress);
                        case "rewardxp" -> {
                            int xpPerItem = main.getQuestService().computeQuestXpPerItem(quest, pd);
                            long totalXp = (long) xpPerItem * quest.itemsRequired;
                            yield NumberFormatter.formatLong( totalXp / 2);
                        }
                        case "rewardstore" -> {
                            if (quest.rewardStoreItems == null) yield "0";
                            yield String.valueOf(quest.rewardStoreItems.amount/2);
                        }
                        case "rewardsskillpoints" -> String.valueOf(quest.getSkillPointRewards(pd));
                        case "storename" -> {
                            if (quest.rewardStoreItems == null) yield "";
                            yield KeyManager.getStoreN(quest.rewardStoreItems.store);
                        }
                        case "description" -> quest.description;

                        default -> null;
                    };


                default: return null;
            }
        } catch (Exception e) { return null; }
    }

    private String getColor(Player player) {
        ChatStyleManager styles = main.getChatStyleManager();
        Optional<String> stored = styles.getColor(player.getUniqueId());
        ChatColor color = stored.map(s -> {
            try { return ChatColor.valueOf(s); } catch (Exception e) { return ChatColor.GRAY; }
        }).orElse(ChatColor.GRAY);
        return color + (styles.isBold(player.getUniqueId()) ? ChatColor.BOLD.toString() : "");
    }

    private String nextStorePercentage(PlayerData pd, Player p) {
        int nextStore = pd.getKey().getOrder() + 1;
        var key = KeyManager.getKeyByOrder(nextStore);
        if (key == null) return "MAX";
        Economy econ = Robbery.getEconomy();
        double balance = econ.getBalance(p);
        double price = key.getPrice(pd);
        double percentage = Math.min((balance / price) * 100.0, 100.0);
        return String.format("%.2f%%", percentage);
    }

    public static void registerHook() {
        new RobberyPlaceholderExpansion(Robbery.getInstance()).register();
    }
    private @NotNull String getTotalRewards(PlayerData pd) {
        long grandTotalXp = 0;
        int grandTotalSP = 0;
        if(pd.getPerkValue(PERK_ABILITY_SPQUEST1) > 0)
            grandTotalSP += 1;
        if(pd.getPerkValue(PERK_ABILITY_SPQUEST2) > 0)
            grandTotalSP += 2;
        java.util.Map<String, Integer> combinedStoreItems = new java.util.HashMap<>();

        for (String qId : pd.getOfferedDailyQuests()) {
            Quest q = main.getQuestManager().getQuest(qId);
            if (q == null) continue;

            int xpPerItem = main.getQuestService().computeQuestXpPerItem(q, pd);
            grandTotalXp += (long) xpPerItem * q.itemsRequired;

            if (q.rewardStoreItems != null) {
                combinedStoreItems.merge(q.rewardStoreItems.store, q.rewardStoreItems.amount, Integer::sum);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§b✨Robbery XP: §f").append(NumberFormatter.formatLong(grandTotalXp/2));
        sb.append("\n§6\uD83D\uDCDASkillPoints: §f").append(grandTotalSP);

        if (!combinedStoreItems.isEmpty()) {
            sb.append("\n§c\uD83D\uDCE6Store Mastery: ");
            combinedStoreItems.forEach((store, amount) -> sb.append("§f").append(amount/2).append("x §f").append(KeyManager.getStoreN(store)).append(" ")
            );
        }
        return sb.toString();
    }
}
