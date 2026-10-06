package dev.railbound.coupling;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules for splitting a stopped train at a coupler and joining two trains whose couplers meet, apart from Create's
 * objects. Carriages run front (index 0) to back; Create keeps the spacing between neighbouring carriages' bogeys.
 */
public final class TrainCut {
    /** Couplers meeting at no more than this (blocks per tick, as Create counts train speed: 1 block a second) join. */
    public static final double JOIN_SPEED = 1 / 20.0;

    private TrainCut() {}

    /**
     * A train cut behind carriage {@code gap}: the front half has carriages 0 to gap, the back half the rest. The half
     * with a loco (Train Controls) keeps the train, its name, schedule and conductor; with locos in both, the front.
     */
    public record Split(int frontCount, List<Integer> frontSpacing, List<Integer> backSpacing, boolean frontKeeps) {}

    public enum Order { MOVING_FIRST, OTHER_FIRST }

    /** Which train comes first once joined, and whether the other train is turned round first. */
    public record Join(Order order, boolean reverseOther) {}

    public enum Contact { JOIN, BUMP }

    public static Split split(int carriages, List<Integer> spacing, int gap, boolean frontHasControls,
                              boolean backHasControls) {
        if (gap < 0 || gap >= carriages - 1) {
            throw new IllegalArgumentException("no gap " + gap + " in a train of " + carriages);
        }
        return new Split(gap + 1, List.copyOf(spacing.subList(0, gap)), List.copyOf(spacing.subList(gap + 1, spacing.size())),
                frontHasControls || !backHasControls);
    }

    /**
     * How two trains join from the ends that met: the moving train's front or rear against the other train's front or
     * rear. Front to rear joins as they stand; front to front or rear to rear first turns the other train round.
     */
    public static Join join(boolean movingFront, boolean otherFront) {
        boolean reverseOther = movingFront == otherFront;
        return new Join(movingFront ? Order.OTHER_FIRST : Order.MOVING_FIRST, reverseOther);
    }

    /** The joined train's spacing: the first train's, the new gap between the two, then the second's. */
    public static List<Integer> joinedSpacing(List<Integer> first, int gap, List<Integer> second) {
        List<Integer> out = new ArrayList<>(first);
        out.add(gap);
        out.addAll(second);
        return out;
    }

    /**
     * Whether a train standing at a station is still there once coupled to or cut from more carriages. Create stops a
     * train with its front at the station, or its back when it arrived backwards; that end must stay outermost. A train
     * that keeps its front has carriages joined (or cut) only behind it, one that keeps its back only in front.
     */
    public static boolean stillAtStation(boolean keepsFront, boolean arrivedBackwards) {
        return keepsFront != arrivedBackwards;
    }

    /** What meeting couplers do at a combined speed (blocks per tick): join slowly, or bump and stop. */
    public static Contact contact(double combinedSpeed) {
        return combinedSpeed <= JOIN_SPEED ? Contact.JOIN : Contact.BUMP;
    }

    /** A carriage's heading and pitch (degrees), as Create's carriage entity has them. */
    public record Pose(float yaw, float pitch) {}

    /**
     * How many carriages, counted from the end standing at the station (the front, or the back of a train that arrived
     * backwards), fit Create's disassembly, which lays a train out in a straight line from the station: each must be
     * level and square to the grid, and along the same line as the first. A carriage on a curve or a slope, past a
     * corner, or not loaded ends the count; zero when not even the first fits.
     */
    public static int fitAtStation(List<java.util.Optional<Pose>> carriages, boolean fromBack) {
        Float line = null;
        for (int i = 0; i < carriages.size(); i++) {
            java.util.Optional<Pose> pose = carriages.get(fromBack ? carriages.size() - 1 - i : i);
            if (pose.isEmpty() || Math.abs(pose.get().pitch()) > SQUARE || offSquare(pose.get().yaw(), 90) > SQUARE) {
                return i;
            }
            float along = pose.get().yaw();
            if (line == null) {
                line = along;
            } else if (offSquare(along - line, 180) > SQUARE) {
                return i;
            }
        }
        return carriages.size();
    }

    /** Degrees a carriage may be off level or off square and still count as straight (float rounding only). */
    private static final float SQUARE = 1e-3f;

    /** How far an angle is from the nearest whole number of steps. */
    private static float offSquare(float degrees, float step) {
        float rest = ((degrees % step) + step) % step;
        return Math.min(rest, step - rest);
    }
}
