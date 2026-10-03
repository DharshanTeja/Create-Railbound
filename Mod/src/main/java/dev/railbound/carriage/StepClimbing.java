package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Boarding steps are climbed from outside the carriage: you count as on the ladder while touching the step's outer
 * face, below the floor. (You can only reach the step from the ground, a block below it, so vanilla's "feet inside
 * the ladder block" rule never applies; the step mixins use this instead.)
 */
public final class StepClimbing {
    /** How close (blocks) to the outer face still counts as touching it. */
    static final double REACH = 0.1;
    /** The ladder runs from the bottom of the step block up to the floor. */
    static final double LADDER_TOP = 7 / 16.0;

    private StepClimbing() {}

    public static boolean touchesLadder(AABB entity, BlockPos step, Direction outward) {
        double x0 = step.getX(), y0 = step.getY(), z0 = step.getZ();
        double x1 = x0 + 1, y1 = y0 + LADDER_TOP, z1 = z0 + 1;
        AABB face = switch (outward) {
            case EAST -> new AABB(x1, y0, z0, x1 + REACH, y1, z1);
            case WEST -> new AABB(x0 - REACH, y0, z0, x0, y1, z1);
            case SOUTH -> new AABB(x0, y0, z1, x1, y1, z1 + REACH);
            case NORTH -> new AABB(x0, y0, z0 - REACH, x1, y1, z0);
            default -> throw new IllegalArgumentException("Steps face sideways, not " + outward);
        };
        return entity.intersects(face);
    }
}
