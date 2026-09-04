package robbery.storeMastery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;
import robbery.skilltree.SkillPerk;

import java.util.*;

public class PlayerSkillTreeGUI implements Listener {

    private final Robbery plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey perkKey;
    private final NamespacedKey targetUuidKey;
    private final NamespacedKey targetNameKey;
    private final NamespacedKey pageKey;

    public PlayerSkillTreeGUI(Robbery plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "skilltree_action");
        this.perkKey = new NamespacedKey(plugin, "skilltree_perk_id");
        this.targetUuidKey = new NamespacedKey(plugin, "skilltree_target_uuid");
        this.targetNameKey = new NamespacedKey(plugin, "skilltree_target_name");
        this.pageKey = new NamespacedKey(plugin, "skilltree_page");
    }

    public void openGUI(Player viewer, PlayerData targetData, String targetName, UUID targetUuid, int page) {
        if (viewer == null || targetData == null) return;
        if (page < 1) page = 1;
        if (page > 2) page = 2;

        boolean isSelf = (targetUuid != null && viewer.getUniqueId().equals(targetUuid)) || viewer.getName().equalsIgnoreCase(targetName);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Skill Tree Menu").color(NamedTextColor.DARK_GRAY));

        // Background Glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.text(" "));
            glass.setItemMeta(gMeta);
        }
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Header Profile Skull (Slot 46)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            if (targetUuid != null) headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            headMeta.displayName(Component.text("Viewing Profile: ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(targetName).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            headMeta.lore(List.of(
                    Component.text("Robbery Level: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(targetData.getLevel())).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Skill Points: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(targetData.getSkillPoints())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)),
                    Component.text("Prestige: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(targetData.getPrestige())).color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
            ));
            head.setItemMeta(headMeta);
        }
        inv.setItem(46, head);

        // Close Button (Slot 45 on Page 1)
        if (page == 1) {
            ItemStack closeItem = createActionButton(Material.BARRIER, "§cClose", "close", page, targetUuid, targetName);
            inv.setItem(45, closeItem);
        }

        // Back to General Menu Button (Slot 48)
        ItemStack backItem = createActionButton(Material.ARROW, "§aBack to General Menu", "back", page, targetUuid, targetName);
        inv.setItem(48, backItem);

        // Skill Points Info Button (Slot 49)
        ItemStack infoItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta iMeta = infoItem.getItemMeta();
        if (iMeta != null) {
            iMeta.displayName(Component.text("Skill Points: ").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(targetData.getSkillPoints())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
            infoItem.setItemMeta(iMeta);
        }
        inv.setItem(49, infoItem);

        // Reset Tree Button (Slot 52)
        int refundPoints = plugin.getSkillTreeConfig() != null ? plugin.getSkillTreeConfig().calculateTotalRefund(targetData) : 0;
        ItemStack resetItem = new ItemStack(Material.REDSTONE);
        ItemMeta rMeta = resetItem.getItemMeta();
        if (rMeta != null) {
            rMeta.displayName(Component.text("Reset Tree").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            rMeta.lore(List.of(
                    Component.text("SkillTreeReset Points: ").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(targetData.getResetSkillTreePoints())).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)),
                    Component.text("You need a reset point to reset your Skill Tree.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("This will give you a total of ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(String.valueOf(refundPoints)).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                            .append(Component.text(" Skill Points.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)),
                    Component.text("You reset all your perks and lose 1 SkillTreeReset Point.").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            rMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "reset");
            rMeta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            if (targetUuid != null) rMeta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid.toString());
            rMeta.getPersistentDataContainer().set(targetNameKey, PersistentDataType.STRING, targetName);
            resetItem.setItemMeta(rMeta);
        }
        inv.setItem(52, resetItem);

        // Page Navigation Buttons
        if (page == 1) {
            ItemStack scrollUp = createActionButton(Material.ARROW, "§eScroll Up", "page_2", page, targetUuid, targetName);
            inv.setItem(8, scrollUp);
        } else {
            ItemStack scrollDown = createActionButton(Material.ARROW, "§eScroll Down", "page_1", page, targetUuid, targetName);
            inv.setItem(53, scrollDown);
        }

        // Level Glass Indicators (Column 0)
        if (page == 1) {
            inv.setItem(36, createLevelGlass(targetData.getLevel(), 2, 0));
            inv.setItem(27, createLevelGlass(targetData.getLevel(), 5, 2));
            inv.setItem(18, createLevelGlass(targetData.getLevel(), 10, 5));
            inv.setItem(9, createLevelGlass(targetData.getLevel(), 20, 10));
            inv.setItem(0, createLevelGlass(targetData.getLevel(), 35, 20));
        } else {
            inv.setItem(45, createLevelGlass(targetData.getLevel(), 20, 10));
            inv.setItem(36, createLevelGlass(targetData.getLevel(), 35, 20));
            inv.setItem(27, createLevelGlass(targetData.getLevel(), 50, 35));
            inv.setItem(18, createLevelGlass(targetData.getLevel(), 65, 50));
            inv.setItem(9, createLevelGlass(targetData.getLevel(), 80, 65));
            inv.setItem(0, createLevelGlass(targetData.getLevel(), 100, 80));
        }

        // Perk Item Placements
        Map<Integer, String> perks = page == 1 ? getPage1Perks() : getPage2Perks();
        for (Map.Entry<Integer, String> entry : perks.entrySet()) {
            int slot = entry.getKey();
            String perkId = entry.getValue();
            ItemStack perkItem = createPerkItem(perkId, targetData, isSelf, targetUuid, targetName, page);
            if (perkItem != null) {
                inv.setItem(slot, perkItem);
            }
        }

        viewer.openInventory(inv);
    }

    private ItemStack createActionButton(Material mat, String name, String action, int page, UUID targetUuid, String targetName) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            if (targetUuid != null) meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid.toString());
            if (targetName != null) meta.getPersistentDataContainer().set(targetNameKey, PersistentDataType.STRING, targetName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLevelGlass(int currentLevel, int reqLevel, int prevReqLevel) {
        boolean green = currentLevel >= reqLevel;
        boolean yellow = currentLevel >= prevReqLevel && currentLevel < reqLevel;
        Material mat = green ? Material.LIME_STAINED_GLASS_PANE : (yellow ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String color = green ? "§a" : (yellow ? "§e" : "§c");
            String denomColor = green ? "§2" : (yellow ? "§6" : "§4");
            meta.displayName(Component.text(color + "Robbery Lvl " + currentLevel + "§8/" + denomColor + reqLevel).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Map<Integer, String> getPage1Perks() {
        return Map.ofEntries(
                Map.entry(40, "moneymultiplier1"),
                Map.entry(30, "stealspeed1"),
                Map.entry(31, "xp1"),
                Map.entry(32, "outpostbuff1"),
                Map.entry(23, "abilityspinquests1"),
                Map.entry(21, "extrabackpackslots1"),
                Map.entry(12, "chanceskillpoint1"),
                Map.entry(11, "chancemoneymultiplier1"),
                Map.entry(14, "chancebooster1"),
                Map.entry(15, "chancestealspeed1"),
                Map.entry(3, "doubleitemchance1"),
                Map.entry(4, "avoidbeingcaught"),
                Map.entry(5, "itemstreakspeed1")
        );
    }

    private Map<Integer, String> getPage2Perks() {
        return Map.ofEntries(
                Map.entry(28, "abilityspinquests2"),
                Map.entry(29, "stealspeed2"),
                Map.entry(30, "moneymultiplier2"),
                Map.entry(31, "xp2"),
                Map.entry(19, "tripleitemchance1"),
                Map.entry(21, "instastealchance1"),
                Map.entry(25, "doubleinventorychance1"),
                Map.entry(2, "doublejump"),
                Map.entry(6, "keyschance"),
                Map.entry(4, "featherflight")
        );
    }

    private ItemStack createPerkItem(String perkId, PlayerData targetData, boolean isSelf, UUID targetUuid, String targetName, int page) {
        if (plugin.getSkillTreeConfig() == null) return null;
        SkillPerk perk = plugin.getSkillTreeConfig().getTier(perkId);
        if (perk == null) return null;

        int curLevel = targetData.getSkillTreeLevel(perkId);
        int maxLevel = perk.maxLevel();
        int reqLevel = perk.requiredLevel();
        int costNext = curLevel < maxLevel ? perk.costForNext(curLevel) : 0;
        double curVal = targetData.getPerkValue(perkId);
        double nextVal = curLevel < maxLevel ? perk.valueForLevel(curLevel + 1) : perk.valueForLevel(curLevel);
        boolean meetReq = targetData.canBuyPerk(perk);

        boolean isLockedLvl = targetData.getLevel() < reqLevel;
        boolean isLockedTier = !meetReq;
        boolean isMax = curLevel >= maxLevel;
        boolean isAffordable = targetData.getSkillPoints() >= costNext;

        Material mat;
        String colorPrefix;
        List<Component> lore = new ArrayList<>();

        if (isLockedTier) {
            mat = Material.GRAY_DYE;
            colorPrefix = "§c";
            lore.add(Component.text("Level " + curLevel + "§8/" + maxLevel).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Skillpoints: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(costNext)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(" "));
            lore.add(Component.text("Grants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatGrantValue(perkId, nextVal)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("You must unlock the previous tier first").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else if (isLockedLvl) {
            mat = Material.GRAY_DYE;
            colorPrefix = "§c";
            lore.add(Component.text("Level " + curLevel + "§8/" + maxLevel).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Skillpoints: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(costNext)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(" "));
            lore.add(Component.text("Grants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatGrantValue(perkId, nextVal)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text("You need to be Robbery Lvl " + reqLevel).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else if (isMax) {
            mat = Material.LIME_DYE;
            colorPrefix = "§a";
            lore.add(Component.text("Level " + curLevel + " (MAX)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(" "));
            lore.add(Component.text("Grants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatGrantValue(perkId, curVal)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            addExtraLoreNotes(lore, perkId);
        } else if (isAffordable) {
            mat = Material.YELLOW_DYE;
            colorPrefix = "§a";
            lore.add(Component.text("Level " + curLevel + "§8/" + maxLevel).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Skillpoints: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(costNext)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(" "));
            lore.add(Component.text("Grants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatGrantTransition(perkId, curVal, nextVal, curLevel)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            addExtraLoreNotes(lore, perkId);
            if (isSelf) {
                lore.add(Component.text("Click to Purchase").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            mat = Material.YELLOW_DYE;
            colorPrefix = "§c";
            lore.add(Component.text("Level " + curLevel + "§8/" + maxLevel).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Skillpoints: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(costNext)).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(" "));
            lore.add(Component.text("Grants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatGrantTransition(perkId, curVal, nextVal, curLevel)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            addExtraLoreNotes(lore, perkId);
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(colorPrefix + perk.name()).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(perkKey, PersistentDataType.STRING, perkId);
            meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
            if (targetUuid != null) meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid.toString());
            if (targetName != null) meta.getPersistentDataContainer().set(targetNameKey, PersistentDataType.STRING, targetName);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addExtraLoreNotes(List<Component> lore, String perkId) {
        if ("itemstreakspeed1".equals(perkId)) {
            lore.add(Component.text("(Stacks per item, Max 35%)").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        } else if ("doublejump".equals(perkId)) {
            lore.add(Component.text("Cooldown: 5 seconds").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        } else if ("keyschance".equals(perkId)) {
            lore.add(Component.text("(Vote, Epic, Legendary, Tags, Booster)").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        }
    }

    private String formatGrantValue(String id, double val) {
        return switch (id) {
            case "moneymultiplier1" -> String.format("+%.3fx Money Multiplier", val).replace(",", ".");
            case "moneymultiplier2" -> String.format("+%.2fx Money Multiplier", val).replace(",", ".");
            case "stealspeed1", "xp1", "outpostbuff1", "chanceskillpoint1", "chancebooster1", "doubleitemchance1", "avoidbeingcaught", "itemstreakspeed1", "xp2", "tripleitemchance1", "instastealchance1" -> "+" + Math.round(val) + "% " + getMetricSuffix(id);
            case "stealspeed2" -> String.format("+%.1f%% Speed", val).replace(",", ".");
            case "doubleinventorychance1" -> String.format("+%.1f%% Double Inventory Chance", val).replace(",", ".");
            case "extrabackpackslots1" -> "+" + Math.round(val) + " Backpack Slots";
            case "abilityspinquests1" -> "Quests now give Skill Points";
            case "abilityspinquests2" -> "Increases the amount of Skill Points quests give by +2";
            case "chancemoneymultiplier1" -> "5% Chance for +0.5x Money Multiplier (10s)";
            case "chancestealspeed1" -> "5% Chance for +50% Speed (10s)";
            case "doublejump" -> "Ability to Double Jump";
            case "keyschance" -> "Ability to receive Keys while stealing";
            case "featherflight" -> "Feather Flight effect for 5 seconds";
            default -> "+" + val;
        };
    }

    private String getMetricSuffix(String id) {
        return switch (id) {
            case "stealspeed1" -> "Speed";
            case "xp1", "xp2" -> "Robbery XP";
            case "outpostbuff1" -> "Outpost Buff";
            case "chanceskillpoint1" -> "Skill Point Chance";
            case "chancebooster1" -> "Booster Chance";
            case "doubleitemchance1" -> "Double Item Chance";
            case "avoidbeingcaught" -> "Avoidance Chance";
            case "itemstreakspeed1" -> "Speed per Item";
            case "tripleitemchance1" -> "Triple Item Chance";
            case "instastealchance1" -> "Insta Steal Chance";
            default -> "";
        };
    }

    private String formatGrantTransition(String id, double curVal, double nextVal, int curLevel) {
        if (id.equals("chancemoneymultiplier1") || id.equals("chancestealspeed1") || id.equals("abilityspinquests1") || id.equals("abilityspinquests2") || id.equals("doublejump") || id.equals("keyschance") || id.equals("featherflight")) {
            return formatGrantValue(id, nextVal);
        }
        if (curLevel == 0) {
            return formatGrantValue(id, nextVal);
        }
        String curStr = formatGrantValue(id, curVal);
        String nextStr = formatGrantValue(id, nextVal);
        return curStr + " §7-> §a" + nextStr;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component titleComp = event.getView().title();
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(titleComp);
        if (!plainTitle.contains("Skill Tree Menu") && !plainTitle.contains("Skill Tree:")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String action = pdc.get(actionKey, PersistentDataType.STRING);
        String perkId = pdc.get(perkKey, PersistentDataType.STRING);
        String targetUuidStr = pdc.get(targetUuidKey, PersistentDataType.STRING);
        String targetName = pdc.get(targetNameKey, PersistentDataType.STRING);
        Integer pageObj = pdc.get(pageKey, PersistentDataType.INTEGER);
        int currentPage = pageObj != null ? pageObj : 1;

        UUID targetUuid = null;
        if (targetUuidStr != null && !targetUuidStr.isEmpty()) {
            try { targetUuid = UUID.fromString(targetUuidStr); } catch (Exception ignored) {}
        }
        if (targetName == null || targetName.isEmpty()) targetName = player.getName();

        PlayerData targetData = getOrLoadPlayerData(plugin, targetUuid != null ? Bukkit.getOfflinePlayer(targetUuid) : Bukkit.getOfflinePlayer(targetName));
        if (targetData == null) return;

        boolean isSelf = (targetUuid != null && player.getUniqueId().equals(targetUuid)) || player.getName().equalsIgnoreCase(targetName);

        if (action != null) {
            switch (action) {
                case "close" -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    player.closeInventory();
                }
                case "back" -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    plugin.getPlayerStatsGUI().openGUIForOfflinePlayer(player, Bukkit.getOfflinePlayer(targetName));
                }
                case "page_1" -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    openGUI(player, targetData, targetName, targetUuid, 1);
                }
                case "page_2" -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    openGUI(player, targetData, targetName, targetUuid, 2);
                }
                case "reset" -> {
                    if (!isSelf) {
                        player.sendMessage(Component.text("You cannot reset another player's skill tree.").color(NamedTextColor.RED));
                        return;
                    }
                    if (targetData.getResetSkillTreePoints() <= 0) {
                        Messages.send(player, "command.skilltree.no-reset");
                        return;
                    }
                    targetData.setResetSkillTreePoints(targetData.getResetSkillTreePoints() - 1);
                    int refund = plugin.getSkillTreeConfig().calculateTotalRefund(targetData);
                    plugin.getSkillTreeConfig().performReset(targetData);
                    targetData.addSkillPoints(refund);
                    Messages.send(player, "command.skilltree.success");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    openGUI(player, targetData, targetName, targetUuid, currentPage);
                }
            }
            return;
        }

        if (perkId != null) {
            // Can only buy if inspecting self
            if (!isSelf) {
                return;
            }
            if (plugin.getSkillService().canUpgrade(player, perkId)) {
                plugin.getSkillService().upgrade(player, perkId);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openGUI(player, targetData, targetName, targetUuid, currentPage);
            }
        }
    }

    public static PlayerData getOrLoadPlayerData(Robbery plugin, org.bukkit.OfflinePlayer targetOff) {
        if (targetOff == null) return null;
        if (targetOff.isOnline() && targetOff.getPlayer() != null) {
            return PlayerDataManager.getPlayerData(targetOff.getPlayer());
        } else {
            org.bukkit.configuration.file.YamlConfiguration cfg = plugin.getPlayerDataDao().loadPlayerData(targetOff.getUniqueId());
            if (cfg != null) {
                PlayerData pd = new PlayerData(null);
                plugin.getPlayerEventListener().loadPlayerDataFromDB(null, pd, cfg);
                return pd;
            }
        }
        return null;
    }
}
