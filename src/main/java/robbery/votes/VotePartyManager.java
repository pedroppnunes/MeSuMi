package robbery.votes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import robbery.Robbery;
import robbery.messages.Messages;

/**
 * Manages vote party system for the robbery plugin.
 * Tracks player votes and triggers rewards when a certain vote threshold is reached.
 * A vote party is triggered every time the cumulative votes reach a multiple of the required votes.
 *
 * <p>This system:
 * <ul>
 *   <li>Persists vote counts between server restarts</li>
 *   <li>Automatically triggers rewards when threshold is reached</li>
 *   <li>Provides reset functionality for vote counting</li>
 *   <li>Broadcasts messages to all online players</li>
 * </ul>
 *
 * @see Robbery
 * @see Messages
 */
public class VotePartyManager {
    private final Robbery plugin;
    private int currentVotes = 0;
    private final int requiredVotes = 50;

    /**
     * Constructs a new VotePartyManager and loads the saved vote count from configuration.
     *
     * @param plugin the main plugin instance
     */
    public VotePartyManager(Robbery plugin) {
        this.plugin = plugin;
        loadVotes();
    }

    /**
     * Handles a new vote by incrementing the vote counter and checking if a vote party should be triggered.
     * Saves the updated vote count to configuration and triggers a vote party when the required votes threshold is reached.
     * The vote party is triggered every time the cumulative votes reach a multiple of the required votes.
     */
    public void handleVote() {
        currentVotes++;
        saveVotes();
        if (currentVotes % requiredVotes == 0) {
            triggerVoteParty();
        }
    }

    /**
     * Triggers a vote party by broadcasting a message to all players and giving each online player a vote crate key.
     * Executed via console command to ensure proper permission handling.
     */
    private void triggerVoteParty() {
        Bukkit.broadcastMessage(Messages.get("voteparty.reached"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate key give " + player.getName() + " vote 1");
        }
    }

    /**
     * Saves the current vote count to the plugin configuration.
     * The votes are stored under the "voteparty.votes" path.
     */
    private void saveVotes() {
        plugin.getConfig().set("voteparty.votes", currentVotes);
        plugin.saveConfig();
    }

    /**
     * Loads the vote count from the plugin configuration.
     * If no votes are found in configuration, defaults to 0.
     */
    private void loadVotes() {
        currentVotes = plugin.getConfig().getInt("voteparty.votes", 0);
    }

    /**
     * Resets the vote count to the remainder after dividing by the required votes.
     * This preserves any votes that have accumulated towards the next vote party while
     * effectively resetting the counter. Broadcasts a reset message to all players.
     *
     * <p>Example: If required votes is 50 and current votes is 125, after reset current votes will be 25.
     */
    public void resetVotes() {
        currentVotes = currentVotes % requiredVotes;
        saveVotes();
        Bukkit.broadcastMessage(Messages.get("voteparty.reset"));
    }
}