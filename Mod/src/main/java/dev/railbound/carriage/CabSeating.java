package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which controls a cab seat drives. As Create pairs a conductor seat with the Train Controls facing it right beside it,
 * and also, for our cab controls, across one open cell such as a doorway, so a cab can keep its doorways clear. Sitting
 * down in a driver's seat takes its controls (see the client's CabDriving); a mob there drives on its schedule.
 */
public final class CabSeating {
    /** How far from its seat cab controls may stand: beside it, or across one open cell. */
    public static final int REACH = 2;

    private CabSeating() {}

    /**
     * The controls a seat drives, or empty for a passenger seat. controlsFacing gives the facing of the controls at a
     * position, if there are any; open says whether a position is free to reach across.
     */
    public static Optional<BlockPos> controlsFor(BlockPos seat, Function<BlockPos, Optional<Direction>> controlsFacing,
                                                 Predicate<BlockPos> open) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Optional<BlockPos> found = controlsToward(seat, direction, controlsFacing, open);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** The controls facing back at a seat from one way: right beside it, or across one open cell. */
    public static Optional<BlockPos> controlsToward(BlockPos seat, Direction direction,
                                                    Function<BlockPos, Optional<Direction>> controlsFacing, Predicate<BlockPos> open) {
        for (int reach = 1; reach <= REACH; reach++) {
            BlockPos candidate = seat.relative(direction, reach);
            if (controlsFacing.apply(candidate).filter(facing -> facing == direction.getOpposite()).isPresent()) {
                return Optional.of(candidate);
            }
            if (!open.test(candidate)) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
