package robbery.votes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.messages.Messages;

import java.time.LocalDate;
import java.util.Random;

/**
 * Manages vote party system for the robbery plugin.
 * Tracks player votes and triggers rewards when a certain vote threshold is reached.
 * A vote party is triggered every time the cumulative votes reach a multiple of the required votes.
 */
public class VotePartyManager {
    private final Robbery plugin;
    private int currentVotes = 0;
    private final int requiredVotes = 50;
    private int totalVotes = 0;
    private String lastResetDate = "";

    public VotePartyManager(Robbery plugin) {
        this.plugin = plugin;
        loadVotes();
        checkDailyReset();
    }

    private void checkDailyReset() {
        String currentDate = LocalDate.now().toString();
        if (!currentDate.equals(lastResetDate)) {
            resetVotes();
            lastResetDate = currentDate;
            saveVotes();
        }
    }

    public void handleVote() {
        checkDailyReset(); // Check in case the day changes while the server is running
        currentVotes++;
        totalVotes++;

        if (currentVotes >= requiredVotes) {
            triggerVoteParty();
            currentVotes = 0;
        }

        saveVotes();
    }

    private void triggerVoteParty() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage(Messages.get("voteparty.reached"));
            Random random = new Random();

            for (Player player : Bukkit.getOnlinePlayers()) {
                double chance = random.nextDouble() * 100.0;
                String command;

                if (chance < 0.5) {
                    command = "crates key give " + player.getName() + " legendary";
                } else if (chance < 2.5) { // 0.5 + 2.0 = 2.5%
                    command = "crates key give " + player.getName() + " epic";
                } else if (chance < 12.5) { // 2.5 + 10.0 = 12.5%
                    command = "crates key give " + player.getName() + " vote 2";
                } else {
                    command = "crates key give " + player.getName() + " vote 1";
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }

    private void saveVotes() {
        plugin.getConfig().set("voteparty.votes", currentVotes);
        plugin.getConfig().set("voteparty.totalVotes", totalVotes);
        plugin.getConfig().set("voteparty.lastReset", lastResetDate);
        plugin.saveConfig();
    }

    private void loadVotes() {
        currentVotes = plugin.getConfig().getInt("voteparty.votes", 0);
        totalVotes = plugin.getConfig().getInt("voteparty.totalVotes", 0);
        lastResetDate = plugin.getConfig().getString("voteparty.lastReset", "");
    }

    public void resetVotes() {
        currentVotes = currentVotes % requiredVotes;
        totalVotes = currentVotes;
        lastResetDate = LocalDate.now().toString();
        saveVotes();
        Bukkit.broadcastMessage(Messages.get("voteparty.reset"));
    }

    public int getDisplayCurrentVotes() {
        checkDailyReset();
        return totalVotes;
    }

    public int getDisplayRequiredVotes() {
        checkDailyReset();
        int nextMilestone = ((totalVotes / requiredVotes) + 1) * requiredVotes;
        return nextMilestone;
    }
}