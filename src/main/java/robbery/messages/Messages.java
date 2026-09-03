package robbery.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import robbery.core.Robbery;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Utility class for managing and sending messages from the language.yml file.
 * <p>
 * Supports:
 * <ul>
 *     <li>Standard chat messages to players and command senders.</li>
 *     <li>Formatted messages with placeholders.</li>
 *     <li>Action bars and titles using Adventure API.</li>
 *     <li>Loading and reloading language configuration.</li>
 * </ul>
 */
public class Messages {

    private static FileConfiguration lang;
    private static File cfile;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Initializes the message system by loading language.yml.
     *
     * @param plugin The main plugin instance.
     */
    public static void init(Robbery plugin) {
        File langFile = new File(plugin.getDataFolder(), "language.yml");
        cfile = langFile;
        if (!langFile.exists()) {
            plugin.saveResource("language.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Reloads the language.yml file.
     */
    public static void reload() {
        if (cfile != null) {
            lang = YamlConfiguration.loadConfiguration(cfile);
        }
    }

    /**
     * Retrieves a message from the language file and translates color codes.
     *
     * @param path The path in language.yml.
     * @return The message string.
     */
    public static String get(String path) {
        return ChatColor.translateAlternateColorCodes('&', lang.getString(path, "&cMessage not found: " + path));
    }

    /**
     * Sends a message to a player.
     *
     * @param player The player.
     * @param path   The message path in language.yml.
     */
    public static void send(Player player, String path) {
        player.sendMessage(get(path));
    }

    /**
     * Sends a message to a command sender.
     *
     * @param sender The command sender.
     * @param path   The message path in language.yml.
     */
    public static void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Returns a formatted message with placeholders replaced.
     *
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders and their values.
     * @return Formatted message.
     */
    public static String getFormatted(String path, Map<String, String> placeholders) {
        String msg = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return msg;
    }

    /**
     * Sends a title and subtitle to a player.
     *
     * @param player   The player.
     * @param titlePath    Path for the title message.
     * @param subtitlePath Path for the subtitle message.
     * @param fadeIn   Fade in time in ticks.
     * @param stay     Stay duration in ticks.
     * @param fadeOut  Fade out time in ticks.
     */
    public static void sendTitle(Player player, String titlePath, String subtitlePath, int fadeIn, int stay, int fadeOut) {
        String title = get(titlePath);
        String subtitle = subtitlePath.isEmpty() ? "" : get(subtitlePath);
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Sends a formatted chat message to a player with multiple placeholders.
     *
     * @param player       The player.
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders.
     */
    public static void sendFormatted(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getFormatted(path, placeholders));
    }

    /**
     * Sends a formatted chat message to a command sender with multiple placeholders.
     *
     * @param sender       The command sender.
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders.
     */
    public static void sendFormatted(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getFormatted(path, placeholders);
        sender.sendMessage(message);
    }

    /**
     * Sends a formatted chat message to a player with a single placeholder.
     *
     * @param player          The player.
     * @param path            The path in language.yml.
     * @param placeholderKey  Placeholder key.
     * @param placeholderValue Placeholder value.
     */
    public static void sendFormatted(Player player, String path, String placeholderKey, String placeholderValue) {
        sendFormatted(player, path, Collections.singletonMap(placeholderKey, placeholderValue));
    }

    /**
     * Sends a formatted chat message to a sender with a single placeholder.
     *
     * @param sender          The sender.
     * @param path            The path in language.yml.
     * @param placeholderKey  Placeholder key.
     * @param placeholderValue Placeholder value.
     */
    public static void sendFormatted(CommandSender sender, String path, String placeholderKey, String placeholderValue) {
        sendFormatted(sender, path, Collections.singletonMap(placeholderKey, placeholderValue));
    }

    /**
     * Translates color codes in a string.
     *
     * @param message The message with color codes.
     * @return Message with Bukkit color codes.
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Sends a plain action bar message to a player.
     *
     * @param player The player.
     * @param path   The path in language.yml.
     */
    public static void sendActionBar(Player player, String path) {
        String message = get(path);
        if (path != null && path.contains("progress_bar")) {
            ActionBarManager.sendDirect(player, message);
        } else {
            ActionBarManager.enqueue(player, message);
        }
    }

    /**
     * Sends a formatted action bar message with placeholders.
     *
     * @param player       The player.
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders.
     */
    public static void sendActionBarFormatted(Player player, String path, Map<String, String> placeholders) {
        String message = getFormatted(path, placeholders);
        if (path != null && path.contains("progress_bar")) {
            ActionBarManager.sendDirect(player, message);
        } else {
            ActionBarManager.enqueue(player, message);
        }
    }

    /**
     * Sends a formatted action bar message with a single placeholder.
     *
     * @param player The player.
     * @param path   The path in language.yml.
     * @param key    Placeholder key.
     * @param value  Placeholder value.
     */
    public static void sendActionBarFormatted(Player player, String path, String key, String value) {
        sendActionBarFormatted(player, path, Collections.singletonMap(key, value));
    }

    /**
     * Sends a component-based title to a player.
     *
     * @param player       The player.
     * @param path         Path for the message.
     * @param placeholders Placeholders for formatting.
     */
    public static void sendComponent(Player player, String path, Map<String, String> placeholders) {
        String message = getFormatted(path, placeholders);
        Component title = LegacyComponentSerializer.legacy('&').deserialize(message);
        Component emptySubtitle = Component.empty();
        Title finalTitle = Title.title(title, emptySubtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));
        player.showTitle(finalTitle);
    }

    /**
     * Returns a component for a message path.
     *
     * @param path The path in language.yml.
     * @return Component of the message.
     */
    public static Component getComponent(String path) {
        String raw = lang.getString(path, "&cMessage not found: " + path);
        return LEGACY.deserialize(raw);
    }

    /**
     * Returns a component for a message path with placeholders.
     *
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders.
     * @return Component with placeholders replaced.
     */
    public static Component getComponentFormatted(String path, Map<String, String> placeholders) {
        String raw = lang.getString(path, "&cMessage not found: " + path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return LEGACY.deserialize(raw);
    }

    /**
     * Sends a component message to a player.
     *
     * @param player The player.
     * @param path   The path in language.yml.
     */
    public static void sendComponentMessage(Player player, String path) {
        player.sendMessage(getComponent(path));
    }

    /**
     * Sends a formatted component message to a player.
     *
     * @param player       The player.
     * @param path         The path in language.yml.
     * @param placeholders Map of placeholders.
     */
    public static void sendComponentMessageFormatted(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(getComponentFormatted(path, placeholders));
    }
}
