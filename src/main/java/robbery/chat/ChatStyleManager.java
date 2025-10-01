package robbery.chat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages per-player chat styles, including color and bold formatting.
 * <p>
 * Player chat styles are stored in a YAML file ("chatcolors.yml") inside the plugin's data folder.
 * Provides methods to get/set color and bold settings, as well as ensuring default values
 * for new players.
 * </p>
 */
public class ChatStyleManager {

    private final File file;
    private final YamlConfiguration yml;

    /**
     * Initializes a ChatStyleManager with the given plugin data folder.
     *
     * @param dataFolder the plugin's data folder where "chatcolors.yml" will be stored
     */
    public ChatStyleManager(File dataFolder) {
        this.file = new File(dataFolder, "chatcolors.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Sets the chat color for a specific player.
     *
     * @param uuid            the UUID of the player
     * @param bukkitColorName the Bukkit chat color name (e.g., "RED", "BLUE", etc.)
     */
    public void setColor(UUID uuid, String bukkitColorName) {
        yml.set("players." + uuid + ".color", bukkitColorName);
        save();
    }

    /**
     * Retrieves the chat color for a specific player.
     *
     * @param uuid the UUID of the player
     * @return an {@link Optional} containing the player's color, or "&7" (gray) if not set
     */
    public Optional<String> getColor(UUID uuid) {
        String val = yml.getString("players." + uuid + ".color", null);
        return Optional.of(val != null ? val : "&7");
    }

    /**
     * Sets whether the player's chat text should be bold.
     *
     * @param uuid the UUID of the player
     * @param bold true to make text bold, false otherwise
     */
    public void setBold(UUID uuid, boolean bold) {
        yml.set("players." + uuid + ".bold", bold);
        save();
    }

    /**
     * Ensures that a player has default chat style values in the configuration.
     * <p>
     * If the player does not exist in the file, this method initializes their color and bold status.
     * Default color is WHITE if the player has a rank, otherwise GRAY. Bold is set to false.
     * </p>
     *
     * @param player  the player to ensure in the configuration
     * @param hasRank true if the player has a rank, false otherwise
     */
    public void ensurePlayerExists(Player player, boolean hasRank) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid;

        if (!yml.contains(path)) {
            String defaultColor = hasRank ? "WHITE" : "GRAY";
            yml.set(path + ".color", defaultColor);
            yml.set(path + ".bold", false);
            save();
        }
    }

    /**
     * Checks if a player's chat text is set to bold.
     *
     * @param uuid the UUID of the player
     * @return true if bold, false otherwise
     */
    public boolean isBold(UUID uuid) {
        return yml.getBoolean("players." + uuid + ".bold", false);
    }

    /**
     * Saves the YAML configuration to the file.
     * <p>
     * Any IOException during saving is silently ignored.
     * </p>
     */
    private void save() {
        try {
            yml.save(file);
        } catch (IOException ignored) {}
    }
}
