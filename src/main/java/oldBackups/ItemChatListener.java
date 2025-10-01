/*package robbery.events;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.libs.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ItemChatListener implements Listener {

    private final Set<String> allowedRanks = new HashSet<>(Arrays.asList(
            "robbery.rank5", "robbery.rank6", "robbery.rank7"
    ));

    private final Map<UUID, Long> itemCooldowns = new HashMap<>();

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check permission
        boolean hasRank = allowedRanks.stream().anyMatch(player::hasPermission);
        if (!hasRank) return;

        String message = event.getMessage();

        // Check for [item] usage
        if (!message.contains("[item]")) return;

        // Check cooldown
        long currentTime = System.currentTimeMillis();
        long lastUsed = itemCooldowns.getOrDefault(uuid, 0L);
        if (currentTime - lastUsed < 60_000) {
            long secondsLeft = (60_000 - (currentTime - lastUsed)) / 1000;
            player.sendMessage(Component.text("You must wait " + secondsLeft + "s before using [item] again.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        itemCooldowns.put(uuid, currentTime);

        // Get item in hand
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must be holding an item to use [item].", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        String displayName;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            displayName = meta.getDisplayName();
        } else {
            displayName = "";
        }
        Component itemComponent = Component.text(displayName)
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showItem(item.getType().getKey(), item.getAmount()));

        String[] parts = message.split("\\[item\\]", -1);
        List<Component> componentList = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                componentList.add(itemComponent);
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                String parsedPart = PlaceholderAPI.setPlaceholders(player, part);
                Component parsedComponent = (Component) legacySerializer.deserialize(parsedPart);
                componentList.add(parsedComponent);
            }
        }

        Component messageComponent = Component.empty();
        for (Component c : componentList) {
            messageComponent = messageComponent.append(c);
        }

        // Format player name with PlaceholderAPI
        String playerNamePlaceholder = "%player_displayname%";
        String playerNameParsed = PlaceholderAPI.setPlaceholders(player, playerNamePlaceholder);
        Component playerNameComponent = (Component) legacySerializer.deserialize(playerNameParsed);

        // Build the full chat message
        Component fullChatMessage = Component.text()
                .append(playerNameComponent)
                .append(Component.text(": ").color(NamedTextColor.WHITE))
                .append(messageComponent)
                .build();

        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(fullChatMessage);
        }

    }

}
 */
