package robbery.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import robbery.items.Items;
import robbery.keys.KeyManager;
import robbery.player.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a backpack in the Robbery plugin.
 * <p>
 * Each backpack has a name, capacity, price, material, color code, and a list of contained items.
 * Provides methods to add, sell, or empty items, and generate a display {@link ItemStack}.
 * </p>
 */
public class Backpacks {

    /** Display name of the backpack. */
    private final String name;

    /** Maximum number of items the backpack can hold. */
    private final int capacity;

    /** Price of the backpack in in-game currency. */
    private final int price;

    /** Material used for the backpack item representation. */
    private final Material material;

    /** List of items currently stored in the backpack. */
    private final List<Items> backpack;

    /** Display name with color codes translated. */
    private final String colorname;

    /**
     * Creates a new backpack with the given parameters.
     *
     * @param name display name of the backpack
     * @param capacity maximum capacity of the backpack
     * @param items initial list of items to store, can be null
     * @param material material of the backpack item
     * @param price purchase price of the backpack
     * @param colorname color-coded name string using codes
     */
    public Backpacks(String name, int capacity, List<Items> items, Material material, int price, String colorname) {
        this.name = name;
        this.capacity = capacity;
        this.material = material;
        this.backpack = new ArrayList<>(capacity);
        this.price = price;
        if (items != null) {
            for (Items it : items) {
                this.backpack.add(it.copyForBackpack());
            }
        }
        this.colorname = ChatColor.translateAlternateColorCodes('&', colorname);
    }

    /** Copy constructor that creates a brand-new list and copies items */
    public Backpacks(Backpacks b, int extraSlots) {
        this.name = b.name;
        this.material = b.material;
        this.price = b.price;
        this.capacity = b.capacity + extraSlots;
        // create a new list; deep-copy every item from the template
        this.backpack = new ArrayList<>(this.capacity);
        if (b.backpack != null) {
            for (Items it : b.backpack) {
                this.backpack.add(it.copyForBackpack());
            }
        }
        this.colorname = b.colorname;
    }

    /**
     * Sells all items in the backpack, clearing it and returning the total value.
     *
     * @param p the player whose boost affects item value
     * @return total value of sold items formatted as a double
     */
    public double sell(PlayerData p) {
        double t = getTotal(p);
        this.backpack.clear();
        return Double.parseDouble(KeyManager.formatDouble(t));
    }

    /** @return the price of the backpack. */
    public int getPrice() {
        return price;
    }

    /** @return formatted price as a String with thousand separators. */
    public String getPriceformatted() {
        return KeyManager.formatNumber(price);
    }

    /**
     * Serializes the backpack contents into an underscore-separated string.
     *
     * @return serialized string of item names
     */
    @Override
    public String toString() {
        String back = "";
        for (Items i : backpack) {
            back = back.concat(i.getId() + ";");
        }
        return back;
    }

    public List<Items> getItems(){
        return backpack;
    }


    /** @return the maximum capacity of the backpack. */
    public int getcapacity() {
        return capacity;
    }

    /** @return the display name of the backpack. */
    public String getName() {
        return name;
    }

    /**
     * Calculates the total value of all items in the backpack for a given player.
     *
     * @param p player whose boost is applied to item values
     * @return total value of items
     */
    public double getTotal(PlayerData p) {
        double t = 0;
        for (Items i : backpack) {
            t += i.getValue() * p.getBoost();
        }
        return t;
    }

    /** @return the number of items currently in the backpack. */
    public int getSize() {
        return backpack.size();
    }

    /**
     * Adds an item to the backpack if there is available capacity.
     *
     * @param i the item to add
     */
    public void addBackpackItem(Items i) {
        if (backpack.size() < capacity)
            backpack.add(i.copyForBackpack());
    }

    /** Empties the backpack by clearing all items. */
    public void emptyBackpack() {
        backpack.clear();
    }

    /** @return true if the backpack is full, false otherwise. */
    public boolean isFull() {
        return backpack.size() == capacity;
    }

    /** @return the color-coded display name of the backpack. */
    public String getColorname() {
        return colorname;
    }

    /**
     * Creates a Bukkit {@link ItemStack} representing the backpack for inventory display.
     * <p>
     * The item includes its display name, lore showing capacity, and hides attributes.
     * </p>
     *
     * @return an {@link ItemStack} representing the backpack
     */
    public ItemStack getItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorname);
        List<String> lore = new ArrayList<>();
        String backname = BackpackManager.getBackpackNameR(name);
        assert backname != null;
        lore.add(ChatColor.GRAY + "Size: " + ChatColor.AQUA + Objects.requireNonNull(BackpackManager.getBackpackName(backname, 0)).getcapacity());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    /** @return the material name of the backpack as a string. */
    public String getMaterial() {
        return material.toString();
    }


}
