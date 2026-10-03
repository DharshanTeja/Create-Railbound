package dev.railbound.carriage;

import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Places a design's model (built in design block space, facing north) around its anchor block: the same
 * mapping as {@link CarriageFootprint}, as a turn about the anchor block's centre.
 */
public final class CarriageTransform {
    private CarriageTransform() {}

    /** Degrees about +Y that turn a north-facing model to face {@code facing}. */
    public static float yRotation(Direction facing) {
        int clockwiseSteps = (facing.get2DDataValue() + 2) % 4;
        return -90 * clockwiseSteps;
    }

    /** Where a point in design block space lands, relative to the anchor block's lower corner. */
    public static Vec3 toAnchorRelative(ParsedDesign design, Direction facing, double x, double y, double z) {
        BlockPos anchor = design.anchor();
        double across = x - anchor.getX() - 0.5;
        double along = z - anchor.getZ() - 0.5;
        BlockPos right = BlockPos.ZERO.relative(facing.getClockWise());
        BlockPos back = BlockPos.ZERO.relative(facing.getOpposite());
        return new Vec3(across * right.getX() + along * back.getX() + 0.5,
                y - anchor.getY(),
                across * right.getZ() + along * back.getZ() + 0.5);
    }
}
