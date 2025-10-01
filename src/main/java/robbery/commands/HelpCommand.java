package robbery.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import robbery.Robbery;
import robbery.help.HelpCommandData;
import robbery.messages.Messages;

import java.util.*;

/**
 * Handles the /help command for the Robbery plugin.
 * <p>
 * Provides players with an interactive help menu, listing commands in categories.
 * Commands can be clicked to suggest them in chat, and hovering shows their description.
 * </p>
 * <p>
 * Usage:
 * <ul>
 *     <li>/help &lt;category&gt; [page] - Shows commands of a specific category, optionally on a given page.</li>
 *     <li>/help - Shows the main help menu with all categories.</li>
 * </ul>
 * </p>
 */
public class HelpCommand implements CommandExecutor {

    private final Map<String, List<HelpCommandData>> helpCategories = new HashMap<>();

    /**
     * Creates a new HelpCommand and loads all help categories and commands.
     *
     * @param main the main plugin instance
     */
    public HelpCommand(Robbery main){
        loadHelp();
    }

    /**
     * Executes the /help command.
     *
     * @param sender the command sender
     * @param cmd    the command object
     * @param label  the command label used
     * @param args   command arguments
     * @return true if executed successfully
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            sendHelpMain(player);
        } else {
            String category = args[0];
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                    page = 1;
                }
            }
            sendCategoryPage(player, category, page);
        }
        return true;
    }

    /**
     * Loads all help categories and their commands.
     * This is called during initialization.
     */
    public void loadHelp() {
        // Populates helpCategories with commands (Robbery, Hideout, Enchants, Shop, Other)
    }

    /**
     * Sends the main help menu to a player, showing all categories.
     *
     * @param player the player to send the menu to
     */
    public void sendHelpMain(Player player) {
        Messages.send(player, "help.main_title");
        for (String category : helpCategories.keySet()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("category", category);

            String display = Messages.getFormatted("help.category_display", placeholders);
            String hover  = Messages.getFormatted("help.category_hover", placeholders);

            TextComponent msg = new TextComponent(display);
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category + " 1"));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(hover).create()));
            player.spigot().sendMessage(msg);
        }
    }

    /**
     * Sends a specific page of commands for a given category to a player.
     *
     * @param player   the player to send the commands to
     * @param category the command category
     * @param page     the page number
     */
    public void sendCategoryPage(Player player, String category, int page) {
        List<HelpCommandData> cmds = helpCategories.get(category);
        if (cmds == null) {
            Messages.send(player, "help.unknown_category");
            return;
        }

        int pageSize = 5;
        int maxPage = (int) Math.ceil((double) cmds.size() / pageSize);
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, cmds.size());

        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("category", category);
        headerPlaceholders.put("page", String.valueOf(page));
        headerPlaceholders.put("maxpage", String.valueOf(maxPage));
        Messages.sendFormatted(player, "help.page_header", headerPlaceholders);

        for (int i = start; i < end; i++) {
            HelpCommandData data = cmds.get(i);

            Map<String, String> cmdPlaceholders = new HashMap<>();
            cmdPlaceholders.put("command", data.command());
            String cmdText = Messages.getFormatted("help.command_text", cmdPlaceholders);

            TextComponent cmdLine = new TextComponent(cmdText);
            cmdLine.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, data.command()));

            Map<String, String> descPlaceholders = new HashMap<>();
            descPlaceholders.put("description", data.description());
            String descText = Messages.getFormatted("help.command_description", descPlaceholders);

            TextComponent desc = new TextComponent(descText);

            player.spigot().sendMessage(cmdLine, desc);
        }

        TextComponent nav = new TextComponent();
        if (page > 1) {
            Map<String, String> prevPlaceholders = new HashMap<>();
            prevPlaceholders.put("page", String.valueOf(page - 1));
            String prevText = Messages.getFormatted("help.nav_prev", prevPlaceholders);

            TextComponent left = new TextComponent(prevText);
            left.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category + " " + (page - 1)));
            nav.addExtra(left);
        }
        if (page < maxPage) {
            Map<String, String> nextPlaceholders = new HashMap<>();
            nextPlaceholders.put("page", String.valueOf(page + 1));
            String nextText = Messages.getFormatted("help.nav_next", nextPlaceholders);

            TextComponent right = new TextComponent(nextText);
            right.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + category + " " + (page + 1)));
            nav.addExtra(right);
        }
        if (nav.getExtra() != null) {
            player.spigot().sendMessage(nav);
        }
    }
}
