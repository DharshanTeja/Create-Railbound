package dev.railbound.steam;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveGearTest {
    private static final double TAU = Math.PI * 2;
    /** The tank engine: main axle at y -17, z -10, 6 px cranks, crosshead line at y -7, 75 px connecting rod. */
    private static final DriveGear GEAR = new DriveGear(-17, -10, 6, -7, 75);

    private static double distance(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    @Test
    void theCrankPinStartsStraightAboveTheAxleAndTurnsWithTheWheel() {
        assertArrayEquals(new double[] {-11, -10}, GEAR.crankPin(0), 1e-9);
        // a quarter turn about the x axis carries "up" round to +z, as the renderer's rotateX does
        assertArrayEquals(new double[] {-17, -4}, GEAR.crankPin(Math.PI / 2), 1e-9);
        assertArrayEquals(new double[] {-23, -10}, GEAR.crankPin(Math.PI), 1e-9);
    }

    @Test
    void theCouplingRodFollowsTheCrankPinWithoutTurning() {
        assertArrayEquals(new double[] {0, 0}, GEAR.couplingRodOffset(0), 1e-9);
        assertArrayEquals(new double[] {-6, 6}, GEAR.couplingRodOffset(Math.PI / 2), 1e-9);
        assertArrayEquals(new double[] {0, 0}, GEAR.couplingRodOffset(TAU), 1e-9);
    }

    @Test
    void theConnectingRodKeepsItsLength() {
        for (double angle = 0; angle < TAU; angle += 0.3) {
            double[] crosshead = {GEAR.crossheadY(), GEAR.crossheadZ(angle)};
            assertEquals(75, distance(crosshead, GEAR.crankPin(angle)), 1e-9, "at " + angle);
        }
    }

    @Test
    void theCrossheadRunsAheadOfTheAxleOverAStrokeOfTwoCranks() {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double angle = 0; angle < TAU; angle += 0.01) {
            double z = GEAR.crossheadZ(angle);
            assertTrue(z < -10, "the cylinder is in front of the axle");
            min = Math.min(min, z);
            max = Math.max(max, z);
        }
        assertEquals(12, max - min, 0.3, "stroke");
        assertEquals(0, GEAR.crossheadOffset(0), 1e-9);
    }

    @Test
    void theConnectingRodSwingsAboutItsCrossheadEnd() {
        assertEquals(0, GEAR.rodAngle(0), 1e-9);
        double[] crosshead = {GEAR.crossheadY(), GEAR.crossheadZ(Math.PI)};
        double[] pin = GEAR.crankPin(Math.PI);
        double rest = Math.atan2(GEAR.crankPin(0)[0] - GEAR.crossheadY(), GEAR.crankPin(0)[1] - GEAR.crossheadZ(0));
        assertEquals(Math.atan2(pin[0] - crosshead[0], pin[1] - crosshead[1]) - rest, GEAR.rodAngle(Math.PI), 1e-9);
        assertNotEquals(0, GEAR.rodAngle(Math.PI), 1e-3);
    }

    @Test
    void rodTurnIsTheRendererAngleThatCarriesTheModelledRodOntoThePin() {
        // the renderer turns parts with rotateX: y' = y cos a - z sin a, z' = y sin a + z cos a
        double angle = 2.0;
        double[] rest = {GEAR.crankPin(0)[0] - GEAR.crossheadY(), GEAR.crankPin(0)[1] - GEAR.crossheadZ(0)};
        double a = GEAR.rodTurn(angle);
        double[] turned = {rest[0] * Math.cos(a) - rest[1] * Math.sin(a), rest[0] * Math.sin(a) + rest[1] * Math.cos(a)};
        double[] wanted = {GEAR.crankPin(angle)[0] - GEAR.crossheadY(), GEAR.crankPin(angle)[1] - GEAR.crossheadZ(angle)};
        assertArrayEquals(wanted, turned, 1e-9);
    }

    @Test
    void rollingForwardTurnsTheWheelsBackwards() {
        // forward is towards -z: the wheel's top runs forward, which is a falling rotateX angle
        assertEquals(-Math.PI, DriveGear.rolled(0, Math.PI * 9.5 / 16, 9.5), 1e-9);
        assertEquals(Math.PI, DriveGear.rolled(0, -Math.PI * 9.5 / 16, 9.5), 1e-9);
    }

    @Test
    void onStraightTrackTheDrivingWheelsStayPut() {
        assertEquals(0, DriveGear.curveShift(0, 5.5, 3.5, 12.5), 1e-9);
    }

    @Test
    void onACurveTheWheelsFollowTheRailsAwayFromTheBodysStraightLine() {
        // the body runs straight between its trucks; the rails bow out from that line, most in the middle
        double k = 1 / 7.0;   // turning right, radius 7
        double front = DriveGear.curveShift(k, 5.5, 3.5, 12.5);
        double middle = DriveGear.curveShift(k, 7.375, 3.5, 12.5);
        assertTrue(middle < front && front < 0, "a right turn bows the rails out to the left");
        assertEquals(-(4.5 * 4.5 - 0.625 * 0.625) / 2 * k, middle, 1e-9);
        assertEquals(0, DriveGear.curveShift(k, 3.5, 3.5, 12.5), 1e-9, "on the truck the wheels are on the rails");
        assertEquals(-front, DriveGear.curveShift(-k, 5.5, 3.5, 12.5), 1e-9, "a left turn mirrors it");
    }

    @Test
    void curvatureIsTheTurnPerBlockRolled() {
        double turn = 0.1;   // radians to the right
        Vec3 north = new Vec3(0, 0, -1);
        Vec3 turnedRight = new Vec3(Math.sin(turn), 0, -Math.cos(turn));
        assertEquals(turn / 0.7, DriveGear.curvature(north, turnedRight, 0.7), 1e-9);
        assertEquals(turn / 0.7, DriveGear.curvature(north, turnedRight.yRot(0), 0.7), 1e-9);
        assertEquals(turn / 0.7, DriveGear.curvature(north, turnedRight, -0.7) * -1, 1e-9, "backing through it");
    }

    @Test
    void theLeftSideLeadsTheRightByAQuarterTurn() {
        assertEquals(1.0 + Math.PI / 2, DriveGear.sideAngle(1.0, true), 1e-9);
        assertEquals(1.0, DriveGear.sideAngle(1.0, false), 1e-9);
    }

    @Test
    void biggerDrivingWheelsTurnSlowerThanTheTruckWheels() {
        // same distance rolled: angle scales with 1 / radius
        assertEquals(65, DriveGear.driverAngle(95, 6.5, 9.5), 1e-9);
    }
}
