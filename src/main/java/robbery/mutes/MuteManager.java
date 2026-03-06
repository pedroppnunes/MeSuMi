package robbery.mutes;

import org.bukkit.configuration.file.YamlConfiguration;
import robbery.core.Robbery;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Manages player mutes in the Robbery plugin.
 * <p>
 * This class handles muting, unmuting, and checking mute status for players.
 * Each player has a dedicated "mute.yml" file stored under "player/uuid/mute.yml".
 * <p>
 * Supported duration formats:
 * <ul>
 *     <li>s: seconds</li>
 *     <li>m: minutes</li>
 *     <li>h: hours</li>
 *     <li>d: days</li>
 *     <li>w: weeks</li>
 *     <li>y: years</li>
 * </ul>
 * Example: "10m" = 10 minutes, "2h" = 2 hours, "1d" = 1 day.
 */
public class MuteManager {

    private final Robbery plugin;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new MuteManager instance.
     *
     * @param plugin The main Robbery plugin instance.
     */
    public MuteManager(Robbery plugin) {
        this.plugin = plugin;
    }

    /**
     * Mutes a player for a specified duration.
     *
     * @param uuid        The UUID of the player to mute.
     * @param issuer      The name of the person issuing the mute.
     * @param durationRaw Duration string (e.g., "10m", "2h").
     * @param reason      Optional reason for the mute. If null, defaults to "No reason given".
     */
    public void mutePlayer(UUID uuid, String issuer, String durationRaw, @Nullable String reason) {
        long now = System.currentTimeMillis();
        File file = getFile(uuid);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        Duration duration;
        try {
            duration = parseDuration(durationRaw);
        } catch (IllegalArgumentException e) {
            duration = Duration.ofMinutes(60);
            plugin.getLogger().warning("Invalid mute duration for " + uuid + ": '" + durationRaw + "' (defaulting to 10m)");
        }

        long expireMillis = now + duration.toMillis();

        cfg.set("muted", true);
        cfg.set("issuer", issuer);
        cfg.set("reason", reason == null ? "No reason given" : reason);
        cfg.set("timestamp", DATE_FMT.format(new Date(now)));
        cfg.set("duration", durationRaw);
        cfg.set("expires_at", DATE_FMT.format(new Date(expireMillis)));

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks whether a player is currently muted.
     * Automatically un-mutes the player if the mute duration has expired.
     *
     * @param uuid The player's UUID.
     * @return True if the player is muted, false otherwise.
     */
    public boolean isMuted(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.getBoolean("muted", false)) return false;

        String expires = cfg.getString("expires_at");
        try {
            Date expiresAt = DATE_FMT.parse(expires);
            if (System.currentTimeMillis() >= expiresAt.getTime()) {
                unmutePlayer(uuid);
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Unmutes a player immediately.
     *
     * @param uuid The UUID of the player to unmute.
     */
    public void unmutePlayer(UUID uuid) {
        File file = getFile(uuid);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("muted", false);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves formatted mute information for a player.
     *
     * @param uuid The player's UUID.
     * @return Formatted string containing issuer, reason, and expiration, or null if no mute exists.
     */
    public String getMuteInfo(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return null;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return String.format("Muted by: %s\nReason: %s\nExpires at: %s",
                cfg.getString("issuer"),
                cfg.getString("reason"),
                cfg.getString("expires_at"));
    }

    /**
     * Retrieves the File object for a player's mute data.
     * Creates parent directories if they do not exist.
     *
     * @param uuid The player's UUID.
     * @return File object pointing to the player's mute.yml.
     */
    private File getFile(UUID uuid) {
        File f = new File(plugin.getDataFolder(), "player/" + uuid + "/mute.yml");
        f.getParentFile().mkdirs();
        return f;
    }

    /**
     * Parses a duration string into a Duration object.
     *
     * @param input Duration string (e.g., "10m", "2h", "1d").
     * @return Duration object representing the parsed duration.
     * @throws IllegalArgumentException If the input is in an invalid format.
     */
    private Duration parseDuration(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Empty duration");
        if (input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("permanent"))
            return Duration.ofDays(365 * 100); // 100 years as "permanent"

        if (!input.matches("\\d+[smhdwy]"))
            throw new IllegalArgumentException("Invalid duration format: " + input);

        int value = Integer.parseInt(input.replaceAll("[^\\d]", ""));
        char unit = input.charAt(input.length() - 1);

        return switch (unit) {
            case 's' -> Duration.ofSeconds(value);
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            case 'w' -> Duration.ofDays(7L * value);
            case 'y' -> Duration.ofDays(365L * value);
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
    }

}
