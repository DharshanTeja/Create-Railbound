package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

/**
 * A carriage's hitbox. Create gives every train carriage a square box as wide as the carriage is long, so it covers
 * any turn; ours instead follow the carriage: its blocks' extent, turned as the carriage is now, plus a margin for
 * the model's gangways and buffers that reach past the blocks.
 */
public final class CarriageBounds {
    /** Blocks: the gangways reach half a block past the carriage ends. */
    public static final double MARGIN = 0.5;

    private CarriageBounds() {}

    /** The contraption-local box that holds every block whole. */
    public static AABB extent(Iterable<BlockPos> blocks) {
        AABB box = null;
        for (BlockPos pos : blocks) {
            AABB block = new AABB(pos);
            box = box == null ? block : box.minmax(block);
        }
        return box == null ? new AABB(BlockPos.ZERO) : box;
    }

    /** The world box around the local extent's eight corners after toWorld, with the margin. */
    public static AABB world(AABB local, UnaryOperator<Vec3> toWorld) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            Vec3 v = toWorld.apply(new Vec3((corner & 1) == 0 ? local.minX : local.maxX,
                    (corner & 2) == 0 ? local.minY : local.maxY, (corner & 4) == 0 ? local.minZ : local.maxZ));
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            minZ = Math.min(minZ, v.z);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
            maxZ = Math.max(maxZ, v.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(MARGIN);
    }
}
