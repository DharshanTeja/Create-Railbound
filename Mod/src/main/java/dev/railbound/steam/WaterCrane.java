package dev.railbound.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where a trackside water crane's hose hangs and how its arm swings. The arm reaches out over the track in the
 * crane's facing; a stopped loco whose tank filler is within reach of the hose end gets filled.
 */
public final class WaterCrane {
    /** Blocks from the hose end to a tank filler's centre that still count as in reach. */
    static final double REACH = 2.0;
    /**
     * How far out the arm reaches (over a loco's near side tank, from a crane two blocks off the track centre), how
     * high it swings (clear of a loco's tanks and boiler) and where its hose end hangs, above the crane's base.
     */
    static final double ARM = 1.25;
    public static final double ARM_HEIGHT = 4.5;
    static final double HOSE_HEIGHT = 4.0;
    /** Degrees: parked, the arm lies along the track; filling, it points straight out over it. */
    public static final double PARKED_ANGLE = 90;
    /** Share of the swing done per tick (a full swing takes a second). */
    static final double SWING_PER_TICK = 0.05;

    private WaterCrane() {}

    public static Vec3 spout(BlockPos pos, Direction facing) {
        return Vec3.atBottomCenterOf(pos).add(facing.getStepX() * ARM, HOSE_HEIGHT, facing.getStepZ() * ARM);
    }

    public static boolean inReach(Vec3 spout, Vec3 filler) {
        return spout.distanceTo(filler) <= REACH + 1e-9;
    }

    /**
     * Which way a newly placed crane faces: towards the nearest track in a straight line, given how far each way
     * the track is (empty where there is none near).
     */
    public static java.util.Optional<Direction> towardsTrack(java.util.function.Function<Direction, java.util.OptionalInt> trackDistance) {
        Direction best = null;
        int nearest = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            java.util.OptionalInt distance = trackDistance.apply(direction);
            if (distance.isPresent() && distance.getAsInt() < nearest) {
                nearest = distance.getAsInt();
                best = direction;
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    /** The arm's angle from straight out (degrees) at swing progress 0 (parked) to 1 (out), easing in and out. */
    public static double armAngle(double progress) {
        double p = Math.max(0, Math.min(1, progress));
        return PARKED_ANGLE * (1 - p * p * (3 - 2 * p));
    }

    /** One tick of swinging out (while filling) or back (otherwise). */
    public static double swingTowards(double progress, boolean out) {
        double next = progress + (out ? SWING_PER_TICK : -SWING_PER_TICK);
        return Math.round(Math.max(0, Math.min(1, next)) * 1e9) / 1e9;
    }
}
