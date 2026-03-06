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
import robbery.prestige.Prestige;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.player.PrestigeLeaderboard;
import robbery.robberyLevel_XP.XPManager;
import robbery.skillpoints.SkillUpgradeData;
import robbery.tool.ToolManager;

import java.util.Optional;

public class RobberyPlaceholderExpansion extends PlaceholderExpansion {

    private final Robbery main;

    public RobberyPlaceholderExpansion(Robbery main) {
        this.main = main;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "robbery"; // Placeholder identifier: %robbery_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "Mesumi"; // Your name or alias
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0"; // Version of your expansion
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return null;

        Player player = offlinePlayer.getPlayer();
        PlayerData playerData = PlayerDataManager.getPlayerData(player);
        if (playerData == null) return "";

        String id = identifier.toLowerCase();
        String[] parts = id.split("_");

        // Handle 3-part placeholders: %robbery_type_category_num%
        if (parts.length == 3) {
            return handleTriPartIdentifier(playerData, parts[0], parts[1], parts[2], player);
        }

        // Handle static/2-part placeholders: %robbery_level%
        return handleStaticIdentifier(playerData, id, player);
    }

    private String handleTriPartIdentifier(PlayerData pd, String type, String category, String num, Player p) {
        try {
            switch (type) {
                case "has":
                    return switch (category) {
                        case "back" -> String.valueOf(pd.hasBackpackName("back" + num));
                        case "tool" -> String.valueOf(pd.hasToolName("tool" + num));
                        case "key"  -> String.valueOf(pd.hasKey("store" + num));
                        default -> null;
                    };

                case "price":
                    return switch (category) {
                        case "back"      -> String.valueOf(BackpackManager.getBackpackName("back" + num, 0).getPrice());
                        case "tool"      -> String.valueOf(ToolManager.getToolsName("tool" + num).getPrice());
                        case "key"       -> String.valueOf(KeyManager.getStoreName("store" + num).getPrice(pd));
                        case "backshort" -> BackpackManager.getBackpackName("back" + num, 0).getPriceformatted();
                        case "toolshort" -> ToolManager.getToolsName("tool" + num).getPriceformatted();
                        case "keyshort"  -> KeyManager.getStoreName("store" + num).getPriceformatted(pd);
                        default -> null;
                    };

                case "colorname":
                    return switch (category) {
                        case "back" -> BackpackManager.getBackpackName("back" + num, 0).getColorname();
                        case "tool" -> ToolManager.getToolsName("tool" + num).getColorname();
                        case "key"  -> KeyManager.getStoreName("store" + num).getColorname();
                        default -> null;
                    };

                case "name":
                    return switch (category) {
                        case "back" -> BackpackManager.getBackpackName("back" + num, 0).getName();
                        case "tool" -> ToolManager.getToolsName("tool" + num).getName();
                        case "key"  -> KeyManager.getStoreName("store" + num).getName();
                        default -> null;
                    };

                case "prestige":
                    int index = Integer.parseInt(num);
                    return category.equals("name") ?
                            PrestigeLeaderboard.getTopPrestigePlayer(index) :
                            String.valueOf(PrestigeLeaderboard.getTopPrestige(index));

                case "booster":
                    return switch (category) {
                        case "quantity" -> String.valueOf(pd.getBoosterQuantity("boost" + num));
                        case "name"     -> BoosterManager.getBooster("boost" + num).getName();
                        case "time"     -> String.valueOf(BoosterManager.getBooster("boost" + num).getSeconds() / 60);
                        case "priority" -> String.valueOf(BoosterManager.getBooster("boost" + num).getPriority());
                        default -> null;
                    };

                case "skillpoint":
                    return String.valueOf((int) (SkillUpgradeData.getUpgradePercentage(pd, category, num) * 100));

                case "damage":
                    if (category.equals("tool"))
                        return String.valueOf((int) (ToolManager.getToolsName("tool" + num).getDamage() * 10));
                    break;

                case "size":
                    if (category.equals("back"))
                        return String.valueOf(BackpackManager.getBackpackName("back" + num, 0).getcapacity());
                    break;

                case "material":
                    if (category.equals("tool"))
                        return String.valueOf(ToolManager.getToolsName("tool" + num).getMaterial());
                    break;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String handleStaticIdentifier(PlayerData pd, String id, Player p) {
        XPManager xp = main.getXpManager();

        return switch (id) {
            // Backpack & Tools
            case "backpack_name"      -> pd.getBackpack().getColorname();
            case "backpack_capacity"  -> String.valueOf(pd.getBackpack().getcapacity());
            case "backpack_size"      -> String.valueOf(pd.getBackpack().getSize());
            case "backpack_total"     -> NumberFormatter.formatDoubleNumber(Double.parseDouble(NumberFormatter.formatDouble(pd.getBackpack().getTotal())));
            case "tool_name"          -> pd.getTool().getColorname();
            case "key_name"           -> pd.getKey().getColorname();
            case "tool_n"             -> String.valueOf(ToolManager.getToolsNameR(pd.getTool().getName()));
            case "back_n"             -> String.valueOf(BackpackManager.getBackpackNameR(pd.getBackpack().getName()));

            // Prestige & Money
            case "prestige"           -> String.valueOf(pd.getPrestige());
            case "prestige_price"     -> String.valueOf(Prestige.getPrestigeValue(pd));
            case "prestige_priceshort"-> NumberFormatter.formatDoubleNumber(Prestige.getPrestigeValue(pd));
            case "next_store"         -> nextStorePercentage(pd, p);

            // Levels & XP
            case "level"              -> String.valueOf(pd.getLevel());
            case "xp"                 -> String.valueOf(pd.getXp());
            case "xptonext"           -> String.valueOf(xp.xpRemainingForNextLevel(pd.getXp(), pd.getLevel()));
            case "levelcolored"       -> xp.colorizeLevel(pd.getLevel());
            case "levelcolor"         -> xp.getLevelColor(pd.getLevel()).toString();

            // Active Boosters
            case "boosterx"           -> String.valueOf(pd.getBoost());
            case "booster_name"       -> pd.getActiveboost().getName();
            case "booster_time"       -> String.valueOf(pd.getActiveboost().getSeconds());
            case "booster_paused"     -> String.valueOf(pd.isBoostersPaused());

            // Skill Points
            case "skillpoints"        -> String.valueOf(pd.getSP());
            case "skillpoint_damage"  -> String.valueOf((int)(pd.getSPShop().extraDamage()*100));
            case "skillpoint_itemchance" -> String.valueOf((int)(pd.getSPShop().doubleItemChance()*100));
            case "skillpoint_extramoney" -> String.valueOf((int)(pd.getSPShop().extraMoney()*100));

            // Outpost & Misc
            case "outstatustitle"     -> main.getOutpostManager().getStatusTitle();
            case "outtime"            -> main.getOutpostManager().getStatusLoreLine1();
            case "chatstyle"          -> getColor(p);
            case "voteparty_required" -> String.valueOf(main.getVotePartyManager().getDisplayRequiredVotes());
            case "voteparty_current"  -> String.valueOf(main.getVotePartyManager().getDisplayCurrentVotes());

            default -> null;
        };
    }

    private String getColor(Player player){
        ChatStyleManager styles = main.getChatStyleManager();
        Optional<String> stored = styles.getColor(player.getUniqueId());
        ChatColor color = stored.map(s -> {
            try {
                return ChatColor.valueOf(s);
            } catch (Exception e) {
                return ChatColor.GRAY;
            }
        }).orElse(ChatColor.GRAY);

        String out = color.toString();
        if (styles.isBold(player.getUniqueId())) {
            out += ChatColor.BOLD;
        }
        return out;
    }

    public static void registerHook(){
        new RobberyPlaceholderExpansion(Robbery.getInstance()).register();
    }

    private String nextStorePercentage(PlayerData pd, Player p) {
        int nextStore = pd.getKey().getOrder() + 1;
        Keys key = KeyManager.getKeyByOrder(nextStore);
        Economy econ = Robbery.getEconomy();
        double balance = econ.getBalance(p);
        double price;
        if(key == null)
            price = balance;
        else
            price = key.getPrice(pd);

        double percentage = (balance / price) * 100.0;

        percentage = Math.min(percentage, 100.0);

        return String.format("%.2f%%", percentage);
    }



}

