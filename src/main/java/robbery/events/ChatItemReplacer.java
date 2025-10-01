package robbery.events;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import robbery.Robbery;

/**
 * Listens to player chat events and replaces the placeholder "[item]" with a hoverable
 * representation of the item in the player's main hand, if allowed.
 * <p>
 * Only players with a rank between 3 and 7 (inclusive) can use the "[item]" placeholder.
 * The chat message is formatted with prefix, tags, prestige, and player's custom chat style.
 * </p>
 */
public class ChatItemReplacer implements Listener {

    /** Reference to the main Robbery plugin instance. */
    private final Robbery plugin;

    /** Legacy component serializer used to deserialize color codes into Components. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /**
     * Constructs a new ChatItemReplacer listener.
     *
     * @param plugin the main Robbery plugin instance
     */
    public ChatItemReplacer(Robbery plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player chat events asynchronously.
     * <p>
     * Replaces "[item]" in chat messages with a hoverable item component if the player
     * has permission. If the player's hand is empty, a gray italic message is shown instead.
     * Formats the full message with prestige, tags, prefix, and player name.
     * </p>
     *
     * @param event the AsyncPlayerChatEvent
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Cancel the default chat message
        event.setCancelled(true);

        // Remove legacy color codes
        message = message.replaceAll("(?i)&[0-9A-FK-OR]", "");

        // Retrieve placeholders
        String prestige = PlaceholderAPI.setPlaceholders(player, "%robbery_prestige%");
        String tag      = PlaceholderAPI.setPlaceholders(player, "%deluxetags_tag%");
        String prefix   = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
        String name     = player.getDisplayName();
        String chatStyle = PlaceholderAPI.setPlaceholders(player, "%robbery_chatstyle%");

        // Split message around [item]
        String[] parts = message.split("\\[item\\]", 2);
        String before = parts[0];
        String after  = parts.length > 1 ? parts[1] : "";

        Component beforeComp = LEGACY.deserialize(chatStyle + before);
        Component afterComp  = LEGACY.deserialize(chatStyle + after);

        // Determine if player can use [item]
        boolean canUseItem = false;
        for (int i = 3; i <= 7; i++) {
            if (player.hasPermission("robbery.rank" + i)) {
                canUseItem = true;
                break;
            }
        }

        // Build item component if allowed
        Component itemComp;
        if (message.contains("[item]") && canUseItem) {
            itemComp = buildItemComponent(player);
        } else if (message.contains("[item]")) {
            itemComp = Component.text("[item]").color(beforeComp.color());
        } else {
            itemComp = Component.empty();
        }

        // Assemble full chat component
        Component full = Component.empty()
                .append(Component.text("[")
                        .color(NamedTextColor.GOLD)
                        .append(Component.text(prestige, NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.GOLD))
                )
                .append(Component.text(" "))
                .append(LEGACY.deserialize(ChatColor.translateAlternateColorCodes('&', prefix + tag + name)))
                .append(Component.text(" > ", NamedTextColor.DARK_GRAY))
                .append(beforeComp)
                .append(itemComp)
                .append(afterComp);

        // Send message to all online players and console
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(full))
        );
        Bukkit.getConsoleSender().sendMessage(full);
    }

    /**
     * Builds a hoverable Component representing the item in the player's main hand.
     * <p>
     * If the hand is empty, returns a gray italic message indicating the hand is empty.
     * Otherwise, shows the item name and hover preview of the item.
     * </p>
     *
     * @param player the player whose item to display
     * @return a Component representing the item or empty hand message
     */
    private Component buildItemComponent(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            String text = player.getDisplayName() + "'s hand is empty";
            return Component.text(text)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true);
        }

        String rawName = item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : ChatColor.YELLOW + item.getType().name().toLowerCase().replace("_", " ");

        Component display = LEGACY.deserialize(rawName);

        return display.hoverEvent(HoverEvent.showItem(item.asHoverEvent().value()));
    }
}
