package robbery.crypto;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import robbery.core.Robbery;
import robbery.messages.Messages;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CryptoNPCListener implements Listener {

    private final Robbery plugin;
    private final Set<UUID> talkingPlayers = new HashSet<>();
    private final Map<UUID, Long> lastClickMap = new HashMap<>();

    public CryptoNPCListener(Robbery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateNPCVisibility(p), 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateNPCVisibility(p), 50L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateNPCVisibility(p), 20L);
    }

    public void updateNPCVisibility(Player p) {
        if (p == null || !p.isOnline()) return;
        PlayerData pd = PlayerDataManager.getPlayerData(p);
        if (pd == null) return;

        boolean talked = pd.hasTalkedToCryptoNPC();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                String name = getNormalizedNPCName(entity);
                if (name.isEmpty()) continue;

                if (isBatteryNPC(name)) {
                    if (!talked) {
                        p.hideEntity(plugin, entity);
                    } else {
                        p.showEntity(plugin, entity);
                    }
                } else if (isDealerNPC(name)) {
                    // Crypto Dealer is always visible to everyone
                    p.showEntity(plugin, entity);
                }
            }
        }
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

    public void handleNPCInteraction(Player p, Entity entity) {
        if (p == null || entity == null) return;

        // Throttle clicks within 500ms to prevent double firing
        long now = System.currentTimeMillis();
        UUID uuid = p.getUniqueId();
        Long last = lastClickMap.get(uuid);
        if (last != null && now - last < 500) {
            return;
        }

        String normalizedName = getNormalizedNPCName(entity);
        if (normalizedName.isEmpty()) return;

        if (!isDealerNPC(normalizedName) && !isBatteryNPC(normalizedName)) return;

        lastClickMap.put(uuid, now);

        PlayerData pd = PlayerDataManager.getPlayerData(p);
        if (pd == null) return;

        // --- NPC 2: Crypto Battery (Sacrifice NPC) ---
        if (isBatteryNPC(normalizedName)) {
            if (!pd.hasTalkedToCryptoNPC()) {
                Messages.send(p, "crypto-dealer.talk-first");
                return;
            }

            if (!pd.hasTalkedToCryptoBatteryNPC()) {
                pd.setTalkedToCryptoBatteryNPC(true);
                plugin.getPlayerEventListener().savePlayerData(p, pd);
                Messages.send(p, "crypto-battery.line1");
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Messages.send(p, "crypto-battery.line2");
                }, 40L);
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Messages.send(p, "crypto-battery.line3");
                }, 80L);
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Messages.send(p, "crypto-battery.line4");
                }, 120L);
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Messages.send(p, "crypto-battery.line5");
                }, 160L);
                return;
            }

            plugin.getCryptoSacrificeGUI().open(p);
            return;
        }

        // --- NPC 1: Crypto Dealer (Intro + Shop NPC) ---
        if (isDealerNPC(normalizedName)) {
            // Check knowledge requirement: Arcade (Store 4+) or Prestige 1+
            boolean hasKnowledge = pd.getPrestige() >= 1
                    || pd.getHighestOwnedStoreTier() >= 4
                    || pd.hasKey("store4")
                    || (pd.getKey() != null && pd.getKey().getOrder() >= 4);

            if (!hasKnowledge) {
                Messages.send(p, "crypto-dealer.not-enough-knowledge");
                return;
            }

            // If already talked, open Crypto Dealer shop menu
            if (pd.hasTalkedToCryptoNPC()) {
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_dealer " + p.getName());
                return;
            }

            if (talkingPlayers.contains(uuid)) return;

            talkingPlayers.add(uuid);
            Messages.send(p, "crypto-dealer.line1");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Messages.send(p, "crypto-dealer.line2");
            }, 4 * 20L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Messages.send(p, "crypto-dealer.line3");
            }, 8 * 20L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Messages.send(p, "crypto-dealer.line4");
            }, 12 * 20L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Messages.send(p, "crypto-dealer.line5");
            }, 16 * 20L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                talkingPlayers.remove(uuid);
                if (p.isOnline()) {
                    Messages.send(p, "crypto-dealer.line6");

                    pd.setTalkedToCryptoNPC(true);
                    plugin.getPlayerEventListener().savePlayerData(p, pd);

                    // Show Battery NPC client-sided
                    updateNPCVisibility(p);

                    // Open Crypto Dealer shop menu so player claims machine himself
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_dealer " + p.getName());
                }
            }, 20 * 20L);
        }
    }

    private boolean isDealerNPC(String name) {
        return name.contains("dealer") || name.contains("cryptodealer") || name.contains("cryptomachine") || name.contains("crypto dealer");
    }

    private boolean isBatteryNPC(String name) {
        return name.contains("battery") || name.contains("cryptobattery") || name.contains("cryptofuel") || name.contains("crypto fuel") || name.contains("fuel");
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

        // First translate '&' color codes, then strip all color/formatting codes (e.g. &n, &6, §n, §6)
        String translated = ChatColor.translateAlternateColorCodes('&', name);
        String stripped = ChatColor.stripColor(translated);

        // Normalize to lowercase alphanumeric
        return stripped.replaceAll("[^a-zA-Z0-9 ]", "").trim().toLowerCase();
    }
}
