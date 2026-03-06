package robbery.votes;

import com.bencodez.votingplugin.events.PlayerVoteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import robbery.core.Robbery;

/**
 * Listener for player voting events.
 * <p>
 * This class handles votes from the VotingPlugin (PlayerVoteEvent) and triggers
 * the VotePartyManager in the main Robbery plugin to handle vote progress.
 */
public class VoteListener implements Listener {

    /** Reference to the main plugin instance. */
    private final Robbery plugin;

    /**
     * Constructs a new VoteListener.
     *
     * @param plugin The main Robbery plugin instance.
     */
    public VoteListener(Robbery plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a player votes using the VotingPlugin.
     * <p>
     * This method notifies the VotePartyManager to handle the vote progress.
     *
     * @param event The PlayerVoteEvent triggered by the VotingPlugin.
     */
    @EventHandler
    public void onVote(PlayerVoteEvent event) {
        plugin.getVotePartyManager().handleVote();
    }
}
