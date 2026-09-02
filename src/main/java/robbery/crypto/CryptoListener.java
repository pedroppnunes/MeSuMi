package robbery.crypto;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import robbery.messages.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import robbery.core.Robbery;

public class CryptoListener implements Listener {

    private final Robbery plugin;

    public CryptoListener(Robbery plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (CryptoItemHelper.isCryptoMachineItem(item, plugin)) {
            Player p = event.getPlayer();
            
            CryptoMachine machine = plugin.getCryptoManager().getMachine(p.getUniqueId());
            if (machine == null) {
                event.setCancelled(true);
                return;
            }
            
            if (machine.isPlaced()) {
                if (p.hasPermission("robbery.op")) {
                    p.sendMessage(Messages.colorize("&c[Admin] Bypassing placement limit! Old block is now inactive."));
                } else {
                    Messages.send(p, "crypto.already-placed");
                    event.setCancelled(true);
                    return;
                }
            }
            
            // Check if in SuperiorSkyblock island and if they are a member of it
            Island islandAtLoc = SuperiorSkyblockAPI.getIslandAt(event.getBlock().getLocation());
            com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(p);
            
            if (islandAtLoc == null || sp.getIsland() == null || !islandAtLoc.equals(sp.getIsland())) {
                Messages.send(p, "crypto.only-island");
                event.setCancelled(true);
                return;
            }

            // Set custom player head texture on the placed block
            CryptoItemHelper.applyTextureToBlock(event.getBlock(), CryptoItemHelper.DEFAULT_HEAD_TEXTURE);
            
            machine.setLocation(event.getBlock().getLocation());
            machine.updateHologram();
            Messages.send(p, "crypto.placed");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (CryptoItemHelper.isCryptoMachineItem(item, plugin)) {
            event.setCancelled(true);
            Messages.send(event.getPlayer(), "crypto.cannot-drop");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> {
            if (CryptoItemHelper.isCryptoMachineItem(item, plugin)) {
                event.getItemsToKeep().add(item);
                return true;
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        ItemStack item = event.getItem().getItemStack();
        if (CryptoItemHelper.isCryptoMachineItem(item, plugin)) {
            if (CryptoItemHelper.playerAlreadyHasMachine(p, plugin)) {
                event.setCancelled(true);
                Messages.send(p, "crypto.already-possess");
                return;
            }
            if (p.getInventory().firstEmpty() == -1) {
                event.setCancelled(true);
                Messages.send(p, "crypto.inventory-full");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        if (event.getView().getTopInventory().getHolder() instanceof robbery.core.RewardHolder) {
            return;
        }
        
        if (CryptoItemHelper.isCryptoMachineItem(current, plugin) || CryptoItemHelper.isCryptoMachineItem(cursor, plugin)) {
            if (event.getClickedInventory() != null && event.getClickedInventory() != p.getInventory()) {
                event.setCancelled(true);
                Messages.send(p, "crypto.cannot-store");
                return;
            }
            
            if (event.isShiftClick() && event.getClickedInventory() == p.getInventory()) {
                if (event.getView().getTopInventory() != null && event.getView().getTopInventory() != p.getInventory()) {
                    event.setCancelled(true);
                    Messages.send(p, "crypto.cannot-store");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        
        if (event.getView().getTopInventory().getHolder() instanceof robbery.core.RewardHolder) {
            return;
        }
        
        ItemStack oldCursor = event.getOldCursor();
        if (CryptoItemHelper.isCryptoMachineItem(oldCursor, plugin)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    Messages.send(p, "crypto.cannot-store");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isHeadBlock(block) && isCryptoMachine(block)) {
            event.setCancelled(true);
            Messages.send(event.getPlayer(), "crypto.cannot-break");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isCryptoMachine);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isCryptoMachine);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
            Block block = event.getClickedBlock();
            if (block != null && isHeadBlock(block)) {
                if (isCryptoMachine(block)) {
                    event.setCancelled(true);
                    
                    Player p = event.getPlayer();
                    CryptoMachine machine = plugin.getCryptoManager().getMachine(p.getUniqueId());
                    
                    if (machine != null && machine.isPlaced() && machine.getLocation().equals(block.getLocation())) {
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "dm open crypto_machine " + p.getName());
                    } else {
                        Messages.send(p, "crypto.not-yours");
                    }
                }
            }
        }
    }
    
    private boolean isHeadBlock(Block block) {
        if (block == null) return false;
        Material mat = block.getType();
        return mat == Material.PLAYER_HEAD || mat == Material.PLAYER_WALL_HEAD || mat == Material.LOOM;
    }

    private boolean isCryptoMachine(Block block) {
        if (!isHeadBlock(block)) return false;
        
        for (CryptoMachine machine : plugin.getCryptoManager().getActiveMachines().values()) {
            if (machine != null && machine.isPlaced() && block.getLocation().equals(machine.getLocation())) {
                return true;
            }
        }
        return false;
    }
}
