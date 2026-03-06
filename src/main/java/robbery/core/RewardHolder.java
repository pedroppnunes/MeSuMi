package robbery.core;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public record RewardHolder(UUID owner) implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}
