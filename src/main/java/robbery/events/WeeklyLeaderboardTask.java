package robbery.events;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.eduardomcb.discord.webhook.WebhookClient;
import com.eduardomcb.discord.webhook.WebhookManager;
import com.eduardomcb.discord.webhook.models.Message;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Task that manages the weekly leaderboard of SuperiorSkyblock islands.
 * <p>
 * This task automatically checks every minute if it is Friday 22:00 and,
 * if so, generates the top 5 islands by level, saves the results to a
 * YAML file, and sends the leaderboard to a configured Discord webhook.
 */
public class WeeklyLeaderboardTask {

    /** The main plugin instance. */
    private final JavaPlugin plugin;

    /** Discord webhook URL for sending the leaderboard. */
    private final String webhookUrl;

    /**
     * Creates a new WeeklyLeaderboardTask and starts the scheduler.
     *
     * @param plugin     The main plugin instance.
     * @param webhookUrl The Discord webhook URL to send the leaderboard.
     */
    public WeeklyLeaderboardTask(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
        startScheduler();
    }

    /**
     * Starts an asynchronous repeating scheduler that checks every minute
     * if it's time to generate the weekly leaderboard.
     */
    private void startScheduler() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.getDayOfWeek() == DayOfWeek.FRIDAY && now.getHour() == 22 && now.getMinute() == 0) {
                generateAndSendLeaderboard();
            }
        }, 20L, 1200L);
    }

    /**
     * Generates the weekly leaderboard of the top 5 islands and performs the following:
     * <ul>
     *     <li>Saves the leaderboard to a local YAML file.</li>
     *     <li>Sends the leaderboard to the configured Discord webhook.</li>
     * </ul>
     */
    public void generateAndSendLeaderboard() {
        List<Island> topIslands = SuperiorSkyblockAPI.getGrid().getIslands().stream()
                .sorted(Comparator.comparing(Island::getIslandLevel).reversed())
                .limit(5)
                .toList();

        List<String> lines = new ArrayList<>();
        int pos = 1;
        for (Island island : topIslands) {
            String name = island.getName() != null ? island.getName() : "Unknown";
            BigDecimal level = island.getIslandLevel();
            lines.add(pos + ". " + name + " - Level " + level.toPlainString());
            pos++;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = LocalDateTime.now().format(formatter);

        StringBuilder discordMsg = new StringBuilder("```");
        discordMsg.append("Robbery - ").append(formattedDate).append("\n");
        discordMsg.append("Weekly Top 5 Hideouts\n");
        for (String line : lines) {
            discordMsg.append(line).append("\n");
        }
        discordMsg.append("```");

        saveToYaml(lines);
        sendToDiscord(discordMsg.toString());
    }

    /**
     * Saves the leaderboard to a YAML file in the plugin's data folder.
     *
     * @param lines List of leaderboard entries.
     */
    private void saveToYaml(List<String> lines) {
        try {
            File file = new File(plugin.getDataFolder(), "leaderboards.yml");
            if (!file.exists()) file.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String dateKey = LocalDate.now().toString();

            config.set("leaderboards." + dateKey, lines);
            config.save(file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends the generated leaderboard to the configured Discord webhook.
     *
     * @param message The message content to send.
     */
    private void sendToDiscord(String message) {
        Message discordMessage = new Message().setContent(message);

        WebhookManager manager = new WebhookManager()
                .setChannelUrl(webhookUrl)
                .setMessage(discordMessage)
                .setListener(new WebhookClient.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        plugin.getLogger().info("Discord message sent successfully.");
                    }

                    @Override
                    public void onFailure(int code, String message) {
                        plugin.getLogger().warning("Failed to send Discord message: " + message);
                    }

                    @Override
                    public void onError(Exception exception) {
                        plugin.getLogger().warning("Failed to send Discord message: " + message);
                    }
                });

        manager.exec();
    }
}
