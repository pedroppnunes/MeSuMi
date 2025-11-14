package robbery.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import robbery.Robbery;
import robbery.help.HelpCommandData;
import robbery.messages.Messages;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpCommand implements CommandExecutor {
    private final Map<String, List<HelpCommandData>> helpCategories = new HashMap<>();

    public HelpCommand(Robbery main){
        loadHelp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

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

    public void loadHelp() {
        helpCategories.put("Robbery", Arrays.asList(
                new HelpCommandData("/boosters", "Opens boosters menu."),
                new HelpCommandData("/claim", "Claim your rewards that you got in crates."),
                new HelpCommandData("/mall", "Teleports you to the mall. Require: Outlaw+ Rank"),
                new HelpCommandData("/muteinfo", "Information about your mute."),
                new HelpCommandData("/outpostinfo", "Opens outpost menu."),
                new HelpCommandData("/outpost", "Teleports you to the outpost."),
                new HelpCommandData("/rankup", "Buy automatically the next store."),
                new HelpCommandData("/skillpoints", "Opens skillpoint menu."),
                new HelpCommandData("/store", "Teleports you to your current store. Require: Heister+ Rank"),
                new HelpCommandData("/toggledoublejump", "Toggles your double jump. Require: MafiaBoss"),
                new HelpCommandData("/warnings", "Information about your warnings."),
                new HelpCommandData("[item]", "In chat, displays the item in your hand. Require: Heister+ Rank")
        ));
        helpCategories.put("Hideout", Arrays.asList(
                new HelpCommandData("/hideout", "Teleports you to the hideout."),
                new HelpCommandData("/ho", "Teleports you to the hideout."),
                new HelpCommandData("/ho accept [player-name]", "Accept an invitation from a player."),
                new HelpCommandData("/ho ban [player-name]", "Ban a player from your hideout."),
                new HelpCommandData("/ho biome", "Change the biome of the hideout."),
                new HelpCommandData("/ho chest [page]", "Open the hideout's chest."),
                new HelpCommandData("/ho close", "Close the hideout to the public."),
                new HelpCommandData("/ho coop [player-name]", "Add a player as a co-op to your hideout."),
                new HelpCommandData("/ho coops", "Open the coops menu."),
                new HelpCommandData("/ho counts [player-name/hideout-name]", "See block counts in your hideout."),
                new HelpCommandData("/ho create [hideout-name]", "Create a new hideout."),
                new HelpCommandData("/ho delwarp [warp-name]", "Delete an hideout warp."),
                new HelpCommandData("/ho demote [player-name]", "Demote a member in your hideout."),
                new HelpCommandData("/ho disband", "Disband your hideout permanently."),
                new HelpCommandData("/ho expel [player-name]", "Kick a visitor from your hideout."),
                new HelpCommandData("/ho fly", "Toggle hideout fly. Require: Outlaw+ Rank."),
                new HelpCommandData("/ho invite [player-name]", "Invite a player to your hideout."),
                new HelpCommandData("/ho kick [player-name]", "Kick a player from your hideout."),
                new HelpCommandData("/ho leave", "Leave your hideout."),
                new HelpCommandData("/ho members", "Open the members menu."),
                new HelpCommandData("/ho name [hideout-name]", "Change the name of your hideout."),
                new HelpCommandData("/ho open", "Open the hideout to the public."),
                new HelpCommandData("/ho panel [members/visitors] [toggle]", "Open hideout panel."),
                new HelpCommandData("/ho pardon [player-name]", "Unban a player from your hideout."),
                new HelpCommandData("/ho permissions [player-name] [reset]", "Get all permissions for an hideout role."),
                new HelpCommandData("/ho promote [player-name]", "Promote a member in your hideout."),
                new HelpCommandData("/ho rate [player-name/hideout-name]", "Rate an hideout."),
                new HelpCommandData("/ho ratings", "Show all hideout ratings."),
                new HelpCommandData("/ho setrole [player-name] [hideout-role]", "Change the role of a player in your hideout."),
                new HelpCommandData("/ho setteleport", "Change the teleport location of your hideout."),
                new HelpCommandData("/ho settings", "Open the settings menu."),
                new HelpCommandData("/ho setwarp [warp-name] [warp-category]", "Create a new hideout warp."),
                new HelpCommandData("/ho show [player-name/hideout-name]", "Get information about an hideout."),
                new HelpCommandData("/ho team [player-name/hideout-name]", "Get information about hideout members status."),
                new HelpCommandData("/ho teamchat [message]", "Toggle team chat mode."),
                new HelpCommandData("/ho teleport", "Teleport to your hideout."),
                new HelpCommandData("/ho top", "Open top hideouts panel."),
                new HelpCommandData("/ho transfer [player-name]", "Transfer your hideout's leadership."),
                new HelpCommandData("/ho uncoop [player-name]", "Remove a player from being a co-op in your hideout."),
                new HelpCommandData("/ho value [material]", "Get the worth value of a block."),
                new HelpCommandData("/ho values [player-name/hideout-name]", "Open the values menu."),
                new HelpCommandData("/ho visit [player-name/hideout-name]", "Teleport to the visitors location of an hideout."),
                new HelpCommandData("/ho visitors", "Open the visitors menu."),
                new HelpCommandData("/ho warp [player-name/hideout-name] [warp-name]", "Warp to an hideout warp."),
                new HelpCommandData("/ho warps", "Open the warps menu."),
                new HelpCommandData("/pv [page]", "Open your private chest.")
        ));
        helpCategories.put("Enchants", Arrays.asList(
                new HelpCommandData("/alchemist", "Combine books."),
                new HelpCommandData("/enchanter", "To buy enchants with your exp."),
                new HelpCommandData("/enchants", "Opens enchants panel."),
                new HelpCommandData("/tinkerer", "Exchange enchanted items for exp.")
        ));
        helpCategories.put("Shop", Arrays.asList(
                new HelpCommandData("/shop", "Opens shop."),
                new HelpCommandData("/sell hand", "Sells item in hand."),
                new HelpCommandData("/sellall [category/inventory]", "Sells all items in the category or inventory."),
                new HelpCommandData("/sellgui", "Opens sell gui.")
        ));
        helpCategories.put("Other", Arrays.asList(
                new HelpCommandData("/afk", "Become AFK."),
                new HelpCommandData("/balance [player]", "Displays your balance/Displays balance of a player."),
                new HelpCommandData("/baltop", "Displays balance top of the server."),
                new HelpCommandData("/buy", "Displays the store menu."),
                new HelpCommandData("/workbench", "Opens a crafting table. Require: Burglar+"),
                new HelpCommandData("/feed", "Feed yourself. Require: Robber+"),
                new HelpCommandData("/fix", "Fixes the item in hand. Require: Bandit+"),
                new HelpCommandData("/fix all", "Fixes all the items in your inventory. Require: Outlaw+"),
                new HelpCommandData("/help", "Displays this menu."),
                new HelpCommandData("/hp", "Hide Players/Unhide Players."),
                new HelpCommandData("/ignore [player]", "Displays ignored players/Ignore a player."),
                new HelpCommandData("/msg [player]", "Message a player."),
                new HelpCommandData("/near", "Displays players nearby."),
                new HelpCommandData("/nick", "Change your nickname on the server. Require: Bandit+ Rank."),
                new HelpCommandData("/nv", "Night Vision Effect."),
                new HelpCommandData("/ptime", "Changes player time. Require: Burglar+"),
                new HelpCommandData("/pweather", "Changes player weather. Require: Burglar+"),
                new HelpCommandData("/r", "Replies to the last messaged player."),
                new HelpCommandData("/realname", "Displays the real name of a player's nickname."),
                new HelpCommandData("/recipe [item]", "Displays the recipe of an item. Require: Bandit+"),
                new HelpCommandData("/stacker toggle", "Toggles stacking value blocks."),
                new HelpCommandData("/vote", "Displays all voting sites so you can vote."),
                new HelpCommandData("/xp", "Displays your xp.")
        ));

    }

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

    public void sendCategoryPage(Player player, String category, int page) {
        List<HelpCommandData> cmds = helpCategories.get(category);
        if (cmds == null) {
            Messages.send(player, "help.unknown_category");
            return;
        }

        int pageSize = 5;
        int maxPage = (int) Math.ceil((double) cmds.size() / pageSize);
        page = Math.max(1, Math.min(page, Math.max(1, maxPage)));

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