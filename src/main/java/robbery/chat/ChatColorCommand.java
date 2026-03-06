package robbery.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.core.Robbery;
import robbery.chat.ChatStyleManager;
import robbery.messages.Messages;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the /chatcolor command, allowing players to change their chat text color and
 * toggle bold text, depending on their rank permissions.
 * <p>
 * Players can only select colors allowed by their rank and cannot choose RED or BLACK.
 * Certain ranks also allow toggling bold text. Changes are stored via {@link ChatStyleManager}.
 * </p>
 * <p>
 * Command usage: /chatcolor &lt;player&gt; &lt;color|bold&gt;
 * </p>
 */
public class ChatColorCommand implements CommandExecutor {

    private final ChatStyleManager styles;

    /**
     * Constructs a ChatColorCommand handler.
     *
     * @param plugin the main plugin instance, used to access ChatStyleManager
     */
    public ChatColorCommand(Robbery plugin) {
        this.styles = plugin.getChatStyleManager();
    }

    /**
     * Executes the /chatcolor command.
     *
     * @param sender  the sender of the command
     * @param command the command object
     * @param label   the command alias used
     * @param args    the command arguments: &lt;player&gt; &lt;color|bold&gt;
     * @return true if the command executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length < 2) {
            Messages.send(sender, "command.chatcolor.usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Messages.sendFormatted(sender, "command.chatcolor.player-not-found", "player", args[0]);
            return true;
        }

        String choice = args[1].toLowerCase(Locale.ROOT);

        // Handle bold toggle
        if (choice.equals("bold")) {
            if (target.hasPermission("robbery.rank6") || target.hasPermission("robbery.rank7") || target.hasPermission("robbery.bypass")) {
                boolean current = styles.isBold(target.getUniqueId());
                boolean newState = !current;

                styles.setBold(target.getUniqueId(), newState);

                if (newState) {
                    Messages.send(target, "command.chatcolor.notify-bold-on");
                } else {
                    Messages.send(target, "command.chatcolor.notify-bold-off");
                }
            } else {
                Messages.send(target, "command.chatcolor.no-permission-bold");
            }
            return true;
        }

        // Handle color selection
        ChatColor selected = parseColor(choice);
        if (selected == null || !selected.isColor()) {
            Messages.send(sender, "command.chatcolor.invalid-color");
            return true;
        }

        List<ChatColor> allowed = getAllowedColors(target);
        if (!allowed.contains(selected)) {
            Messages.send(target, "command.chatcolor.no-permission-color");
            return true;
        }

        styles.setColor(target.getUniqueId(), selected.name());
        Messages.sendFormatted(sender, "command.chatcolor.set-color", Map.of(
                "player", target.getName(),
                "color", choice.toUpperCase()
        ));
        Messages.sendFormatted(target, "command.chatcolor.notify-color", "color", choice.toUpperCase());
        return true;
    }

    /**
     * Parses a string into a ChatColor.
     *
     * @param input the color name
     * @return the corresponding ChatColor, or null if invalid
     */
    private ChatColor parseColor(String input) {
        try {
            return ChatColor.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Predefined allowed colors per rank
    private static final List<ChatColor> RANK1 = List.of(ChatColor.GRAY, ChatColor.WHITE);
    private static final List<ChatColor> RANK2 = concat(List.of(ChatColor.DARK_GRAY, ChatColor.AQUA), RANK1);
    private static final List<ChatColor> RANK3 = concat(List.of(ChatColor.DARK_AQUA, ChatColor.BLUE), RANK2);
    private static final List<ChatColor> RANK4 = concat(List.of(ChatColor.DARK_BLUE, ChatColor.YELLOW), RANK3);
    private static final List<ChatColor> RANK5 = concat(List.of(ChatColor.GOLD, ChatColor.GREEN, ChatColor.DARK_GREEN), RANK4);
    private static final List<ChatColor> RANK6 = concat(List.of(ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE), RANK5);
    private static final List<ChatColor> RANK7 = Arrays.stream(ChatColor.values())
            .filter(c -> c.isColor() && c != ChatColor.RED && c != ChatColor.BLACK && c != ChatColor.DARK_RED)
            .toList();

    /**
     * Returns the list of allowed chat colors for a player based on their rank permissions.
     *
     * @param p the player
     * @return a list of allowed ChatColor
     */
    private List<ChatColor> getAllowedColors(Player p) {
        List<ChatColor> out = new ArrayList<>();
        if (p.hasPermission("robbery.rank1")) out.addAll(RANK1);
        if (p.hasPermission("robbery.rank2")) out.addAll(RANK2);
        if (p.hasPermission("robbery.rank3")) out.addAll(RANK3);
        if (p.hasPermission("robbery.rank4")) out.addAll(RANK4);
        if (p.hasPermission("robbery.rank5")) out.addAll(RANK5);
        if (p.hasPermission("robbery.rank6")) out.addAll(RANK6);
        if (p.hasPermission("robbery.rank7")) out.addAll(RANK7);
        if(p.hasPermission("robbery.bypass")) out.addAll(List.of(ChatColor.values()));
        return out;
    }

    /**
     * Combines two lists of ChatColor into an unmodifiable list.
     *
     * @param base  the base list
     * @param extra the extra list to add
     * @return an unmodifiable list containing both base and extra
     */
    private static List<ChatColor> concat(List<ChatColor> base, List<ChatColor> extra) {
        List<ChatColor> result = new ArrayList<>(base);
        result.addAll(extra);
        return Collections.unmodifiableList(result);
    }
}
