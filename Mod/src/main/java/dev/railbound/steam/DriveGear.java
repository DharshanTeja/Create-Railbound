package dev.railbound.steam;

/**
 * Where a steam loco's motion is at a given crank angle, in model pixels (y up, z towards the rear). Angles are
 * radians about the x axis, the same way the renderer turns the driving wheels; at angle 0 the crank pins are
 * straight above their axles. The coupling rod rides the crank pins without turning; the connecting rod runs from
 * the crosshead, which slides on a level line ahead of the main axle, to the main axle's crank pin.
 */
public record DriveGear(double axleY, double axleZ, double crankRadius, double crossheadY, double rodLength) {

    /** The main axle's crank pin, as {y, z}. */
    public double[] crankPin(double angle) {
        return new double[] {axleY + crankRadius * Math.cos(angle), axleZ + crankRadius * Math.sin(angle)};
    }

    /** How far the coupling rod has moved from where it was modelled (angle 0), as {dy, dz}. */
    public double[] couplingRodOffset(double angle) {
        return new double[] {crankRadius * (Math.cos(angle) - 1), crankRadius * Math.sin(angle)};
    }

    /** The crosshead pin's z: the rod's length back from the crank pin, on the crosshead line. */
    public double crossheadZ(double angle) {
        double[] pin = crankPin(angle);
        double rise = pin[0] - crossheadY;
        return pin[1] - Math.sqrt(rodLength * rodLength - rise * rise);
    }

    /** How far the crosshead (and piston rod) has slid from where it was modelled. */
    public double crossheadOffset(double angle) {
        return crossheadZ(angle) - crossheadZ(0);
    }

    /** How far the connecting rod has swung about its crosshead end from where it was modelled. */
    public double rodAngle(double angle) {
        return slope(angle) - slope(0);
    }

    /**
     * The renderer's rotateX angle for the connecting rod about its crosshead end. rotateX turns +y towards +z,
     * which lowers a slope measured from +z towards +y, so it is the swing with its sign flipped.
     */
    public double rodTurn(double angle) {
        return -rodAngle(angle);
    }

    /**
     * The wheel angle after rolling a distance (blocks, positive towards the front, which is -z). The top of a rolling
     * wheel runs forward, and rotateX turns the top towards +z, so rolling forward lowers the angle.
     */
    public static double rolled(double angle, double distance, double wheelRadiusPixels) {
        return angle - distance / (wheelRadiusPixels / 16);
    }

    private double slope(double angle) {
        double[] pin = crankPin(angle);
        return Math.atan2(pin[0] - crossheadY, pin[1] - crossheadZ(angle));
    }

    /** The two sides are quartered: the left cranks lead the right by a quarter turn, so the engine always starts. */
    public static double sideAngle(double angle, boolean left) {
        return left ? angle + Math.PI / 2 : angle;
    }

    /**
     * How far (blocks, + to the right) a part at partZ must slide sideways to stay over the rails on a curve. The body
     * runs straight between its trucks (at bogeyA and bogeyB, along design z); the rails bow out from that line by
     * the circle's sagitta, most at the middle and nothing at the trucks. Curvature is + for a right turn.
     */
    public static double curveShift(double curvature, double partZ, double bogeyA, double bogeyB) {
        double half = Math.abs(bogeyB - bogeyA) / 2;
        double fromMiddle = partZ - (bogeyA + bogeyB) / 2;
        double shift = -curvature * (half * half - fromMiddle * fromMiddle) / 2;
        return Math.max(-MAX_CURVE_SHIFT, Math.min(MAX_CURVE_SHIFT, shift));
    }

    /**
     * The furthest the wheels slide out (blocks): a real loco could not take Create's tightest curves, and wheels
     * slid further than this look detached from the body.
     */
    public static final double MAX_CURVE_SHIFT = 1 / 3.0;

    /** Turn per block rolled (radians, + to the right) between two forward headings, over a distance along them. */
    public static double curvature(net.minecraft.world.phys.Vec3 before, net.minecraft.world.phys.Vec3 after, double distance) {
        double cross = before.z * after.x - before.x * after.z;
        double dot = before.x * after.x + before.z * after.z;
        return Math.atan2(-cross, dot) / distance;
    }

    /** The driving wheels' angle when the truck wheels have turned truckAngle: both roll the same distance. */
    public static double driverAngle(double truckAngle, double truckRadius, double driverRadius) {
        return truckAngle * truckRadius / driverRadius;
    }
}
