package robbery.outpost;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a rectangular cuboid region in a specific world, used for defining
 * Outpost boundaries.
 * <p>
 * The region is defined by two sets of coordinates (x1, y1, z1) and (x2, y2, z2).
 * The constructor automatically calculates the minimum and maximum values for each axis.
 */
public class OutpostRegion {

    private final World world;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    /**
     * Constructs a new OutpostRegion given a world and two opposite corners.
     *
     * @param world The world where the region exists.
     * @param x1 X-coordinate of the first corner.
     * @param y1 Y-coordinate of the first corner.
     * @param z1 Z-coordinate of the first corner.
     * @param x2 X-coordinate of the second corner.
     * @param y2 Y-coordinate of the second corner.
     * @param z2 Z-coordinate of the second corner.
     */
    public OutpostRegion(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world  = world;
        this.minX   = Math.min(x1, x2);
        this.maxX   = Math.max(x1, x2);
        this.minY   = Math.min(y1, y2);
        this.maxY   = Math.max(y1, y2);
        this.minZ   = Math.min(z1, z2);
        this.maxZ   = Math.max(z1, z2);
    }

    /**
     * Checks if a given location is inside this region.
     *
     * @param loc The location to check.
     * @return True if the location is inside the region and in the correct world, false otherwise.
     */
    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
