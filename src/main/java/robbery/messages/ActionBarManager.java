package robbery.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import robbery.core.Robbery;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player Action Bar notification queues.
 * Displays queued messages sequentially with a delay so players can read every notification
 * without text being immediately overwritten or spammed.
 */
public class ActionBarManager {

    private static final Map<UUID, Deque<String>> playerQueues = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSentTime = new ConcurrentHashMap<>();
    private static final long DELAY_MS = 600L; // 0.6s fast display delay between notifications

    public static void init(Robbery plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Deque<String>> entry : playerQueues.entrySet()) {
                    UUID uuid = entry.getKey();
                    Deque<String> queue = entry.getValue();

                    if (queue == null || queue.isEmpty()) continue;

                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        playerQueues.remove(uuid);
                        lastSentTime.remove(uuid);
                        continue;
                    }

                    Long last = lastSentTime.getOrDefault(uuid, 0L);
                    if (now - last >= DELAY_MS) {
                        String msg = queue.pollFirst();
                        if (msg != null) {
                            Component component = LegacyComponentSerializer.legacy('&').deserialize(msg);
                            player.sendActionBar(component);
                            lastSentTime.put(uuid, now);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    public static void enqueue(Player player, String rawMessage) {
        if (player == null || rawMessage == null || rawMessage.isEmpty()) return;
        UUID uuid = player.getUniqueId();
        Deque<String> queue = playerQueues.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        // Limit queue size to 10 to prevent infinite backlog
        if (queue.size() < 10) {
            queue.addLast(rawMessage);
        }
    }

    public static void sendDirect(Player player, String rawMessage) {
        if (player == null || rawMessage == null) return;
        Component component = LegacyComponentSerializer.legacy('&').deserialize(rawMessage);
        player.sendActionBar(component);
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid != null) {
            playerQueues.remove(uuid);
            lastSentTime.remove(uuid);
        }
    }
}
