package robbery.events;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HourlyLeaderboard
 *
 * - Builds top 5 islands by level using SuperiorSkyblock2 API.
 * - Sends a single message to a Discord webhook and edits that same message once per hour.
 * - Persists the messageId to plugin's data folder in leaderboard.yml under "discord.message-id".
 */
public class HourlyLeaderboard {

    private final JavaPlugin plugin;
    private final String webhookUrl; // full webhook url: https://discord.com/api/webhooks/{id}/{token}
    private final File storageFile;
    private String messageId; // persisted message id

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Create and start the hourly updater.
     *
     * @param plugin     your plugin instance
     * @param webhookUrl discord webhook url (full)
     */
    public HourlyLeaderboard(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = Objects.requireNonNull(webhookUrl, "webhookUrl");
        this.storageFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        loadMessageId();
        startScheduler();
    }

    /* ---------------------------
       Scheduler
       --------------------------- */
    private void startScheduler() {
        // Run once immediately, then every hour (3600 seconds)
        long initialDelay = 0L;
        long intervalTicks = 20L * 3600; // 1 hour

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::generateAndUpdate, initialDelay, intervalTicks);
        plugin.getLogger().info("HourlyLeaderboard scheduled (every 1 hour).");
    }

    /* ---------------------------
       Main flow
       --------------------------- */
    private void generateAndUpdate() {
        try {
            List<String> lines = buildTop5Lines();
            if (lines.isEmpty()) {
                plugin.getLogger().info("HourlyLeaderboard: no islands found.");
                return;
            }

            String content = buildMessageContent(lines);

            if (messageId == null || messageId.isEmpty()) {
                // Create initial message and save message id
                plugin.getLogger().info("HourlyLeaderboard: creating initial webhook message.");
                String createdId = createWebhookMessage(content);
                if (createdId != null) {
                    messageId = createdId;
                    saveMessageId();
                    plugin.getLogger().info("HourlyLeaderboard: created message id " + messageId);
                } else {
                    plugin.getLogger().warning("HourlyLeaderboard: failed to create webhook message.");
                }
            } else {
                // Try to edit existing message
                boolean ok = editWebhookMessage(content, messageId);
                if (!ok) {
                    plugin.getLogger().warning("HourlyLeaderboard: failed to edit message (will attempt recreate).");
                    // attempt recreate and persist new id
                    String createdId = createWebhookMessage(content);
                    if (createdId != null) {
                        messageId = createdId;
                        saveMessageId();
                        plugin.getLogger().info("HourlyLeaderboard: recreated message id " + messageId);
                    }
                } else {
                    plugin.getLogger().info("HourlyLeaderboard: successfully edited message id " + messageId);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("HourlyLeaderboard: exception while updating leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ---------------------------
       Build leaderboard message
       --------------------------- */
    private List<String> buildTop5Lines() {
        try {
            List<Island> top = SuperiorSkyblockAPI.getGrid().getIslands().stream()
                    .sorted(Comparator.comparing(Island::getIslandLevel).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            List<String> lines = new ArrayList<>();
            int pos = 1;
            for (Island island : top) {
                String name = island.getName() != null ? island.getName() : "Unknown";
                BigDecimal level = island.getIslandLevel();
                String line = String.format("%d. %s - Level %s", pos, name, level.toPlainString());
                lines.add(line);
                pos++;
            }
            return lines;
        } catch (Exception e) {
            plugin.getLogger().warning("HourlyLeaderboard: failed to collect islands: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildMessageContent(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 **Robbery - Top 5 Hideouts** (updated ").append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append(")\n\n");
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        sb.append("\n_Last updated hourly_");
        return sb.toString();
    }

    /* ---------------------------
       Persistence of messageId
       --------------------------- */
    private void loadMessageId() {
        try {
            if (!storageFile.exists()) {
                // ensure parent folder exists
                storageFile.getParentFile().mkdirs();
                // no file yet
                return;
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
            this.messageId = cfg.getString("discord.message-id", null);
            if (this.messageId != null) {
                plugin.getLogger().info("HourlyLeaderboard: loaded message-id " + this.messageId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("HourlyLeaderboard: failed to load messageId: " + e.getMessage());
        }
    }

    private void saveMessageId() {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
            cfg.set("discord.message-id", this.messageId);
            cfg.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("HourlyLeaderboard: failed to save messageId: " + e.getMessage());
        }
    }

    /* ---------------------------
       Discord webhook helpers (HTTP)
       --------------------------- */

    /**
     * Create webhook message (POST). Returns message id on success, null on failure.
     * Uses webhookUrl + "?wait=true" to receive the created message object in response.
     */
    private String createWebhookMessage(String content) {
        HttpURLConnection conn = null;
        try {
            // append wait to get message object back
            String urlString = webhookUrl;
            if (!urlString.contains("?")) {
                urlString = urlString + "?wait=true";
            } else {
                urlString = urlString + "&wait=true";
            }

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "Robbery-Plugin");

            String json = "{\"content\":\"" + escapeJson(content) + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                String response = readStream(conn.getInputStream());
                String id = extractIdFromJson(response);
                return id;
            } else {
                String err = readStream(conn.getErrorStream());
                plugin.getLogger().warning("HourlyLeaderboard: create webhook returned code " + code + " - " + err);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("HourlyLeaderboard: create webhook failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Edit existing webhook message (PATCH). Returns true on success.
     */
    private boolean editWebhookMessage(String content, String messageId) {
        HttpURLConnection conn = null;
        try {
            String base = webhookUrl;
            // ensure no trailing slash issues: webhookUrl already includes id/token
            String editUrl = base.endsWith("/") ? base + "messages/" + messageId : base + "/messages/" + messageId;
            URL url = new URL(editUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "Robbery-Plugin");

            String json = "{\"content\":\"" + escapeJson(content) + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                // success
                return true;
            } else {
                String err = readStream(conn.getErrorStream());
                plugin.getLogger().warning("HourlyLeaderboard: edit webhook returned code " + code + " - " + err);
                // if 404 -> message deleted, caller will recreate
            }
        } catch (Exception e) {
            plugin.getLogger().warning("HourlyLeaderboard: edit webhook failed: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    /* ---------------------------
       Small JSON / IO helpers
       --------------------------- */

    private static String escapeJson(String s) {
        if (s == null) return "";
        // minimal escaping for quotes, backslashes and newlines
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Naive extraction of the "id" field from Discord JSON response.
     * The POST ?wait=true returns the created message as JSON containing "id":"12345"
     */
    private static String extractIdFromJson(String json) {
        if (json == null) return null;
        String marker = "\"id\":\"";
        int idx = json.indexOf(marker);
        if (idx == -1) return null;
        int start = idx + marker.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}