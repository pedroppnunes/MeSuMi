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
import robbery.robberyLevel_XP.XPManager;
import robbery.skilltree.SkillPerk;
import robbery.skilltree.SkillTreeConfig;
import robbery.tool.ToolManager;

import java.util.Arrays;
import java.util.Optional;

import static robbery.attribute.Attribute.PERCENTAGE_PERKS;

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
        if (offlinePlayer == null) return null;
        Player player = offlinePlayer.getPlayer();
        if (player == null) return null;

        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return null;

        String id = identifier.toLowerCase();

        // 1. Try static/exact matches first (Prevents total_items_stolen from breaking)
        String staticMatch = handleStaticIdentifier(pd, id, player);
        if (staticMatch != null) return staticMatch;

        // 2. Fallback to Dynamic (Split) Logic
        String[] parts = id.split("_");
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
            case "xptonext" -> String.valueOf(xp.xpRemainingForNextLevel(pd.getXp(), pd.getLevel()));
            case "levelcolored" -> xp.colorizeLevel(pd.getLevel());
            case "levelcolor" -> xp.getLevelHexColor(pd.getLevel());
            case "boosterx" -> String.valueOf(pd.getBoost());
            case "booster_name" -> pd.getActiveboost().getName();
            case "booster_time" -> String.valueOf(pd.getActiveboost().getSeconds());
            case "booster_paused" -> String.valueOf(pd.isBoostersPaused());
            case "booster_priority" -> String.valueOf(pd.getActiveboost().getPriority());
            case "skillpoints" -> String.valueOf(pd.getSkillPoints());
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

                case "store":
                    if (parts.length < 3) return null;
                    String stat = parts[1];
                    String storeId = "store" + parts[2];
                    return switch (stat) {
                        case "items" -> String.valueOf(pd.getStoreItems(storeId));
                        case "milestone" -> String.valueOf(pd.getStoreMilestone(storeId));
                        case "level" -> String.valueOf(main.getMasteryManager().getLevelFromItems(storeId, pd.getStoreItems(storeId)));
                        case "nextmilestone" -> String.valueOf(main.getMasteryManager().getNextStoreMilestone(pd, storeId));
                        case "reward" -> (parts.length == 4) ? main.getMasteryManager().getRewardDisplay(storeId, Integer.parseInt(parts[3])) : null;
                        case "required" -> (parts.length == 4) ? String.valueOf(main.getMasteryManager().getItemsRequiredForLevel(storeId, Integer.parseInt(parts[3]))) : null;
                        case "remaining" -> {
                            if (parts.length < 4) yield null;
                            int req = main.getMasteryManager().getItemsRequiredForLevel(storeId, Integer.parseInt(parts[3]));
                            yield String.valueOf(Math.max(req - pd.getStoreItems(storeId), 0));
                        }
                        default -> null;
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

                case "prestige":
                    if (parts.length < 3) return null;
                    int rank = Integer.parseInt(parts[2]);
                    return switch (parts[1]) {
                        case "name" -> PrestigeLeaderboard.getTopPrestigePlayer(rank);
                        case "top" -> String.valueOf(PrestigeLeaderboard.getTopPrestige(rank));
                        default -> null;
                    };

                case "skilltree":
                    if (parts.length < 3) return null;

                    String metric = parts[1];
                    String perkId = parts[2];

                    SkillTreeConfig cfg = main.getSkillTreeConfig();
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
                        case "currentvalue" -> {
                            double displayValue = currentValue;
                            if (PERCENTAGE_PERKS.contains(perkId)) {
                                displayValue *= 100;
                            }
                            yield String.format("%.3f", displayValue);
                        }
                        case "nextvalue" -> String.format("%.3f", nextValue);
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
}