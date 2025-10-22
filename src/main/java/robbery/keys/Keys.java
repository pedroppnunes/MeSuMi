package robbery.keys;

import org.bukkit.*;
import robbery.player.PlayerData;

/**
 * Represents a store key in the Robbery plugin.
 * <p>
 * Each key has a store name, base price, order, and a colored display name.
 * The price can be modified by a player's prestige boost using {@link PlayerData}.
 */
public class Keys {

    /** The plain name of the store. */
    private final String name;

    /** The base price of the store key before prestige adjustments. */
    private final int price;

    /** The order number of the store (1-12). */
    private final int order;

    /** The display name of the store key with color codes translated. */
    private final String colorname;
    private final String id;

    /**
     * Constructs a new store key.
     *
     * @param name       The plain name of the store.
     * @param price      The base price of the key.
     * @param order      The store's order number.
     * @param colorname  The display name with color codes.
     */
    public Keys(String name, int price, int order, String colorname, String id) {
        this.name = name;
        this.price = price;
        this.order = order;
        this.colorname = ChatColor.translateAlternateColorCodes('&', colorname);
        this.id = id;
    }

    /**
     * Returns the plain store name.
     *
     * @return The store's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the price of the key for a given player, taking into account
     * their prestige boost.
     *
     * @param p The player data containing prestige boost.
     * @return The price after prestige adjustments.
     */
    public double getPrice(PlayerData p) {
        return KeyManager.applyPrestigeIncrease(price, p);
    }

    /**
     * Returns the price formatted as a human-readable string with K, M, B suffixes.
     *
     * @param p The player data containing prestige boost.
     * @return Formatted price string.
     */
    public String getPriceformatted(PlayerData p) {
        return KeyManager.formatNumber(KeyManager.applyPrestigeIncrease(price, p));
    }

    /**
     * Returns the store's order number.
     *
     * @return The store order (1-12).
     */
    public int getOrder() {
        return order;
    }

    /**
     * Returns the colored display name of the store key.
     *
     * @return The display name with color codes translated.
     */
    public String getColorname() {
        return colorname;
    }
    public String getId() {
        return id;
    }

}
