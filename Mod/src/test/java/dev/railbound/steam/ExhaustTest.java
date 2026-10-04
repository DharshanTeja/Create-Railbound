package dev.railbound.steam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExhaustTest {

    @Test
    void modelPixelsLandInDesignBlocksAsTheConverterPutsThem() {
        // the converter's ModelSpace: model origin is the floor top at the carriage centre
        assertArrayEquals(new double[] {0.5, 1, 8}, Exhaust.toDesign(0, 0, 0, 16), 1e-9);
        assertArrayEquals(new double[] {0.5, 4.125, 0.9375}, Exhaust.toDesign(0, 50, -113, 16), 1e-9, "the chimney top");
    }

    @Test
    void eachChuffPuffsSmokeAndMoreWhenWorkingHard() {
        Exhaust cruising = Exhaust.of(1, true, 0.5, false, 3, Exhaust.LONG_STOPPED, 1.0);
        Exhaust pulling = Exhaust.of(1, true, 0.5, true, 3, Exhaust.LONG_STOPPED, 1.0);
        assertTrue(cruising.chuff());
        assertEquals(Exhaust.PUFFS, cruising.smokePuffs());
        assertTrue(pulling.smokePuffs() > cruising.smokePuffs());
        assertEquals(2 * cruising.smokePuffs(), Exhaust.of(2, true, 0.5, false, 3, Exhaust.LONG_STOPPED, 1.0).smokePuffs());
    }

    @Test
    void aDeadFireMakesNoSmokeAndNoChuffs() {
        Exhaust coasting = Exhaust.of(2, false, 0.5, false, 0, Exhaust.LONG_STOPPED, 1.0);
        assertEquals(0, coasting.smokePuffs());
        assertFalse(coasting.chuff());
        assertFalse(coasting.wisp());
        assertFalse(coasting.cylinderSteam());
    }

    @Test
    void aStandingEngineWithItsFireLitSendsUpAWispNowAndThen() {
        int wisps = 0;
        for (long tick = 0; tick < 80; tick++) {
            Exhaust standing = Exhaust.of(0, true, 0, false, tick, Exhaust.LONG_STOPPED, 1.0);
            assertEquals(0, standing.smokePuffs());
            wisps += standing.wisp() ? 1 : 0;
        }
        assertTrue(wisps >= 5 && wisps <= 20, wisps + " wisps in 4 seconds");
        assertFalse(Exhaust.of(0, true, 0.5, false, 0, Exhaust.LONG_STOPPED, 1.0).wisp(), "no idle wisps on the move");
    }

    @Test
    void stoppingReleasesAPuffOfSteamForAMoment() {
        assertTrue(Exhaust.of(0, true, 0, false, 0, 5, 0.9).blowdown(), "just stopped");
        assertFalse(Exhaust.of(0, true, 0, false, 0, Exhaust.BLOWDOWN_TICKS + 1, 0.9).blowdown(), "a while later");
        assertFalse(Exhaust.of(0, true, 0.5, false, 0, -1, 0.9).blowdown(), "still moving");
        assertFalse(Exhaust.of(0, false, 0, false, 0, 5, 0).blowdown(), "no steam left to release");
    }

    @Test
    void aFullBoilerStandingBlowsOffAtTheSafetyValve() {
        int lifts = 0;
        for (long tick = 0; tick < 40; tick++) {
            lifts += Exhaust.of(0, true, 0, false, tick, Exhaust.LONG_STOPPED, 1.0).safetyValve() ? 1 : 0;
            assertFalse(Exhaust.of(0, true, 0, false, tick, Exhaust.LONG_STOPPED, 0.8).safetyValve(), "below full pressure");
            assertFalse(Exhaust.of(0, true, 0.5, false, tick, -1, 1.0).safetyValve(), "working, the steam goes to the cylinders");
        }
        assertTrue(lifts > 5, lifts + " lifts");
    }

    @Test
    void theCylindersBlowSteamWhenStartingOff() {
        assertTrue(Exhaust.of(0, true, 0.05, true, 0, Exhaust.LONG_STOPPED, 1.0).cylinderSteam());
        assertFalse(Exhaust.of(0, true, 0.5, true, 0, Exhaust.LONG_STOPPED, 1.0).cylinderSteam(), "not once under way");
        assertFalse(Exhaust.of(0, true, 0.05, false, 0, Exhaust.LONG_STOPPED, 1.0).cylinderSteam(), "not when drifting to a stop");
    }
}
