package robbery.player;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import robbery.Robbery;
import robbery.backpacks.BackpackManager;
import robbery.commands.PvCommand;
import robbery.commands.Rcrate;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.messages.Messages;
import robbery.tool.ToolManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static robbery.keys.KeyManager.getStoreName;


public class PlayerEventListener implements Listener {

    private static final String NOITEMS_PERMISSION = "robbery.noitems";
    private final Robbery plugin;
    private boolean hasLoaded = false;

    public PlayerEventListener(Robbery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData memory = new PlayerData(event.getPlayer());
        File playerFolder = new File(plugin.getDataFolder(), "player/" + event.getPlayer().getUniqueId());
        File file = new File(playerFolder, "general.yml");
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        if (player.hasPermission("robbery.rank7") || player.hasPermission("robbery.staff")) {
            String prefix = getLuckPermsPrefix(player);
            String color = extractColorFromPrefix(prefix);

            for (Player online : Bukkit.getOnlinePlayers()) {
                Messages.sendComponentMessageFormatted(online, "join-message",
                        Map.of("prefix", prefix, "player", color + "&l" + player.getName()));
            }
        }

        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            memory.setBackpack(BackpackManager.toBackpack(cfg.getString("stats.backpack"),cfg.getString("stats.material"),cfg.getString("stats.itemsbackpack"),cfg.getString("stats.colorbackpack")));
            memory.setBackpackunlucked(cfg.getString("stats.hasbackpack"));
            memory.setToolsunlocked(cfg.getString("stats.hastool"));
            memory.setTool(ToolManager.getToolsName(cfg.getString("stats.tool")));
            memory.setKeys(cfg.getString("stats.haskeys"));
            memory.setKey(getStoreName(cfg.getString("stats.key")));
            memory.setPrestige(Integer.parseInt(cfg.getString("stats.prestige")));
            memory.setActiveBooster(cfg.getString("stats.booster"));
            memory.setBoostersFromString(cfg.getString("stats.hasbooster"));
            memory.setSP(cfg.getString("stats.skillpoints"));
            memory.setSPShopString(cfg.getString("stats.spshop"));
            memory.setRank(cfg.getString("stats.rank"));
            if (cfg.contains("stats.location.world") && cfg.contains("stats.location.x") && cfg.contains("stats.location.y") && cfg.contains("stats.location.z") && cfg.contains("stats.location.yaw") && cfg.contains("stats.location.pitch")) {
                String worldName = cfg.getString("stats.location.world");
                double x = cfg.getDouble("stats.location.x");
                double y = cfg.getDouble("stats.location.y");
                double z = cfg.getDouble("stats.location.z");
                float yaw = (float) cfg.getDouble("stats.location.yaw");
                float pitch = (float) cfg.getDouble("stats.location.pitch");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location savedLoc = new Location(world, x, y, z, yaw, pitch);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleport(savedLoc);
                        }
                    }, 5L);
                }
            }
        }

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island island = superiorPlayer.getIsland();
        UUID currentOutpostOwner = plugin.getOutpostManager().getCurrentIsland();

        if (island != null && currentOutpostOwner != null && island.getOwner().getUniqueId().equals(currentOutpostOwner)) {
            memory.setOutpostBoost(1.0);
        } else {
            memory.setOutpostBoost(0.0);
        }

        if (!hasLoaded) {
            hasLoaded = true;
            Bukkit.getScheduler().runTaskLater(plugin, plugin::loadItems, 20L);
        }
        if(!player.hasPermission(NOITEMS_PERMISSION)) {
            memory.giveToolToInv();
            memory.giveBackpackToInv();
            memory.giveBooster(plugin);
            memory.giveSkillPoint(plugin);
            memory.giveFeather();
            plugin.getHidePlayers().handleWorldChange(player);
        }
        Rcrate.loadRewards(player.getUniqueId());
        PlayerDataManager.setPlayerData(event.getPlayer(), memory);
        PrestigeLeaderboard.updateLeaderboard(event.getPlayer());
        boolean hasRank = player.hasPermission("robbery.rank1") || player.hasPermission("robbery.rank2") || player.hasPermission("robbery.rank3") || player.hasPermission("robbery.rank4") || player.hasPermission("robbery.rank5") || player.hasPermission("robbery.rank6") || player.hasPermission("robbery.rank7") || player.hasPermission("robbery.staff");
        plugin.getChatStyleManager().ensurePlayerExists(player, hasRank);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData memory = PlayerDataManager.getPlayerData(event.getPlayer());
        File playerFolder = new File(plugin.getDataFolder(), "player/" + event.getPlayer().getUniqueId());
        File file = new File(playerFolder, "general.yml");
        Player player = event.getPlayer();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        event.setQuitMessage(null);

        if (player.hasPermission("robbery.rank7") || player.hasPermission("robbery.staff")) {
            String prefix = getLuckPermsPrefix(player);
            String color = extractColorFromPrefix(prefix);

            for (Player online : Bukkit.getOnlinePlayers()) {
                Messages.sendComponentMessageFormatted(online, "quit-message",
                        Map.of("prefix", prefix, "player", color + "&l" + player.getName()));
            }
        }

        cfg.set("stats.backpack", memory.getBackpackString());
        cfg.set("stats.material", memory.getBackpack().getMaterial());
        cfg.set("stats.itemsbackpack",memory.getBackpackItemsString());
        cfg.set("stats.hasbackpack",memory.getBackpackunlucked());
        cfg.set("stats.colorbackpack",memory.getBackpack().getColorname());
        cfg.set("stats.hastool",memory.getToolsunlucked());
        cfg.set("stats.tool", memory.getToolString());
        cfg.set("stats.key",memory.getKeyString());
        cfg.set("stats.haskeys",memory.getKeysString());
        cfg.set("stats.prestige",memory.getPrestige());
        cfg.set("stats.booster",memory.getActiveBoostString());
        cfg.set("stats.hasbooster",memory.getBoostersString());
        cfg.set("stats.rank",memory.getRank());
        cfg.set("stats.skillpoints",memory.getSP());
        cfg.set("stats.spshop",memory.getSPShopString());
        cfg.set("stats.location.world", player.getWorld().getName());
        cfg.set("stats.location.x", player.getLocation().getX());
        cfg.set("stats.location.y", player.getLocation().getY());
        cfg.set("stats.location.z", player.getLocation().getZ());
        cfg.set("stats.location.yaw", player.getLocation().getYaw());
        cfg.set("stats.location.pitch", player.getLocation().getPitch());
        PrestigeLeaderboard.updateLeaderboard(event.getPlayer());
        PvCommand.saveAllInventories(player.getUniqueId());
        Rcrate.saveRewards(player.getUniqueId());
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PlayerDataManager.setPlayerData(event.getPlayer(), null);
    }

    private String getLuckPermsPrefix(Player player) {
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData metaData = user.getCachedData().getMetaData(api.getContextManager().getQueryOptions(player));
        String prefix = metaData.getPrefix();
        return prefix == null ? "" : prefix;
    }

    public static String extractColorFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "&f"; // fallback white

        int bracketEnd = prefix.indexOf('[');
        if (bracketEnd != -1) {
            String afterBracket = prefix.substring(bracketEnd + 1);

            int hexIndex = afterBracket.indexOf("&#");
            if (hexIndex != -1 && hexIndex + 8 <= afterBracket.length()) {
                return afterBracket.substring(hexIndex, hexIndex + 8);
            }

            int ampIndex = afterBracket.indexOf('&');
            if (ampIndex != -1 && ampIndex + 1 < afterBracket.length()) {
                return afterBracket.substring(ampIndex, ampIndex + 2);
            }
        }

        return "&f";
    }


}

