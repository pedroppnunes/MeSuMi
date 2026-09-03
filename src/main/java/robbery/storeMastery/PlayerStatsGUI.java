package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import robbery.backpacks.BackpackManager;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.keys.Keys;
import robbery.number.NumberFormatter;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.tool.ToolManager;
import robbery.tool.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static robbery.attribute.Attribute.*;

public class PlayerStatsGUI implements Listener {

    private final Robbery plugin;
    private final NamespacedKey mainmenuKey;

    public PlayerStatsGUI(Robbery plugin) {
        this.plugin = plugin;
        this.mainmenuKey = new NamespacedKey(plugin, "mainmenu_item");
    }

    public void openGUI(Player viewer, Player target) {
        if (viewer == null || target == null) return;
        PlayerData pd = PlayerDataManager.getPlayerData(target);
        if (pd == null) return;

        openGUIForData(viewer, pd, target.getName(), target.getUniqueId());
    }

    public void openGUIForOfflinePlayer(Player viewer, OfflinePlayer offlineTarget) {
        if (viewer == null || offlineTarget == null) return;

        if (offlineTarget.isOnline() && offlineTarget.getPlayer() != null) {
            openGUI(viewer, offlineTarget.getPlayer());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = offlineTarget.getUniqueId();
            String name = offlineTarget.getName() != null ? offlineTarget.getName() : "Offline Player";

            YamlConfiguration cfg = plugin.getPlayerDataDao().loadPlayerData(uuid);
            if (cfg == null) {
                java.io.File f = new java.io.File(plugin.getDataFolder(), "Playerdata/" + uuid + ".yml");
                if (f.exists()) {
                    cfg = YamlConfiguration.loadConfiguration(f);
                }
            }

            if (cfg == null) {
                robbery.messages.Messages.sendFormatted(viewer, "storeMastery.stats.no-stats-found", "player", name);
                return;
            }

            PlayerData offlineData = new PlayerData(null);
            plugin.getPlayerEventListener().loadPlayerDataFromDB(null, offlineData, cfg);

            Bukkit.getScheduler().runTask(plugin, () -> {
                openGUIForData(viewer, offlineData, name, uuid);
            });
        });
    }

    private void openGUIForData(Player viewer, PlayerData pd, String targetName, UUID targetUuid) {
        // Privacy check
        boolean isSelf = viewer.getUniqueId().equals(targetUuid);
        boolean isOp = viewer.isOp() || viewer.hasPermission("robbery.op");

        if (!isSelf && !isOp) {
            String privacy = pd.getProfilePrivacy();
            if (privacy.equalsIgnoreCase("PRIVATE")) {
                robbery.messages.Messages.send(viewer, "storeMastery.stats.privacy-private");
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            if (privacy.equalsIgnoreCase("HIDEOUT")) {
                boolean inSameHideout = areInSameHideout(viewer, targetUuid);
                if (!inSameHideout) {
                    robbery.messages.Messages.send(viewer, "storeMastery.stats.privacy-hideout");
                    viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
            }
        }

        Economy econ = Robbery.getEconomy();
        double balance = 0.0;
        if (targetUuid != null) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetUuid);
            if (econ != null) balance = econ.getBalance(off);
        }

        // Dark Gray title format: "Stats: PlayerName"
        String titleStr = "Stats: " + targetName;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(titleStr).color(NamedTextColor.DARK_GRAY));

        // Background Glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.text(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        // 1. Header Skull (Slot 4) - "[Level] PlayerName"
        // 1. Target Skull Header (Slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            if (targetUuid != null) headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            headMeta.displayName(Component.text("[").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getLevel())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("] ").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(targetName).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            headMeta.lore(null);
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // 2. Overview & Account Card (Slot 10 - Chest)
        ItemStack overviewItem = new ItemStack(Material.CHEST);
        ItemMeta oMeta = overviewItem.getItemMeta();
        if (oMeta != null) {
            oMeta.displayName(Component.text("Overview & Account").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            String displayRank = getFormattedRank(targetUuid, pd);
            lore.add(Component.text("Rank: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(displayRank).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Prestige: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Prestige " + pd.getPrestige()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Robbery Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Level " + pd.getLevel()).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Total XP: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(NumberFormatter.formatDoubleNumber((double) pd.getXp()) + " XP").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Balance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("$" + NumberFormatter.formatDoubleNumber(balance)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Skill Points: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getSkillPoints())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Total Items Stolen: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getItemsStolen())).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Total Times Busted: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(pd.getBustedCount())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            oMeta.lore(lore);
            overviewItem.setItemMeta(oMeta);
        }
        inv.setItem(10, overviewItem);

        // 3. Attributes & Boosts Card (Slot 12 - Beacon)
        ItemStack attrItem = new ItemStack(Material.BEACON);
        ItemMeta aMeta = attrItem.getItemMeta();
        if (aMeta != null) {
            aMeta.displayName(Component.text("Attributes & Boosts").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Money Multiplier: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.format("%.2f", pd.getBoost()) + "x").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Steal Speed Bonus: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getExtraDamage()) + "%").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Robbery XP Boost: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getXPBoost() * 100) + "%").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Extra Skill Point Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.2f", pd.getPerkValue(PERK_CHANCE_SP1)) + "%").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Extra Backpack Slots: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + pd.getExtraSlots()).color(NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Double Item Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_DOUBLE_ITEM1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Triple Item Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_TRIPLE_ITEM1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("Insta-Steal Chance: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("+" + String.format("%.1f", pd.getPerkValue(PERK_INSTA_STEAL1)) + "%").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            aMeta.lore(lore);
            attrItem.setItemMeta(aMeta);
        }
        inv.setItem(12, attrItem);

        // 4. Equipment & Unlocks Card (Slot 14 - Diamond Pickaxe)
        ItemStack equipItem = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta eMeta = equipItem.getItemMeta();
        if (eMeta != null) {
            eMeta.displayName(Component.text("Equipment & Unlocks").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

            // Backpack name without (Cap.5)
            String backName = pd.getBackpack() != null ? pd.getBackpack().getName() : "Cloth Backpack";
            lore.add(Component.text("Current Backpack: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(backName).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));

            // Real Tool Display Name formatted in White
            String toolId = pd.getToolString();
            Tools toolObj = ToolManager.getToolsName(toolId);
            String toolName = toolObj != null ? ChatColor.stripColor(toolObj.getColorname()) : toolId;
            lore.add(Component.text("Current Tool: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(toolName).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));

            // Highest Achieved Store
            int highestKeyTier = pd.getHighestOwnedStoreTier();
            String highestStoreName = KeyManager.getStoreN("store" + highestKeyTier);
            if (highestStoreName == null) highestStoreName = "Store " + highestKeyTier;

            lore.add(Component.text("Highest Achieved Store: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(highestStoreName).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));

            // Current Store with Milestone in gray in front: e.g. "Supermarket (M1)"
            String currentStoreId = pd.getKey() != null ? pd.getKey().getId() : "store1";
            Keys curKey = KeyManager.getStoreName(currentStoreId);
            String curStoreName = curKey != null ? curKey.getName() : "Supermarket";
            int curMastery = pd.getStoreMasteryLevel(currentStoreId);

            lore.add(Component.text("Current Store: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(curStoreName + " ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("(M" + curMastery + ")").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));

            lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            eMeta.lore(lore);
            equipItem.setItemMeta(eMeta);
        }
        inv.setItem(14, equipItem);

        // 5. Skill Tree Button (Slot 16 - Enchanted Book)
        ItemStack skillTreeBtn = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta stMeta = skillTreeBtn.getItemMeta();
        if (stMeta != null) {
            stMeta.displayName(Component.text("Skill Tree Progress").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            stMeta.lore(List.of(
                    Component.text("Click to view " + targetName + "'s full Skill Tree").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("and tier-by-tier perk upgrades!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            skillTreeBtn.setItemMeta(stMeta);
        }
        inv.setItem(16, skillTreeBtn);

        // 6. Privacy Dye Button (Slot 20 - Dye, only interactive for self)
        String currentPrivacy = pd.getProfilePrivacy();
        ItemStack dyeItem;
        String dyeName;
        NamedTextColor dyeColor;

        if (currentPrivacy.equalsIgnoreCase("PRIVATE")) {
            dyeItem = new ItemStack(Material.RED_DYE);
            dyeName = "Profile Privacy: PRIVATE";
            dyeColor = NamedTextColor.RED;
        } else if (currentPrivacy.equalsIgnoreCase("HIDEOUT")) {
            dyeItem = new ItemStack(Material.YELLOW_DYE);
            dyeName = "Profile Privacy: HIDEOUT ONLY";
            dyeColor = NamedTextColor.YELLOW;
        } else {
            dyeItem = new ItemStack(Material.LIME_DYE);
            dyeName = "Profile Privacy: PUBLIC";
            dyeColor = NamedTextColor.GREEN;
        }

        ItemMeta dyeMeta = dyeItem.getItemMeta();
        if (dyeMeta != null) {
            dyeMeta.displayName(Component.text(dyeName).color(dyeColor).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            if (isSelf) {
                dyeMeta.lore(List.of(
                        Component.text("Click to toggle profile privacy:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("• PUBLIC (Everyone can view)").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.text("• HIDEOUT ONLY (Only hideout members)").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                        Component.text("• PRIVATE (Only OP & self)").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                ));
            } else {
                dyeMeta.lore(List.of(
                        Component.text("Status: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(currentPrivacy).color(dyeColor).decoration(TextDecoration.ITALIC, false))
                ));
            }
            dyeItem.setItemMeta(dyeMeta);
        }
        inv.setItem(20, dyeItem);

        // 7. Store Item Catalog Button (Slot 22 - Writable Book)
        ItemStack catalogButton = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta cMeta = catalogButton.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(Component.text("View Store Item Catalog").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            cMeta.lore(List.of(
                    Component.text("Click to open completionist item catalog!").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            catalogButton.setItemMeta(cMeta);
        }
        inv.setItem(22, catalogButton);

        // 8. Close Button (Slot 24 - Barrier)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(closeMeta);
        }
        inv.setItem(24, closeItem);

        // 9. Target Head Indicator at Bottom Right (Slot 26)
        ItemStack targetIndicatorHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta tiMeta = (SkullMeta) targetIndicatorHead.getItemMeta();
        if (tiMeta != null) {
            if (targetUuid != null) tiMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            tiMeta.displayName(Component.text("Inspecting: ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(targetName).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            targetIndicatorHead.setItemMeta(tiMeta);
        }
        inv.setItem(26, targetIndicatorHead);

        viewer.openInventory(inv);
    }

    private String getFormattedRank(UUID targetUuid, PlayerData pd) {
        String r = pd.getRank();
        if (r == null || r.equalsIgnoreCase("NONE") || r.equalsIgnoreCase("rank0")) {
            if (targetUuid != null) {
                Player p = Bukkit.getPlayer(targetUuid);
                if (p != null) {
                    for (int i = 7; i >= 1; i--) {
                        if (p.hasPermission("robbery.rank" + i)) return "Rank " + i;
                    }
                }
            }
            return "Burglar";
        }
        return r;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);

        // 1. Detect click in DeluxeMenus Main Menu on slot 4 (Player Head)
        boolean isGeneralMenu = titleStr.toLowerCase().contains("general") || titleStr.toLowerCase().contains("main menu") || titleStr.toLowerCase().contains("robbery menu");
        if (isGeneralMenu && event.getSlot() == 4) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, player);
                return;
            }
        }

        // 2. Detect clicks inside PlayerStatsGUI
        if (titleStr.contains("Stats: ")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            String targetName = titleStr.substring(titleStr.indexOf("Stats: ") + 7).trim();
            OfflinePlayer offTarget = Bukkit.getOfflinePlayer(targetName);
            PlayerData targetPd = (offTarget.isOnline() && offTarget.getPlayer() != null) ? PlayerDataManager.getPlayerData(offTarget.getPlayer()) : null;

            if (clicked.getType() == Material.ENCHANTED_BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                if (targetPd != null) {
                    plugin.getPlayerSkillTreeGUI().openGUI(player, targetPd, targetName, offTarget.getUniqueId(), 1);
                } else {
                    openGUIForOfflinePlayer(player, offTarget);
                }
                return;
            }

            if (clicked.getType() == Material.WRITABLE_BOOK) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                plugin.getStoreCatalogGUI().openStoreSelectorGUI(player);
                return;
            }

            if (event.getRawSlot() == 20 || clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.YELLOW_DYE || clicked.getType() == Material.RED_DYE) {
                // Toggle privacy if viewing self
                if (player.getName().equalsIgnoreCase(targetName) || (offTarget != null && player.getUniqueId().equals(offTarget.getUniqueId()))) {
                    PlayerData selfPd = PlayerDataManager.getPlayerData(player);
                    if (selfPd != null) {
                        String current = selfPd.getProfilePrivacy();
                        String next = switch (current.toUpperCase()) {
                            case "HIDEOUT" -> "PRIVATE";
                            case "PRIVATE" -> "PUBLIC";
                            default -> "HIDEOUT";
                        };
                        selfPd.setProfilePrivacy(next);
                        plugin.getPlayerEventListener().savePlayerData(player, selfPd);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        openGUI(player, player);
                    }
                }
                return;
            }

            if (clicked.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onPlayerRightClickPlayer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        Player clicker = event.getPlayer();
        ItemStack item = clicker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(mainmenuKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            openGUI(clicker, targetPlayer);
        }
    }

    @EventHandler
    public void onPlayerRightClickPlayerAt(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        Player clicker = event.getPlayer();
        ItemStack item = clicker.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(mainmenuKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            openGUI(clicker, targetPlayer);
        }
    }

    private boolean areInSameHideout(Player viewer, UUID targetUuid) {
        if (viewer == null || targetUuid == null) return false;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer spViewer = com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.getPlayer(viewer);
                com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer spTarget = com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI.getPlayer(targetUuid);
                if (spViewer != null && spTarget != null) {
                    com.bgsoftware.superiorskyblock.api.island.Island islandViewer = spViewer.getIsland();
                    com.bgsoftware.superiorskyblock.api.island.Island islandTarget = spTarget.getIsland();
                    if (islandViewer != null && islandTarget != null && islandViewer.equals(islandTarget)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
