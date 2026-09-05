package robbery.notifications;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsGUI implements Listener {

    private final Robbery plugin;
    private final NamespacedKey notifKey;
    private final NamespacedKey closeKey;

    public NotificationsGUI(Robbery plugin) {
        this.plugin = plugin;
        this.notifKey = new NamespacedKey(plugin, "notif_type");
        this.closeKey = new NamespacedKey(plugin, "notif_close");
    }

    public String getGuiTitle() {
        String title = Messages.get("notifications-gui.title");
        if (title == null || title.contains("Message not found")) {
            return ChatColor.translateAlternateColorCodes('&', "&8Notification Settings");
        }
        return title;
    }

    public void openGUI(Player player) {
        if (player == null) return;
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        // 45 slots (5 rows) to center icons and status indicators vertically & horizontally
        Inventory inv = Bukkit.createInventory(null, 45, getGuiTitle());

        // Fill background with dark gray stained glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Row 1 (centered cols 2..6): Item icons at slots 11, 12, 13, 14, 15
        // Row 2 (centered cols 2..6): Status indicators at slots 20, 21, 22, 23, 24
        setupNotifSlot(inv, pd, 11, 20, NotificationType.ABILITY_PROCS, Material.NETHER_STAR);
        setupNotifSlot(inv, pd, 12, 21, NotificationType.HIDEOUT_VALUE, Material.EMERALD);
        setupNotifSlot(inv, pd, 13, 22, NotificationType.HIDEOUT_DQ, Material.BARRIER);
        setupNotifSlot(inv, pd, 14, 23, NotificationType.ROBBERY_XP, Material.EXPERIENCE_BOTTLE);
        setupNotifSlot(inv, pd, 15, 24, NotificationType.CRYPTO_MACHINE, Material.REDSTONE_TORCH);

        // Slot 40: Close Menu button (centered on bottom row)
        String closeText = Messages.get("notifications-gui.close");
        if (closeText.contains("Message not found")) closeText = "&c&lClose Menu";
        ItemStack closeBtn = createCloseItem(Material.REDSTONE, closeText);
        inv.setItem(40, closeBtn);

        player.openInventory(inv);
    }

    private void setupNotifSlot(Inventory inv, PlayerData pd, int iconSlot, int statusSlot, NotificationType type, Material material) {
        boolean enabled = pd.isNotificationEnabled(type);

        String typeKey = type.name().toLowerCase();
        String name = Messages.get("notifications-gui.items." + typeKey + ".name");
        if (name.contains("Message not found")) name = "&a&l" + type.getDisplayName();

        String desc = Messages.get("notifications-gui.items." + typeKey + ".desc");
        if (desc.contains("Message not found")) desc = "&7" + type.getDescription();

        String statusLabel = enabled ? Messages.get("notifications-gui.status-enabled") : Messages.get("notifications-gui.status-disabled");
        if (statusLabel.contains("Message not found")) statusLabel = enabled ? "&a&lENABLED" : "&c&lDISABLED";

        String clickToToggle = Messages.get("notifications-gui.click-to-toggle");
        if (clickToToggle.contains("Message not found")) clickToToggle = "&eClick to toggle!";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', desc));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: " + statusLabel));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', clickToToggle));

        ItemStack mainItem = createPersistentItem(material, name, lore, type.name());
        inv.setItem(iconSlot, mainItem);

        Material statusMaterial = enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String statusName = enabled ? Messages.get("notifications-gui.status-on") : Messages.get("notifications-gui.status-off");
        if (statusName.contains("Message not found")) statusName = enabled ? "&a&l[✔] ON" : "&c&l[✘] OFF";

        String switchText = Messages.getFormatted("notifications-gui.click-to-switch", Map.of("next", enabled ? "OFF" : "ON"));
        if (switchText.contains("Message not found")) switchText = "&7Click to switch to " + (enabled ? "&cOFF" : "&aON");

        List<String> statusLore = List.of(ChatColor.translateAlternateColorCodes('&', switchText));
        ItemStack statusItem = createPersistentItem(statusMaterial, statusName, statusLore, type.name());
        inv.setItem(statusSlot, statusItem);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPersistentItem(Material material, String name, List<String> lore, String typeName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.getPersistentDataContainer().set(notifKey, PersistentDataType.STRING, typeName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.getPersistentDataContainer().set(closeKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(getGuiTitle())) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta()) return;

        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        if (meta.getPersistentDataContainer().has(closeKey, PersistentDataType.BYTE)) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
            return;
        }

        String typeName = meta.getPersistentDataContainer().get(notifKey, PersistentDataType.STRING);
        if (typeName == null) return;

        try {
            NotificationType type = NotificationType.valueOf(typeName);
            PlayerData pd = PlayerDataManager.getPlayerData(player);
            if (pd != null) {
                boolean newState = pd.toggleNotification(type);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, newState ? 1.2f : 0.8f);

                // Refresh GUI
                openGUI(player);
            }
        } catch (IllegalArgumentException ignored) {}
    }
}
