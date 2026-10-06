package dev.railbound.steam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoilerTest {
    private static final int TICKS_PER_DAY = 24000;
    private static final int TANK = 16000;

    /** A bunker holding a number of coal items. */
    private static final class Bunker implements Boiler.FuelSource {
        int coal;
        int taken;

        Bunker(int coal) {
            this.coal = coal;
        }

        @Override
        public double take() {
            if (coal == 0) {
                return 0;
            }
            coal--;
            taken++;
            return 1;
        }
    }

    private static Boiler full() {
        Boiler boiler = new Boiler(TANK);
        boiler.addWater(TANK);
        return boiler;
    }

    private static int ticksUntilItPulls(Boiler boiler, Bunker bunker) {
        for (int t = 1; t <= 20 * 120; t++) {
            boiler.tick(0, bunker);
            if (boiler.pull() > 0) {
                return t;
            }
        }
        return -1;
    }

    @Test
    void aColdBoilerCannotPull() {
        assertEquals(0, full().pull());
        assertFalse(full().fireLit());
    }

    @Test
    void theFireLightsWithCoalAndWater() {
        Boiler boiler = full();
        boiler.tick(0, new Bunker(10));
        assertTrue(boiler.fireLit());
    }

    @Test
    void noCoalMeansNoFireAndNoPressure() {
        Boiler boiler = full();
        Bunker empty = new Bunker(0);
        for (int t = 0; t < 2000; t++) {
            boiler.tick(0, empty);
        }
        assertFalse(boiler.fireLit());
        assertEquals(0, boiler.pressure());
    }

    @Test
    void fromColdItTakesHalfAMinuteToAMinuteBeforeItCanMove() {
        int ticks = ticksUntilItPulls(full(), new Bunker(64));
        assertTrue(ticks >= 30 * 20 && ticks <= 60 * 20, "took " + ticks / 20.0 + " s");
    }

    @Test
    void pressureNeverPassesFull() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(64);
        for (int t = 0; t < 10000; t++) {
            boiler.tick(0, bunker);
        }
        assertEquals(1, boiler.pressure(), 1e-9);
        assertEquals(1, boiler.pull(), 1e-9);
    }

    @Test
    void moreSteamPullsHarder() {
        Boiler warm = full();
        Bunker bunker = new Bunker(64);
        ticksUntilItPulls(warm, bunker);
        double justMoving = warm.pull();
        for (int t = 0; t < 2000; t++) {
            warm.tick(0, bunker);
        }
        assertTrue(warm.pull() > justMoving);
    }

    @Test
    void hardWorkBurnsMoreCoalAndWater() {
        Boiler idle = full(), working = full();
        Bunker idleBunker = new Bunker(500), workingBunker = new Bunker(500);
        for (int t = 0; t < 6000; t++) {
            idle.tick(0, idleBunker);
            working.tick(1, workingBunker);
        }
        assertTrue(workingBunker.taken > idleBunker.taken);
        assertTrue(working.water() < idle.water());
    }

    @Test
    void aFullTankLastsAboutOneDayAndTheCoalOutlastsIt() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(128);
        int waterGone = -1;
        for (int t = 1; t <= 2 * TICKS_PER_DAY && waterGone < 0; t++) {
            boiler.tick(0.6, bunker);
            if (boiler.plugMelted()) {
                waterGone = t;
            }
        }
        assertTrue(waterGone > 0.75 * TICKS_PER_DAY && waterGone < TICKS_PER_DAY,
                "water lasted " + waterGone / (double) TICKS_PER_DAY + " days");
        assertTrue(bunker.coal > 100, "most of the coal is left: " + bunker.coal);
    }

    @Test
    void oneCoalBurnsForEightySecondsFlatOutLikeInAFurnace() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(100);
        for (int t = 0; t < 10 * 80 * 20; t++) {
            boiler.tick(1, bunker);
        }
        assertEquals(10, bunker.taken);
    }

    @Test
    void idlingOneCoalLastsFourTimesAsLong() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(100);
        for (int t = 0; t < 2 * 4 * 80 * 20; t++) {
            boiler.tick(0, bunker);
        }
        assertEquals(2, bunker.taken);
    }

    @Test
    void lowWaterMeltsTheSafetyPlugAndDropsTheFire() {
        Boiler boiler = new Boiler(TANK);
        boiler.addWater(TANK / 10);
        Bunker bunker = new Bunker(500);
        for (int t = 0; t < TICKS_PER_DAY && !boiler.plugMelted(); t++) {
            boiler.tick(1, bunker);
        }
        assertTrue(boiler.plugMelted());
        assertFalse(boiler.fireLit());
        int coalAtMelt = bunker.taken;
        for (int t = 0; t < 2000; t++) {
            boiler.tick(0, bunker);
        }
        assertEquals(coalAtMelt, bunker.taken, "no coal is fed with the plug gone");
        assertEquals(0, boiler.pull(), "the pressure falls away");
    }

    @Test
    void refillingTheTankLetsTheFireBeRelit() {
        Boiler boiler = new Boiler(TANK);
        boiler.addWater(TANK / 10);
        Bunker bunker = new Bunker(500);
        for (int t = 0; t < TICKS_PER_DAY && !boiler.plugMelted(); t++) {
            boiler.tick(1, bunker);
        }
        boiler.addWater(TANK / 10);
        boiler.tick(0, bunker);
        assertFalse(boiler.fireLit(), "a little water is not enough to relight");
        boiler.addWater(TANK / 2);
        boiler.tick(0, bunker);
        assertTrue(boiler.fireLit());
        assertFalse(boiler.plugMelted());
    }

    @Test
    void aColdEmptyBoilerKeepsItsPlugAndLightsOnceFilled() {
        // a loco placed with an empty tank must not count as run dry: the plug only melts with the fire burning
        Boiler boiler = new Boiler(TANK);
        Bunker bunker = new Bunker(64);
        for (int t = 0; t < 100; t++) {
            boiler.tick(0, bunker);
        }
        assertFalse(boiler.plugMelted());
        assertFalse(boiler.fireLit(), "no fire without water");
        assertEquals(64, bunker.coal, "no coal wasted on an empty boiler");
        boiler.addWater(TANK);
        boiler.tick(0, bunker);
        assertTrue(boiler.fireLit());
    }

    @Test
    void aMeltedPlugReadsAsFixedOnceTheTankIsRefilled() {
        assertTrue(Boiler.plugStillMelted(true, TANK * 0.1, TANK));
        assertFalse(Boiler.plugStillMelted(true, TANK, TANK), "refilled: it relights next time it runs");
        assertFalse(Boiler.plugStillMelted(false, 0, TANK));
    }

    @Test
    void withoutCoalItCoastsToAStopWithinHalfAMinuteOfTheFireGoingOut() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(64);
        for (int t = 0; t < 4000; t++) {
            boiler.tick(0, bunker);
        }
        bunker.coal = 0;
        while (boiler.fireLit()) {
            boiler.tick(1, bunker);   // the last lump in the firebox burns out
        }
        int t = 0;
        while (boiler.pull() > 0 && t < 20 * 120) {
            boiler.tick(1, bunker);
            t++;
        }
        assertEquals(0, boiler.pull());
        assertTrue(t <= 45 * 20, "kept pulling for " + t / 20.0 + " s after the fire went out");
    }

    @Test
    void theTankTakesNoMoreThanItHolds() {
        Boiler boiler = new Boiler(TANK);
        assertEquals(TANK, boiler.addWater(TANK + 500));
        assertEquals(0, boiler.addWater(1000));
        assertEquals(TANK, boiler.water(), 1e-9);
    }

    @Test
    void stateSurvivesASaveAndLoad() {
        Boiler boiler = full();
        Bunker bunker = new Bunker(64);
        for (int t = 0; t < 900; t++) {
            boiler.tick(0.5, bunker);
        }
        Boiler copy = Boiler.restore(TANK, boiler.snapshot());
        assertEquals(boiler.snapshot(), copy.snapshot());
        assertEquals(boiler.pull(), copy.pull(), 1e-12);
    }
}
