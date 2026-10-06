package dev.railbound.carriage;

import dev.railbound.trainset.design.FrameShape;
import dev.railbound.trainset.design.HiddenPart;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
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
    /** A cab step's ladder runs up to its tread at the top of the cell, level with a loco's full-height floor. */
    private static final VoxelShape CAB_STEP_LADDER = Block.box(0, 0, 0, 16, 15, 3);
    private static final VoxelShape CAB_STEP_TREAD = Block.box(0, 15, 0, 16, 16, 16);
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

    /**
     * The direction out of the carriage for a door cell: sideways for a side column, or out of the front (local z 0)
     * or rear end for an end door on the centre column.
     */
    public static Direction doorOutward(Direction facing, BlockPos local, int length) {
        if (local.getX() != 0) {
            return outward(facing, local.getX());
        }
        if (local.getZ() == 0) {
            return facing;
        }
        if (local.getZ() == length - 1) {
            return facing.getOpposite();
        }
        throw new IllegalArgumentException("A centre-column door must be at a carriage end, not z=" + local.getZ());
    }

    /**
     * Train Controls face the conductor seat next to them, ahead (towards the carriage front, local z - 1) or behind:
     * Create only lets a mob on that seat drive and run schedules when the controls face back at it.
     */
    public static Direction controlsFacing(Direction facing, boolean seatAhead) {
        return seatAhead ? facing : facing.getOpposite();
    }

    /**
     * Seats face the carriage front, but a conductor seat with its controls only behind it faces back at them: a cab's
     * position for driving bunker-first.
     */
    public static Direction seatFacing(Direction facing, boolean controlsAhead, boolean controlsBehind) {
        return controlsBehind && !controlsAhead ? facing.getOpposite() : facing;
    }

    /** A seat's side is counted from its own facing, so one facing back has the carriage side on its other hand. */
    public static PlaceholderSide seatSide(PlaceholderSide side, boolean facingBack) {
        return facingBack ? side.mirrored() : side;
    }

    /**
     * A cab's control stand: the carriage's outer wall, and a slim stand against it right in front of the driver's seat
     * (which it faces), leaving the aisle clear and the far side of the cell free (it may be the cab's back wall). Its
     * side is counted from its own facing, as a seat's is.
     */
    public static VoxelShape cabControlsShape(Direction facing, PlaceholderSide side) {
        VoxelShape north = switch (side) {
            case LEFT -> Shapes.or(WALL_LEFT, Block.box(2, 0, 0, 12, 16, 6));
            case RIGHT -> Shapes.or(WALL_RIGHT, Block.box(4, 0, 0, 14, 16, 6));
            case NONE -> Block.box(3, 0, 0, 13, 16, 6);
        };
        return FrameShapes.rotate(north.optimize(), facing);
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
    public static VoxelShape stepShape(Direction outward, boolean high) {
        return high ? Shapes.or(FrameShapes.rotate(CAB_STEP_LADDER, outward), CAB_STEP_TREAD)
                : Shapes.or(FrameShapes.rotate(STEP_LADDER, outward), FrameShapes.FLOOR_SLAB);
    }

    /** A step beside a full-height floor (a loco cab) is a high one, climbing right up to that floor. */
    public static boolean stepIsHigh(Optional<HiddenPart> inward) {
        return inward.map(part -> part.type() == PartType.FRAME && part.shape().equals(Optional.of(FrameShape.FULL)))
                .orElse(false);
    }

    /** A closed door is a panel on its outward face; the upper half also keeps the ceiling above the doorway. */
    public static VoxelShape doorShape(Direction outward, boolean open, DoubleBlockHalf half) {
        VoxelShape leaf = open ? Shapes.empty() : FrameShapes.rotate(DOOR_PANEL, outward);
        return half == DoubleBlockHalf.UPPER ? Shapes.or(leaf, FrameShapes.CEILING) : leaf;
    }
}
