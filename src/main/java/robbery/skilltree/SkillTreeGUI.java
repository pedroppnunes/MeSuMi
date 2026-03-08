package robbery.skilltree;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import robbery.core.Robbery;
import robbery.player.PlayerData;
import robbery.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SkillTreeGUI implements Listener {
    private final Robbery plugin;
    private final SkillTreeConfig config;
    private final SkillService service;

    public SkillTreeGUI(Robbery plugin, SkillTreeConfig config, SkillService service) {
        this.plugin = plugin;
        this.config = config;
        this.service = service;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Collection<SkillPerk> tiers = config.getTiers();
        int size = 9 * 6;
        Inventory inv = Bukkit.createInventory(null, size, "Skill Tree");

        int slot = 0;
        PlayerData pd = PlayerDataManager.getPlayerData(player);
        for (SkillPerk t : tiers) {
            ItemStack icon = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(t.getName());
            List<String> lore = new ArrayList<>();
            int cur = pd.getSkillTreeLevel(t.getId());
            lore.add("Level: " + cur + "/" + t.getMaxLevel());
            lore.add("Requirement: Robbery Lv " + t.getRequiredLevel());
            if (cur < t.getMaxLevel()) {
                lore.add("Cost: " + t.costForNext(cur) + " SP");
                lore.add("Click to purchase");
            } else {
                lore.add("Maxed");
            }
            lore.add(t.getDescription());
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
            if (slot >= size) break;
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!"Skill Tree".equals(e.getView().getTitle())) return;
        e.setCancelled(true);

        int index = e.getRawSlot();
        List<SkillPerk> list = new ArrayList<>(config.getTiers());
        if (index < 0 || index >= list.size()) return;
        SkillPerk tier = list.get(index);
        if (service.canUpgrade(p, tier.getId())) {
            boolean ok = service.upgrade(p, tier.getId());
            if (!ok) p.sendMessage("§cFailed to purchase skill.");
            else p.sendMessage("§aPurchased " + tier.getName() + ".");
            open(p);
        } else {
            p.sendMessage("§cCannot upgrade: either insufficient SP, maxed, or robbery level too low.");
        }
    }
}
