package dev.railbound.carriage;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hitbox boxes for blocks modelled looking south (+z), turned to face the way the block does. */
public final class FacingBoxes {
    private FacingBoxes() {}

    /** A box (pixels) given looking south (+z), turned to face the other way. */
    public static VoxelShape box(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case NORTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            case WEST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }
}
