package robbery.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.keys.KeyManager;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.player.PrestigeLeaderboard;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the /prestige command, allowing players to prestige once
 * they meet the requirements of the last store and have enough money.
 *
 * <p>When a player prestiges:
 * <ul>
 *     <li>All money is withdrawn.</li>
 *     <li>Prestige level is incremented.</li>
 *     <li>Backpack is emptied.</li>
 *     <li>Keys are reset for the new prestige.</li>
 *     <li>Player is removed from all store regions.</li>
 *     <li>Fireworks are displayed at predefined locations.</li>
 *     <li>A broadcast announces the player's prestige.</li>
 * </ul>
 *
 */
public class Prestige implements CommandExecutor {

    private final List<Location> fireworkLocations = Arrays.asList(
            new Location(Bukkit.getWorld("world"), 20059, 110, 20015),
            new Location(Bukkit.getWorld("world"), 20130, 110, 20015),
            new Location(Bukkit.getWorld("world"), 200285, 110, 20053),
            new Location(Bukkit.getWorld("world"), 200285, 110, 20145),
            new Location(Bukkit.getWorld("world"), 200229, 110, 200180),
            new Location(Bukkit.getWorld("world"), 20170, 110, 20180),
            new Location(Bukkit.getWorld("world"), 20120, 110, 20180),
            new Location(Bukkit.getWorld("world"), 20059, 110, 20180)
    );

    private static final int basePrestige = 1_500_000_000;

    /**
     * Constructs a Prestige command instance.
     *
     * @param main the plugin instance
     */
    public Prestige(Robbery main) {
    }

    /**
     * Executes the /prestige command.
     *
     * <p>Players can specify another player's name as an argument
     * if executed from the console. Requirements:
     * <ul>
     *     <li>Player must be in the last store for their prestige level.</li>
     *     <li>Player must have enough balance multiplied by their prestige boost.</li>
     * </ul>
     *
     *
     * @param sender the command sender
     * @param command the command object
     * @param label the command label used
     * @param args optional arguments: [playerName]
     * @return true always
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;

        if (args.length > 0) {
            player = Bukkit.getPlayer(args[0]);
            if (player == null || !player.isOnline() || !player.getWorld().getName().equals("world")) {
                Messages.send(sender, "global.player-not-found");
                return true;
            }
        }

        if (player == null) {
            if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                Messages.send(sender, "command.prestige.console-no-player");
                return true;
            }
        }

        PlayerData p = PlayerDataManager.getPlayerData(player);
        Economy econ = Robbery.getEconomy();
        double balance = econ.getBalance(player);
        int prestige = p.getPrestige();
        double prestigeboost = p.getPrestigeBoost();

        if (balance >= getPrestigeValue(p) * prestigeboost && isInLastStore(prestige, p)) {
            // Remove all money
            econ.withdrawPlayer(player, econ.getBalance(player));
            // Increment prestige
            p.setPrestige(prestige + 1);
            // Reset backpack and keys
            p.getBackpack().emptyBackpack();
            p.prestigeKeys();
            PrestigeLeaderboard.updateLeaderboard(player);
            // Remove from store regions
            KeyManager.removePlayerFromAllRegions(player);

            // Broadcast prestige message
            String border = Messages.colorize("&c&l■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
            String msg = Messages.colorize("&e&l" + player.getDisplayName() + " &e&lhas prestiged to &6Prestige " + (prestige + 1) + "&e&l!");
            Bukkit.broadcastMessage(border);
            Bukkit.broadcastMessage(msg);
            Bukkit.broadcastMessage(border+"\u200B");

            // Teleport player to prestige start
            player.teleport(new Location(player.getWorld(), 20025, 100, 20015));

            // Launch fireworks
            for (Location loc : fireworkLocations) {
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.RED)
                        .withFade(Color.ORANGE)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }
            return true;
        } else if (!isInLastStore(prestige, p)) {
            Messages.sendFormatted(player, "command.prestige.not-in-last-store", "store", lastStoreForPrestige(prestige));
        } else if (balance < getPrestigeValue(p) * prestigeboost) {
            Messages.send(player, "command.prestige.not-enough-money");
        }
        return true;
    }

    /**
     * Returns the prestige value required for the player.
     *
     * @param p the player data
     * @return the required prestige balance
     */
    public static double getPrestigeValue(PlayerData p) {
        int prestige = p.getPrestige();
        if (prestige == 0) return KeyManager.STORE11.getPrice(p) / 1.5;
        else if (prestige == 1) return KeyManager.STORE12.getPrice(p) / 1.5;
        else return basePrestige;
    }

    /**
     * Checks if the player is in the last store for their prestige level.
     *
     * @param prestige current prestige
     * @param p player data
     * @return true if in last store, false otherwise
     */
    private boolean isInLastStore(int prestige, PlayerData p) {
        if (prestige == 0) return p.hasKey("store10");
        else if (prestige == 1) return p.hasKey("store11");
        else return p.hasKey("store12");
    }

    /**
     * Returns the last store's name for the player's current prestige.
     *
     * @param prestige current prestige
     * @return the store name
     */
    private String lastStoreForPrestige(int prestige) {
        if (prestige == 0) return KeyManager.STORE10.getName();
        else if (prestige == 1) return KeyManager.STORE11.getName();
        else return KeyManager.STORE12.getName();
    }
}
