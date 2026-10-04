package dev.railbound.steam;

import java.util.List;
import java.util.Optional;

/**
 * How a train's steam locos limit Create's speeds, how hard a loco is working, and when it chuffs. Speeds are in
 * metres (blocks) per second, as in Create's config.
 */
public final class TrainPower {
    /** Share of effort from running speed; the rest comes from accelerating. */
    static final double SPEED_EFFORT = 0.6;
    /** Degrees of driving-wheel turn per chuff: two double-acting cylinders give four beats a turn. */
    static final double DEGREES_PER_CHUFF = 90;
    /** Create brakes with the acceleration figure too, so it never falls below this share: brakes need no steam. */
    static final double BRAKE_SHARE = 0.5;

    private TrainPower() {}

    /** One loco: its design speeds and how hard its boiler can pull right now (see {@link Boiler#pull()}). */
    public record Loco(double topSpeed, double curveSpeed, double acceleration, double pull) {}

    public record Limits(double topSpeed, double curveSpeed, double acceleration) {}

    /**
     * The train's limits: the slowest design sets the speeds, scaled by the best pull any loco has (a loco with
     * steam can haul the others). Acceleration only drops to {@link #BRAKE_SHARE}, since Create also brakes with
     * it. Empty when the train has none of our locos, so Create's own speeds apply.
     */
    public static Optional<Limits> limits(List<Loco> locos) {
        if (locos.isEmpty()) {
            return Optional.empty();
        }
        double top = Double.MAX_VALUE, curve = Double.MAX_VALUE, acceleration = Double.MAX_VALUE, pull = 0;
        for (Loco loco : locos) {
            top = Math.min(top, loco.topSpeed());
            curve = Math.min(curve, loco.curveSpeed());
            acceleration = Math.min(acceleration, loco.acceleration());
            pull = Math.max(pull, loco.pull());
        }
        return Optional.of(new Limits(top * pull, curve * pull, acceleration * (BRAKE_SHARE + (1 - BRAKE_SHARE) * pull)));
    }

    /** How hard the engine works, 0 to 1: more the faster it runs, and more again while it accelerates. */
    public static double effort(double speed, double targetSpeed, double topSpeed) {
        double running = topSpeed <= 0 ? 0 : Math.min(1, Math.abs(speed) / topSpeed);
        double accelerating = Math.abs(targetSpeed) > Math.abs(speed) + 1e-6 ? 1 - SPEED_EFFORT : 0;
        return Math.min(1, SPEED_EFFORT * running + accelerating);
    }

    /** Chuffs between two driving-wheel angles (degrees, either direction): one per quarter turn crossed. */
    public static int chuffs(double fromDegrees, double toDegrees) {
        return (int) Math.abs(Math.floor(toDegrees / DEGREES_PER_CHUFF) - Math.floor(fromDegrees / DEGREES_PER_CHUFF));
    }
}
