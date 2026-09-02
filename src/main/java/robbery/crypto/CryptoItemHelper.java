package robbery.crypto;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import robbery.core.Robbery;

import java.util.Collections;
import java.util.UUID;

public class CryptoItemHelper {

    public static final String MACHINE_ID = "ROBBERY:Crypto_Machine";
    
    // Default 3D Bitcoin / Crypto Computer Server Head Base64 Texture
    public static final String DEFAULT_HEAD_TEXTURE = 
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTAzNTBiM2E0OTA0OWU0ZjI1N2NjZDJkOWY0YjhmYTg2YzEzZTU2YmU1ZDExNWM1ZjE2MGEzOTg2MjdkMTgxMyJ9fX0=";

    public static ItemStack createMachineItem(Robbery plugin) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Crypto Machine").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            meta.lore(Collections.singletonList(Component.text("Place on your island!").color(NamedTextColor.GRAY)));

            // Apply custom Base64 texture profile
            applyTextureToMeta(meta, DEFAULT_HEAD_TEXTURE);

            NamespacedKey key = new NamespacedKey(plugin, "crypto_machine");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, MACHINE_ID);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            head.setItemMeta(meta);
        }
        return head;
    }

    public static void applyTextureToMeta(SkullMeta meta, String base64Texture) {
        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64Texture.getBytes()), "CryptoMachine");
            profile.setProperty(new ProfileProperty("textures", base64Texture));
            meta.setPlayerProfile(profile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void applyTextureToBlock(Block block, String base64Texture) {
        if (block.getState() instanceof Skull skull) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64Texture.getBytes()), "CryptoMachine");
                profile.setProperty(new ProfileProperty("textures", base64Texture));
                skull.setPlayerProfile(profile);
                skull.update(true, false);
            } catch (Throwable e) { e.printStackTrace(); }
        }
    }

    public static boolean isCryptoMachineItem(ItemStack item, Robbery plugin) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "crypto_machine");
        var container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(key, PersistentDataType.STRING)) {
            String val = container.get(key, PersistentDataType.STRING);
            if (MACHINE_ID.equalsIgnoreCase(val)) return true;
        }
        if (container.has(key, PersistentDataType.BYTE)) {
            return true;
        }
        if (item.getItemMeta().hasDisplayName()) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            return name.contains(MACHINE_ID) || name.contains("Crypto Machine");
        }
        return false;
    }

    public static boolean playerAlreadyHasMachine(Player p, Robbery plugin) {
        if (p == null) return false;
        UUID uuid = p.getUniqueId();

        // 1. Placed on island?
        CryptoMachine machine = plugin.getCryptoManager().getMachine(uuid);
        if (machine != null && machine.isPlaced()) {
            return true;
        }

        // 2. In player inventory?
        for (ItemStack item : p.getInventory().getContents()) {
            if (isCryptoMachineItem(item, plugin)) {
                return true;
            }
        }

        // 3. In player ender chest?
        p.getEnderChest();
        for (ItemStack item : p.getEnderChest().getContents()) {
            if (isCryptoMachineItem(item, plugin)) {
                return true;
            }
        }

        // 4. Pending in /claim?
        var pending = robbery.keys.Rcrate.getPendingItemRewards().get(uuid);
        return pending != null && (pending.getOrDefault(Material.LOOM, 0) > 0 || pending.getOrDefault(Material.PLAYER_HEAD, 0) > 0);
    }
}
