package dev.railbound.carriage;

import dev.railbound.trainset.design.FrameShape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Collision shapes for frame blocks, defined facing north and rotated for each horizontal facing. */
public final class FrameShapes {
    /** At the top of the block: layer 0 is at bogey height, so people walk level with the bogey's top. */
    public static final VoxelShape FLOOR_SLAB = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape WALL_LEFT = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_RIGHT = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape ROOF = Block.box(0, 4, 0, 16, 12, 16);
    private static final VoxelShape ROOF_WALL_LEFT_LOWER = Block.box(0, 0, 0, 2, 4, 16);
    private static final VoxelShape ROOF_WALL_RIGHT_LOWER = Block.box(14, 0, 0, 16, 4, 16);
    private static final VoxelShape PARTITION = Block.box(0, 0, 0, 16, 16, 2);

    private static final Map<FrameShape, Map<Direction, VoxelShape>> CACHE = new EnumMap<>(FrameShape.class);

    static {
        for (FrameShape shape : FrameShape.values()) {
            VoxelShape north = northShape(shape);
            Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                byFacing.put(facing, rotate(north, facing));
            }
            CACHE.put(shape, byFacing);
        }
    }

    private FrameShapes() {}

    public static VoxelShape get(FrameShape shape, Direction facing) {
        return CACHE.get(shape).get(facing);
    }

    private static VoxelShape northShape(FrameShape shape) {
        return switch (shape) {
            case FLOOR -> FLOOR_SLAB;
            case FLOOR_WALL_LEFT -> Shapes.or(FLOOR_SLAB, WALL_LEFT);
            case FLOOR_WALL_RIGHT -> Shapes.or(FLOOR_SLAB, WALL_RIGHT);
            case WALL_LEFT -> WALL_LEFT;
            case WALL_RIGHT -> WALL_RIGHT;
            case ROOF -> ROOF;
            case ROOF_WALL_LEFT -> Shapes.or(ROOF, ROOF_WALL_LEFT_LOWER);
            case ROOF_WALL_RIGHT -> Shapes.or(ROOF, ROOF_WALL_RIGHT_LOWER);
            case PARTITION -> PARTITION;
            case FULL -> Shapes.block();
        };
    }

    /** Rotates a north-facing shape clockwise (seen from above) to the given facing. */
    static VoxelShape rotate(VoxelShape north, Direction facing) {
        if (facing == Direction.NORTH) {
            return north;
        }
        VoxelShape result = Shapes.empty();
        for (AABB box : north.toAabbs()) {
            AABB rotated = switch (facing) {
                case EAST -> new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
                case SOUTH -> new AABB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY, 1 - box.minZ);
                case WEST -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
                default -> box;
            };
            result = Shapes.or(result, Shapes.create(rotated));
        }
        return result.optimize();
    }
}
