package robbery;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.backpacks.BackpackManager;
import robbery.booster.BoosterManager;
import robbery.chat.ChatStyleManager;
import robbery.commands.Prestige;
import robbery.keys.KeyManager;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.player.PrestigeLeaderboard;
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

        if (offlinePlayer != null && offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();

            assert player != null;
            PlayerData playerData = PlayerDataManager.getPlayerData(player);

            String[] parts = identifier.toLowerCase().split("_");
            if (parts.length == 3) {
                String type = parts[0];
                String category = parts[1];
                String num = parts[2];
                if (type == null || category == null || num == null) {
                    return null;
                }

                switch (type) {
                    case "has":
                        switch (category) {
                            case "back":
                                return String.valueOf(playerData.hasBackpackName("back" + num));
                            case "tool":
                                return String.valueOf(playerData.hasToolName("tool" + num));
                            case "key":
                                return String.valueOf(playerData.hasKey("store" + num));
                        }
                        break;
                    case "price":
                        switch (category) {
                            case "back":
                                return String.valueOf(BackpackManager.getBackpackName("back" + num,0).getPrice());
                            case "tool":
                                return String.valueOf(ToolManager.getToolsName("tool" + num).getPrice());
                            case "key":
                                return String.valueOf(KeyManager.getStoreName("store" + num).getPrice(playerData));
                            case "backshort":
                                return String.valueOf(BackpackManager.getBackpackName("back" + num,0).getPriceformatted());
                            case "toolshort":
                                return String.valueOf(ToolManager.getToolsName("tool" + num).getPriceformatted());
                            case "keyshort":
                                return String.valueOf(KeyManager.getStoreName("store" + num).getPriceformatted(playerData));
                        }
                    case "colorname":
                        switch (category) {
                            case "back":
                                return String.valueOf(BackpackManager.getBackpackName("back" + num,0).getColorname());
                            case "tool":
                                return String.valueOf(ToolManager.getToolsName("tool" + num).getColorname());
                            case "key":
                                return String.valueOf(KeyManager.getStoreName("store" + num).getColorname());
                        }
                    case "name":
                        switch (category) {
                            case "back":
                                return String.valueOf(BackpackManager.getBackpackName("back" + num,0).getName());
                            case "tool":
                                return String.valueOf(ToolManager.getToolsName("tool" + num).getName());
                            case "key":
                                return String.valueOf(KeyManager.getStoreName("store" + num).getName());
                        }
                    case "material":
                        if (category.equals("tool")) {
                            return String.valueOf(ToolManager.getToolsName("tool" + num).getMaterial());
                        }
                        break;
                    case "prestige":
                        switch (category){
                            case "name":
                                return String.valueOf(PrestigeLeaderboard.getTopPrestigePlayer(Integer.parseInt(num)));
                            case "top":
                                return String.valueOf(PrestigeLeaderboard.getTopPrestige(Integer.parseInt(num)));
                        }
                    case "damage":
                        if (category.equals("tool")) {
                            return String.valueOf((int)(ToolManager.getToolsName("tool" + num).getDamage()*10));
                        }
                    case "size":
                        if (category.equals("back")) {
                            return String.valueOf(BackpackManager.getBackpackName("back" + num,0).getcapacity());
                        }
                    case "booster":
                        switch (category){
                            case "quantity":
                                return String.valueOf(playerData.getBoosterQuantity("boost" + num));
                            case "name":
                                return String.valueOf(BoosterManager.getBooster("boost" + num).getName());
                            case "time":
                                return String.valueOf(BoosterManager.getBooster("boost" + num).getSeconds()/60);
                            case "priority":
                                return String.valueOf(BoosterManager.getBooster("boost" + num).getPriority());
                        }
                    case "skillpoint":
                        return String.valueOf((int)(SkillUpgradeData.getUpgradePercentage(playerData,category, num)*100));
                }
            }
                switch (identifier.toLowerCase()) {
                    case "backpack_name":
                        return playerData.getBackpack().getColorname();
                    case "backpack_capacity":
                        return String.valueOf(playerData.getBackpack().getcapacity());
                    case "backpack_size":
                        return String.valueOf(playerData.getBackpack().getSize());
                    case "backpack_total":
                        return String.valueOf(NumberFormatter.formatDoubleNumber(Double.parseDouble(NumberFormatter.formatDouble(playerData.getBackpack().getTotal()))));
                    case "tool_name":
                        return String.valueOf(playerData.getTool().getColorname());
                    case "key_name":
                        return String.valueOf(playerData.getKey().getColorname());
                    case "prestige":
                        return String.valueOf(playerData.getPrestige());
                    case "prestige_price":
                        return String.valueOf(playerData.getPrestigeBoost() * Prestige.getPrestigeValue(playerData));
                    case "prestige_priceshort":
                        return NumberFormatter.formatDoubleNumber(playerData.getPrestigeBoost() * Prestige.getPrestigeValue(playerData));
                    case "boosterx":
                        return String.valueOf(playerData.getBoost());
                    case "booster_name":
                        return String.valueOf(playerData.getActiveboost().getName());
                    case "booster_time":
                        return String.valueOf(playerData.getActiveboost().getSeconds());
                    case "booster_priority":
                        return String.valueOf(playerData.getActiveboost().getPriority());
                    case "skillpoints":
                        return String.valueOf(playerData.getSP());
                    case "skillpoint_damage":
                        return String.valueOf((int)(playerData.getSPShop().extraDamage()*100));
                    case "skillpoint_itemchance":
                        return String.valueOf((int)(playerData.getSPShop().doubleItemChance()*100));
                    case "skillpoint_extramoney":
                        return String.valueOf((int)(playerData.getSPShop().extraMoney()*100));
                    case "skillpoint_slots":
                        return String.valueOf(playerData.getSPShop().extraSlots());
                    case "skillpoint_skillpointchance":
                        return String.valueOf((int)(playerData.getSPShop().skillpointChance()*100));
                    case "skillpoint_moneypouchchance":
                        return String.valueOf((int)(playerData.getSPShop().moneypouchChance()*100));
                    case "skillpoint_instastealchance":
                        return String.valueOf((int)(playerData.getSPShop().instastealChance()*100));
                    case "outmaterial":
                        return main.getOutpostManager().getOutpostMaterial(player);
                    case "outstatustitle":
                        return main.getOutpostManager().getStatusTitle();
                    case "outtime":
                        return main.getOutpostManager().getStatusLoreLine1();
                    case "outholderhead":
                        return main.getOutpostManager().getHolderLeaderName();
                    case "outholdername":
                        return main.getOutpostManager().getHolderIslandName();
                    case "outperk1":
                        return String.valueOf(main.getOutpostManager().getPerk1());
                    case "outperk2":
                        return main.getOutpostManager().getPerk2();
                    case "chatstyle":
                        return getColor(player);
                    case "booster_paused":
                        return String.valueOf(playerData.isBoostersPaused());

                    case "tool_n":
                        return String.valueOf(ToolManager.getToolsNameR(playerData.getTool().getName()));
                    case "back_n":
                        return String.valueOf(BackpackManager.getBackpackNameR(playerData.getBackpack().getName()));
                }
        }
        String[] parts = identifier.toLowerCase().split("_");
        if (parts.length == 3) {
            String type = parts[0];
            String category = parts[1];
            String num = parts[2];
            if (type == null || category == null || num == null) {
                return null;
            }
            if (type.equals("prestige")) {
                switch (category) {
                    case "name":
                        return String.valueOf(PrestigeLeaderboard.getTopPrestigePlayer(Integer.parseInt(num)));
                    case "top":
                        return String.valueOf(PrestigeLeaderboard.getTopPrestige(Integer.parseInt(num)));
                }
            }
        }
        return null;
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



}

