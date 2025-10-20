package robbery.keys;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import robbery.Robbery;

public class DoorArea {
    private final Location corner1;
    private final Location corner2;

    public DoorArea(Location corner1, Location corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    /**
     * Show a block only to a specific player using ProtocolLib.
     * Updates blocks gradually to avoid server lag.
     */
    public void setMaterial(Player player, Material material) {
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        Bukkit.getScheduler().runTaskAsynchronously(Robbery.getInstance(), () -> {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPosition pos = new BlockPosition(x, y, z);
                        WrappedBlockData blockData = WrappedBlockData.createData(material);

                        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                                .createPacket(PacketType.Play.Server.BLOCK_CHANGE);
                        packet.getBlockPositionModifier().write(0, pos);
                        packet.getBlockData().write(0, blockData);

                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public boolean isChunkLoadedFor(Player player) {
        if (corner1 == null || corner2 == null) return false;
        if (!corner1.getWorld().equals(player.getWorld())) return false;

        // Get all chunks covered by this door area
        int minChunkX = Math.min(corner1.getBlockX() >> 4, corner2.getBlockX() >> 4);
        int maxChunkX = Math.max(corner1.getBlockX() >> 4, corner2.getBlockX() >> 4);
        int minChunkZ = Math.min(corner1.getBlockZ() >> 4, corner2.getBlockZ() >> 4);
        int maxChunkZ = Math.max(corner1.getBlockZ() >> 4, corner2.getBlockZ() >> 4);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (player.getWorld().isChunkLoaded(cx, cz)) {
                    return true;
                }
            }
        }
        return false;
    }



}
