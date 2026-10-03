package dev.railbound.carriage;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/** Pure helpers for seats and doors: which half, which way out, and their shapes. */
public final class InteriorParts {
    /** On the floor layer: the floor is half a block up, and the cushion sits on it. */
    private static final VoxelShape SEAT_CUSHION = Block.box(0, 7, 0, 16, 15, 16);
    /** Behind the passenger, who faces the carriage front (north in the reference orientation); the model's back rises above. */
    private static final VoxelShape SEAT_BACK = Block.box(0, 15, 14, 16, 16, 16);
    private static final VoxelShape WALL_LEFT = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_RIGHT = Block.box(14, 0, 0, 16, 16, 16);
    /** A step's ladder on the outer (north) face below the floor, rotated to the step's outward direction. */
    private static final VoxelShape STEP_LADDER = Block.box(0, 0, 0, 16, 7, 3);
    /** A closed door panel on the north face, rotated to the door's outward direction. */
    private static final VoxelShape DOOR_PANEL = Block.box(0, 0, 0, 16, 16, 2);

    private InteriorParts() {}

    /** A door cell is the upper half when the cell below it is also a door. */
    public static DoubleBlockHalf doorHalf(boolean doorCellBelow) {
        return doorCellBelow ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
    }

    /** The direction out of the carriage for a side-column cell (local x of -1 is the left side). */
    public static Direction outward(Direction facing, int localX) {
        if (localX == 0) {
            throw new IllegalArgumentException("The centre column has no outside");
        }
        return localX > 0 ? facing.getClockWise() : facing.getCounterClockWise();
    }

    /** The carriage side a side-column seat backs onto (where its outer wall is), or empty for a centre seat. */
    public static Optional<Direction> seatOutward(Direction facing, PlaceholderSide side) {
        return switch (side) {
            case RIGHT -> Optional.of(facing.getClockWise());
            case LEFT -> Optional.of(facing.getCounterClockWise());
            case NONE -> Optional.empty();
        };
    }

    public static VoxelShape seatShape(Direction facing, PlaceholderSide side) {
        VoxelShape north = Shapes.or(SEAT_CUSHION, SEAT_BACK);
        if (side == PlaceholderSide.LEFT) {
            north = Shapes.or(north, WALL_LEFT);
        } else if (side == PlaceholderSide.RIGHT) {
            north = Shapes.or(north, WALL_RIGHT);
        }
        return FrameShapes.rotate(north.optimize(), facing);
    }

    /**
     * The ladder on the carriage's outer face below the floor (climbed from outside, see {@link StepClimbing}), plus
     * the floor itself, so the doorway above stays open and nobody can fall through.
     */
    public static VoxelShape stepShape(Direction outward) {
        return Shapes.or(FrameShapes.rotate(STEP_LADDER, outward), FrameShapes.FLOOR_SLAB);
    }

    public static VoxelShape doorShape(Direction outward, boolean open) {
        return open ? Shapes.empty() : FrameShapes.rotate(DOOR_PANEL, outward);
    }
}
