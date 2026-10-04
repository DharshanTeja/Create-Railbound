package dev.railbound.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * How a trackside water crane reaches a loco. Its telescopic arm swings round towards the loco's tank filler, slides
 * out until the hose hangs over it and lowers the hose onto it, so it lines up wherever the crane stands within reach.
 * Parked, the arm lies along the track (a quarter turn from the crane's facing) with its hose drawn up.
 */
public final class WaterCrane {
    /** How far from the column a filler may be (blocks, level): clear of the column, up to three blocks out. */
    static final double MIN_REACH = 0.75;
    static final double MAX_REACH = 3.0;
    /** The arm's length parked, and how high it swings above the crane's base (clear of a loco's tanks and boiler). */
    static final double ARM = 1.25;
    public static final double ARM_HEIGHT = 4.5;
    /** The hose collar under the arm and the nozzle at the hose end (blocks). */
    public static final double COLLAR = 1 / 16.0;
    public static final double NOZZLE = 1 / 16.0;
    /** Degrees: parked, the arm lies along the track; filling, it points at the filler. */
    public static final double PARKED_ANGLE = 90;
    /** The arm's parked pose, with its hose drawn up. */
    public static final Aim PARKED = new Aim(PARKED_ANGLE, ARM, 6 / 16.0);
    /** Share of the swing done per tick (a full swing takes a second). */
    static final double SWING_PER_TICK = 0.05;

    /** The crane is five blocks tall: the base (section 0) and four column blocks, the top one holding the arm. */
    public static final int TOP = 4;

    private WaterCrane() {}

    /** Whether a crane block stays: each needs the section below it (but the base) and the one above it (but the top). */
    public static boolean sectionStands(int section, boolean belowFits, boolean aboveFits) {
        return (section == 0 || belowFits) && (section == TOP || aboveFits);
    }

    /**
     * The arm's pose: its turn from the crane's facing (degrees, positive towards the facing's clockwise side seen from
     * above), how far out its hose hangs (blocks) and the hose length (blocks).
     */
    public record Aim(double angle, double reach, double hose) {}

    /** Whether the crane, its base's bottom centre at base, can reach a filler there. */
    public static boolean inReach(Vec3 base, Vec3 filler) {
        double level = Math.hypot(filler.x - base.x, filler.z - base.z);
        return level >= MIN_REACH - 1e-9 && level <= MAX_REACH + 1e-9 && hoseTo(base, filler) > 0;
    }

    /** The pose that puts the nozzle on the filler. */
    public static Aim aim(Vec3 base, Direction facing, Vec3 filler) {
        double dx = filler.x - base.x, dz = filler.z - base.z;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));   // Minecraft yaw of the way to the filler
        double angle = net.minecraft.util.Mth.wrapDegrees(facing.toYRot() - yaw);
        return new Aim(angle, Math.hypot(dx, dz), hoseTo(base, filler));
    }

    private static double hoseTo(Vec3 base, Vec3 filler) {
        return ARM_HEIGHT - (filler.y - base.y) - COLLAR - NOZZLE;
    }

    /** Where the nozzle's end is in the world for a pose. */
    public static Vec3 nozzle(Vec3 base, Direction facing, Aim aim) {
        double yaw = Math.toRadians(facing.toYRot() - aim.angle());
        return base.add(-Math.sin(yaw) * aim.reach(), ARM_HEIGHT - COLLAR - aim.hose() - NOZZLE, Math.cos(yaw) * aim.reach());
    }

    /** The pose at swing progress 0 (parked) to 1 (on the filler), easing in and out. */
    public static Aim swung(Aim target, double progress) {
        double p = Math.max(0, Math.min(1, progress));
        double s = p * p * (3 - 2 * p);
        double turn = net.minecraft.util.Mth.wrapDegrees(target.angle() - PARKED.angle());   // the short way round
        return new Aim(PARKED.angle() + turn * s, PARKED.reach() + (target.reach() - PARKED.reach()) * s,
                PARKED.hose() + (target.hose() - PARKED.hose()) * s);
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

    /** One tick of swinging out (while filling) or back (otherwise). */
    public static double swingTowards(double progress, boolean out) {
        double next = progress + (out ? SWING_PER_TICK : -SWING_PER_TICK);
        return Math.round(Math.max(0, Math.min(1, next)) * 1e9) / 1e9;
    }
}
