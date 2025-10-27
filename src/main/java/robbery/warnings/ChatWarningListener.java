package robbery.warnings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import robbery.Robbery;
import robbery.messages.Messages;
import robbery.mutes.MuteManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Listener for chat events that detects and handles spam behavior in player chat.
 * Implements a strike-based system for dealing with repeated messages and character spam.
 *
 * <p>This system monitors chat for the following spam patterns:
 * <ul>
 *   <li>Repeated characters (7 or more of the same character in sequence)</li>
 *   <li>Identical message repetition within a short timeframe</li>
 * </ul>
 *
 * <p>The punishment system works on a strike basis:
 * <ul>
 *   <li>Strikes 1-2: Warning messages</li>
 *   <li>Strike 3: Final warning</li>
 *   <li>Strike 4+: 3-hour mute and formal warning</li>
 * </ul>
 *
 * <p>Strikes automatically decay after 15 minutes of no violations.
 *
 * @see Listener
 * @see MuteManager
 * @see robbery.warnings.WarningManager
 */
public class ChatWarningListener implements Listener {

    private static final Map<UUID, Deque<String>> lastMessages = new HashMap<>();
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{6,}"); // 7+ repeated chars

    private final Map<UUID, String> lastMessage = new HashMap<>();
    private final Map<UUID, Integer> repeatCount = new HashMap<>();
    private final Map<UUID, Integer> spamStrikes = new HashMap<>();

    private final Robbery plugin;
    private final MuteManager muteManager;

    private static final long STRIKE_DECAY_TICKS = TimeUnit.MINUTES.toSeconds(15) * 20;

    /**
     * Constructs a new ChatWarningListener with the required dependencies.
     *
     * @param plugin the main plugin instance
     */
    public ChatWarningListener(Robbery plugin) {
        this.plugin = plugin;
        this.muteManager = plugin.getMuteManager();
    }

    /**
     * Handles asynchronous player chat events to detect and prevent spam.
     * Checks for muted players, repeated characters, and message repetition.
     * Applies strikes when spam patterns are detected.
     *
     * @param event the AsyncPlayerChatEvent to process
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (muteManager.isMuted(uuid)) {
            event.setCancelled(true);
            Messages.send(player, "chat.muted");
            return;
        }

        String msg = ChatColor.stripColor(event.getMessage()).trim().toLowerCase();

        Deque<String> history = lastMessages.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        if (history.size() >= 10) history.removeFirst();
        history.addLast(msg);

        if (REPEATED_CHAR_PATTERN.matcher(msg).find()) {
            applyStrike(player);
            return;
        }

        String last = lastMessage.get(uuid);
        if (msg.equals(last)) {
            int count = repeatCount.getOrDefault(uuid, 1) + 1;
            if (count >= 3) {
                applyStrike(player);
                repeatCount.put(uuid, 0);
            } else {
                repeatCount.put(uuid, count);
            }
        } else {
            repeatCount.put(uuid, 1);
        }
        lastMessage.put(uuid, msg);
    }

    /**
     * Applies a spam strike to the player and handles the appropriate response based on strike count.
     * Strike counts automatically decay after 15 minutes.
     *
     * @param player the player to apply the strike to
     */
    private void applyStrike(Player player) {
        UUID uuid = player.getUniqueId();

        int strikes = spamStrikes.getOrDefault(uuid, 0) + 1;
        spamStrikes.put(uuid, strikes);

        switch (strikes) {
            case 1, 2 -> Messages.sendFormatted(player, "chat.spam.warning", "count", String.valueOf(strikes));
            case 3 -> Messages.send(player, "chat.spam.final_warning");
            default -> {
                muteAndWarn(player);
                spamStrikes.put(uuid, 0);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int current = spamStrikes.getOrDefault(uuid, 0);
            if (current > 0) spamStrikes.put(uuid, current - 1);
        }, STRIKE_DECAY_TICKS);
    }

    /**
     * Mutes the player for 3 hours and issues a formal warning for spamming.
     * Also displays the warning GUI to the player.
     * Only applies if the player is not already muted.
     *
     * @param player the player to mute and warn
     */
    private void muteAndWarn(Player player) {
        UUID uuid = player.getUniqueId();

        if (muteManager.isMuted(uuid)) return;

        muteManager.mutePlayer(uuid, "Server", "3h","Spamming in chat");

        plugin.getWarningManager().addWarning(uuid, "Spamming in chat", "Server", "12h");

        Bukkit.getScheduler().runTask(plugin, () -> {
            Messages.send(player, "chat.spam.muted");
            plugin.getWarningManager().sendWarningGUI(player, "Spamming in chat", "Server", "12h");
        });
    }

    /**
     * Retrieves the last 10 messages sent by a player for moderation purposes.
     *
     * @param uuid the UUID of the player to get message history for
     * @return a list containing the player's recent messages, newest last
     */
    public static List<String> getLastMessages(UUID uuid) {
        return new ArrayList<>(lastMessages.getOrDefault(uuid, new ArrayDeque<>()));
    }
}