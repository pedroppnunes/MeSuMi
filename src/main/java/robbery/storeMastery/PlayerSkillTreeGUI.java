package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import robbery.core.Robbery;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.skilltree.SkillPerk;
import robbery.skilltree.SkillTreeConfig;

import java.util.*;

public class PlayerSkillTreeGUI implements Listener {

    private final Robbery plugin;
    private final NamespacedKey tierKey;

    public PlayerSkillTreeGUI(Robbery plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "skilltree_view_tier");
    }

    public void openGUI(Player viewer, PlayerData targetData, String targetName, UUID targetUuid, int tier) {
        if (viewer == null || targetData == null) return;
        if (tier < 1) tier = 1;
        if (tier > 4) tier = 4;

        Inventory inv = Bukkit.createInventory(null, 36, Component.text("Skill Tree: " + targetName).color(NamedTextColor.DARK_GRAY));

        // Background Glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.text(" "));
            glass.setItemMeta(gMeta);
        }
        for (int i = 0; i < 36; i++) inv.setItem(i, glass);

        // Header Skull (Slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            if (targetUuid != null) headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            headMeta.displayName(Component.text("[" + targetData.getLevel() + "] " + targetName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            headMeta.lore(List.of(
                    Component.text("Total XP: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(NumberFormatter.formatDoubleNumber((double) targetData.getXp()) + " XP").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Available Skill Points: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(targetData.getSkillPoints())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
            ));
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // Tier Switcher Buttons (Slots 10, 11, 12, 13)
        for (int t = 1; t <= 4; t++) {
            ItemStack tierBtn = new ItemStack(t == tier ? Material.NETHER_STAR : Material.EXPERIENCE_BOTTLE);
            ItemMeta tMeta = tierBtn.getItemMeta();
            if (tMeta != null) {
                tMeta.displayName(Component.text("Tier " + t + " Skills").color(t == tier ? NamedTextColor.YELLOW : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                if (t == tier) {
                    tMeta.lore(List.of(Component.text("Currently Viewing").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
                } else {
                    tMeta.lore(List.of(Component.text("Click to view Tier " + t).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                }
                tMeta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, t);
                tierBtn.setItemMeta(tMeta);
            }
            inv.setItem(9 + t, tierBtn);
        }

        // Display Perks in Selected Tier (Slots 19 to 25)
        List<SkillPerk> perksInTier = SkillTreeConfig.getPerksForTier(tier);
        int[] perkSlots = {19, 20, 21, 22, 23, 24, 25};

        if (perksInTier != null) {
            for (int i = 0; i < Math.min(perksInTier.size(), perkSlots.length); i++) {
                SkillPerk perk = perksInTier.get(i);
                if (perk == null) continue;

                int level = targetData.getSkillTreeLevel(perk.getId());
                boolean unlocked = level > 0;

                ItemStack perkItem = new ItemStack(unlocked ? Material.ENCHANTED_BOOK : Material.BOOK);
                ItemMeta pMeta = perkItem.getItemMeta();
                if (pMeta != null) {
                    pMeta.displayName(Component.text(perk.getName()).color(unlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(level + "/" + perk.getMaxLevel()).color(unlocked ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
                    lore.add(Component.text("Description: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(perk.getDescription()).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));

                    double currentBonus = targetData.getPerkValue(perk.getId());
                    lore.add(Component.text("Current Bonus: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text("+" + String.format("%.2f", currentBonus)).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));

                    pMeta.lore(lore);
                    perkItem.setItemMeta(pMeta);
                }
                inv.setItem(perkSlots[i], perkItem);
            }
        }

        // Close Button (Slot 31)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = closeItem.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(cMeta);
        }
        inv.setItem(31, closeItem);

        // Target Head Indicator at Bottom Right (Slot 35)
        ItemStack targetHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta thMeta = (SkullMeta) targetHead.getItemMeta();
        if (thMeta != null) {
            if (targetUuid != null) thMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            thMeta.displayName(Component.text("Inspecting: ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(targetName).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            targetHead.setItemMeta(thMeta);
        }
        inv.setItem(35, targetHead);

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = title.toString();

        if (titleStr.contains("Skill Tree: ")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
    }
}
