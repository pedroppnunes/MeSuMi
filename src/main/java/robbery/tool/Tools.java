package robbery.tool;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import robbery.number.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a usable tool in the Robbery plugin.
 * <p>
 * Each tool has:
 * <ul>
 *     <li>A name</li>
 *     <li>A colored display name</li>
 *     <li>A speed/damage multiplier</li>
 *     <li>An associated Bukkit {@link Material}</li>
 *     <li>A price (integer)</li>
 * </ul>
 * <p>
 * The class also provides a method to generate an {@link ItemStack} representing this tool
 * with a formatted display name and lore.
 */
public class Tools {
    private final String name;
    private final String colorname;
    private final double damage;
    private final Material material;
    private final long price;

    /**
     * Constructs a new Tool instance.
     *
     * @param name the plain name of the tool
     * @param damage the speed/damage multiplier
     * @param material the Bukkit material representing the tool
     * @param price the cost of the tool
     * @param colorname the colored display name (Minecraft formatting codes supported)
     */
    public Tools(String name, double damage, Material material, long price, String colorname) {
        this.name = name;
        this.colorname = ChatColor.translateAlternateColorCodes('&', colorname);
        this.damage = damage;
        this.material = material;
        this.price = price;
    }

    /**
     * Gets the damage/speed multiplier of this tool.
     *
     * @return the damage multiplier
     */
    public double getDamage() {
        return damage;
    }

    /**
     * Gets the plain name of the tool.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Bukkit material representing this tool.
     *
     * @return the material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Gets the price of the tool.
     *
     * @return the price
     */
    public long getPrice() {
        return price;
    }

    /**
     * Returns the price formatted as a human-readable string.
     *
     * @return the formatted price
     */
    public String getPriceformatted() {
        return NumberFormatter.formatLong(price);
    }

    /**
     * Gets the colored display name of this tool.
     *
     * @return the colored name
     */
    public String getColorname() {
        return colorname;
    }

    /**
     * Generates a Bukkit {@link ItemStack} representing this tool with:
     * <ul>
     *     <li>Display name with colors</li>
     *     <li>Lore showing speed/damage</li>
     *     <li>Hidden default attributes</li>
     * </ul>
     *
     * @return the formatted ItemStack
     */
    public ItemStack getItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorname);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Speed: +" + ChatColor.AQUA + (int)(damage * 10));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setCustomModelData(1);

        item.setItemMeta(meta);

        return item;
    }
}
