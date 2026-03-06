package robbery.leaderboard;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class HourlyLeaderboard {

    private final JavaPlugin plugin;
    private final String webhookUrl;
    private final File storageFile;
    private String messageId;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public HourlyLeaderboard(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;

        this.webhookUrl = webhookUrl.split("\\?")[0];

        this.storageFile = new File(plugin.getDataFolder(), "leaderboard-hourly.yml");

        loadMessageId();
        startScheduler();
    }

    /* ---------------- Scheduler ---------------- */

    private void startScheduler() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::generateAndUpdate,
                20L * 60 * 2,
                20L * 3600
        );
    }

    /* ---------------- Main Logic ---------------- */

    private void generateAndUpdate() {
        try {
            List<String> lines = buildTop5Lines();
            if (lines.isEmpty()) return;

            String content = buildMessage(lines);

            if (messageId == null) {
                createNewMessage(content);
                return;
            }

            int result = editMessage(content);

            if (result == 404) {
                plugin.getLogger().warning("Message not found, recreating...");
                createNewMessage(content);
            } else if (result != 200) {
                plugin.getLogger().warning("Edit failed (code " + result + "), skipping recreate.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------- Leaderboard ---------------- */

    private List<String> buildTop5Lines() {
        try {
            return SuperiorSkyblockAPI.getGrid().getIslands().stream()
                    .sorted(Comparator.comparing(Island::getIslandLevel).reversed())
                    .limit(5)
                    .map(island -> {
                        String name = island.getName() == null ? "Unknown" : island.getName();
                        BigDecimal level = island.getIslandLevel();
                        return name + " - Level " + level.toPlainString();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch islands");
            return Collections.emptyList();
        }
    }

    private String buildMessage(List<String> lines) {
        StringBuilder sb = new StringBuilder();

        sb.append("**Robbery - Top 5 Hideouts** (updated ")
                .append(LocalDateTime.now().format(FORMATTER))
                .append(")\n\n");

        for (int i = 0; i < lines.size(); i++) {
            sb.append(i + 1).append(". ").append(lines.get(i)).append("\n");
        }

        return sb.toString();
    }

    /* ---------------- Discord ---------------- */

    private void createNewMessage(String content) {
        try {
            URL url = new URL(webhookUrl + "?wait=true");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String json = "{\"content\":\"" + escape(content) + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String response = read(conn.getInputStream());

                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                messageId = obj.get("id").getAsString();

                saveMessageId();

                plugin.getLogger().info("Created message ID: " + messageId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return HTTP code (200 success, 404 not found, etc)
     */
    private int editMessage(String content) {
        try {
            String urlStr = webhookUrl + "/messages/" + messageId;

            String json = "{\"content\":\"" + escape(content) + "\"}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest request =
                    java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(urlStr))
                            .method("PATCH",
                                    java.net.http.HttpRequest.BodyPublishers.ofString(json))
                            .header("Content-Type", "application/json")
                            .build();

            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            plugin.getLogger().info("Edit response: " + response.statusCode());

            return response.statusCode();

        } catch (Exception e) {
            e.printStackTrace();
            return 500;
        }
    }

    /* ---------------- Storage ---------------- */

    private void loadMessageId() {
        if (!storageFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
        messageId = cfg.getString("discord.message-id");
    }

    private void saveMessageId() {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
            cfg.set("discord.message-id", messageId);
            cfg.save(storageFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------- Utils ---------------- */

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder out = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }
}