package robbery.keys;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import robbery.core.Robbery;

import java.util.ArrayList;
import java.util.List;

public class CratesMenuGUI implements Listener {

    private final Robbery plugin;

    public CratesMenuGUI(Robbery plugin) {
        this.plugin = plugin;
    }

    public static class CratesMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        Inventory gui = Bukkit.createInventory(new CratesMenuHolder(), 45, LegacyComponentSerializer.legacyAmpersand().deserialize(" "));

        // Background
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, background);
        }

        // Vote Crate - Slot 11
        gui.setItem(11, createCrateItem(player, Material.TRIPWIRE_HOOK, "&a&lVote Crate", List.of(
                "&7You have &f%excellentcrates_keys_vote_crate% &7keys",
                "",
                "&7Left-Click to open",
                "&7Right-Click to preview"
        )));

        // Epic Crate - Slot 19
        gui.setItem(19, createCrateItem(player, Material.TRIPWIRE_HOOK, "&5&lEpic Crate", List.of(
                "&7You have &f%excellentcrates_keys_epic_crate% &7keys",
                "",
                "&7Left-Click to open",
                "&7Right-Click to preview"
        )));

        // Legendary Crate - Slot 29
        gui.setItem(29, createCrateItem(player, Material.TRIPWIRE_HOOK, "&6&lLegendary Crate", List.of(
                "&7You have &f%excellentcrates_keys_legendary% &7keys",
                "",
                "&7Left-Click to open",
                "&7Right-Click to preview"
        )));

        // Monthly Crate - Slot 22
        gui.setItem(22, createCrateItem(player, Material.ENDER_CHEST, "&d&lMonthly Crate", List.of(
                "&7You have &f%excellentcrates_keys_monthly% &7keys",
                "",
                "&7Contains possible rewards:",
                "&8- &aMoney",
                "&8- &bKeys",
                "&8- &eBoosters",
                "&8- &dTags",
                "&8- &6Special Items",
                "&7Click to preview or open"
        )));

        // Boosters - Slot 15
        gui.setItem(15, createCrateItem(player, Material.POTION, "&3&lBoosters", List.of(
                "&7You have &f%excellentcrates_keys_booster% &7keys",
                "",
                "&7Left-Click to open",
                "&7Right-Click to preview"
        )));

        // Tags - Slot 25
        gui.setItem(25, createCrateItem(player, Material.NAME_TAG, "&d&lTags", List.of(
                "&7You have &f%excellentcrates_keys_tags_crate% &7keys",
                "",
                "&7Left-Click to open",
                "&7Right-Click to preview"
        )));

        // Barrier - Slot 40
        gui.setItem(40, createCrateItem(player, Material.BARRIER, "&c&lClose Menu", List.of(
                "&7Click to close this menu"
        )));

        player.openInventory(gui);
    }

    private ItemStack createCrateItem(Player player, Material material, String name, List<String> rawLore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                String parsed = line;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    parsed = PlaceholderAPI.setPlaceholders(player, line);
                }
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(parsed).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name).decoration(TextDecoration.ITALIC, false));
            if (loreLines != null) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) {
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory() == null || !(event.getInventory().getHolder() instanceof CratesMenuHolder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        boolean isRightClick = event.getClick() == ClickType.RIGHT;

        switch (slot) {
            case 11 -> {
                if (isRightClick) {
                    player.performCommand("crate preview vote_crate " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " vote_crate");
                }
            }
            case 19 -> {
                if (isRightClick) {
                    player.performCommand("crate preview epic_crate " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " epic_crate");
                }
            }
            case 29 -> {
                if (isRightClick) {
                    player.performCommand("crate preview legendary " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " legendary");
                }
            }
            case 22 -> {
                if (isRightClick) {
                    player.performCommand("crate preview monthly " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " monthly");
                }
            }
            case 15 -> {
                if (isRightClick) {
                    player.performCommand("crate preview booster " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " booster");
                }
            }
            case 25 -> {
                if (isRightClick) {
                    player.performCommand("crate preview tags_crate " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate openfor " + player.getName() + " tags_crate");
                }
            }
            case 40 -> player.closeInventory();
        }
    }
}
