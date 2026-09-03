package robbery.storeMastery;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import robbery.core.Robbery;
import robbery.keys.KeyManager;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreNPCListener implements Listener {

    private final Robbery plugin;
    private final Map<UUID, Long> lastClickMap = new HashMap<>();
    private final Set<UUID> talkingPlayers = new HashSet<>();

    public StoreNPCListener(Robbery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleNPCInteraction(event.getPlayer(), event.getRightClicked());
    }

    @EventHandler
    public void onNPCInteractAt(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        handleNPCInteraction(event.getPlayer(), event.getRightClicked());
    }

    public void handleNPCInteraction(Player player, Entity entity) {
        if (player == null || entity == null) return;

        // Throttle clicks within 500ms
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long last = lastClickMap.get(uuid);
        if (last != null && now - last < 500) {
            return;
        }

        String normalizedName = getNormalizedNPCName(entity);
        String detectedStore = getStoreFromNPCNameOrLocation(normalizedName, entity, player);

        if (detectedStore == null) {
            return;
        }

        lastClickMap.put(uuid, now);
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        if (pd == null) return;

        // Special handling for the first store NPC "ShopSell"
        if (isShopSellNPC(normalizedName, detectedStore)) {
            if (!pd.hasTalkedToShopSellNPC()) {
                if (talkingPlayers.contains(uuid)) return;
                startShopSellTutorial(player, pd);
                return;
            }
        }

        // Standard interaction: Open Catalog GUI for detected store
        plugin.getStoreCatalogGUI().openGUI(player, detectedStore, 1);
    }

    private boolean isShopSellNPC(String name, String storeId) {
        if (name == null) name = "";
        String lower = name.toLowerCase();
        return lower.contains("shopsell") || lower.contains("shop sell") || lower.contains("sell") || (storeId.equalsIgnoreCase("store1") && lower.contains("clerk"));
    }

    private void startShopSellTutorial(Player player, PlayerData pd) {
        UUID uuid = player.getUniqueId();
        talkingPlayers.add(uuid);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
        Messages.send(player, "storeMastery.shopsell.line1");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                Messages.send(player, "storeMastery.shopsell.line2");
            }
        }, 3 * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                Messages.send(player, "storeMastery.shopsell.line3");
            }
        }, 6 * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
                Messages.send(player, "storeMastery.shopsell.line4");
            }
        }, 9 * 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            talkingPlayers.remove(uuid);
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                pd.setTalkedToShopSellNPC(true);
                plugin.getPlayerEventListener().savePlayerData(player, pd);
                plugin.getStoreCatalogGUI().openGUI(player, "store1", 1);
            }
        }, 12 * 20L);
    }

    private String getStoreFromNPCNameOrLocation(String name, Entity entity, Player player) {
        if (name == null) name = "";
        String lowerName = name.toLowerCase();

        // 1. Direct store ID in NPC name e.g. "store1", "store 2"
        Matcher m = Pattern.compile("store\\s*(\\d+)").matcher(lowerName);
        if (m.find()) {
            return "store" + m.group(1);
        }

        // 2. Check store names (e.g. "Supermarket", "Arcade", "The Bank", etc.)
        for (int i = 1; i <= 12; i++) {
            String sId = "store" + i;
            String storeTitle = KeyManager.getStoreN(sId);
            if (storeTitle != null && lowerName.contains(storeTitle.toLowerCase())) {
                return sId;
            }
        }

        // 3. Keywords in NPC name e.g. "shopsell", "clerk", "catalog", "completionist", "merchant", "vendor"
        boolean isStoreNPC = lowerName.contains("shopsell") || lowerName.contains("clerk") ||
                lowerName.contains("catalog") || lowerName.contains("completionist") ||
                lowerName.contains("merchant") || lowerName.contains("vendor") ||
                lowerName.contains("shopkeeper") || lowerName.contains("sell");

        if (isStoreNPC) {
            String storeAtLoc = plugin.getStorePlaytimeTask().detectStore(player);
            if (storeAtLoc != null) return storeAtLoc;
            return "store1"; // Fallback to store 1
        }

        // 4. If entity is an NPC located inside a store region
        if (entity.hasMetadata("NPC")) {
            String storeAtEntityLoc = plugin.getStorePlaytimeTask().detectStore(player);
            if (storeAtEntityLoc != null) return storeAtEntityLoc;
        }

        return null;
    }

    private String getNormalizedNPCName(Entity entity) {
        if (entity == null) return "";

        String name = "";

        // 1. Try Citizens API if entity is a Citizens NPC
        if (entity.hasMetadata("NPC")) {
            try {
                Class<?> citizensAPI = Class.forName("net.citizensnpcs.api.CitizensAPI");
                Object registry = citizensAPI.getMethod("getNPCRegistry").invoke(null);
                Object npc = registry.getClass().getMethod("getNPC", Entity.class).invoke(registry, entity);
                if (npc != null) {
                    String cName = (String) npc.getClass().getMethod("getName").invoke(npc);
                    if (cName != null && !cName.isEmpty()) {
                        name = cName;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Custom name
        if (name.isEmpty() && entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            name = entity.getCustomName();
        }

        // 3. Entity name fallback
        if (name.isEmpty()) {
            name = entity.getName();
        }

        if (name == null) return "";

        String translated = ChatColor.translateAlternateColorCodes('&', name);
        String stripped = ChatColor.stripColor(translated);
        return stripped.replaceAll("[^a-zA-Z0-9 ]", "").trim().toLowerCase();
    }
}
