package dev.railbound.steam;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainPowerTest {
    private static final TrainPower.Loco TANK_ENGINE = new TrainPower.Loco(20, 10, 2, 1);

    private static TrainPower.Loco withPull(TrainPower.Loco loco, double pull) {
        return new TrainPower.Loco(loco.topSpeed(), loco.curveSpeed(), loco.acceleration(), pull);
    }

    @Test
    void aTrainWithoutOurLocosKeepsCreatesOwnSpeeds() {
        assertEquals(Optional.empty(), TrainPower.limits(List.of()));
    }

    @Test
    void aLocoAtFullPressureRunsAtItsDesignSpeeds() {
        TrainPower.Limits limits = TrainPower.limits(List.of(TANK_ENGINE)).orElseThrow();
        assertEquals(20, limits.topSpeed(), 1e-9);
        assertEquals(10, limits.curveSpeed(), 1e-9);
        assertEquals(2, limits.acceleration(), 1e-9);
    }

    @Test
    void lowPressureMeansSlowerAndWeaker() {
        TrainPower.Limits limits = TrainPower.limits(List.of(withPull(TANK_ENGINE, 0.5))).orElseThrow();
        assertEquals(10, limits.topSpeed(), 1e-9);
        assertEquals(1.5, limits.acceleration(), 1e-9);
    }

    @Test
    void outOfSteamTheTrainCannotBeDriven() {
        TrainPower.Limits limits = TrainPower.limits(List.of(withPull(TANK_ENGINE, 0))).orElseThrow();
        assertEquals(0, limits.topSpeed());
        assertEquals(0, limits.curveSpeed());
        // Create brakes with the same acceleration figure: a train out of steam must still be able to stop
        assertEquals(1, limits.acceleration(), 1e-9);
    }

    @Test
    void theSlowestLocoSetsTheSpeedButAnyLocoWithSteamCanPull() {
        TrainPower.Loco fast = new TrainPower.Loco(30, 15, 3, 0);   // fire out
        TrainPower.Limits limits = TrainPower.limits(List.of(fast, TANK_ENGINE)).orElseThrow();
        assertEquals(20, limits.topSpeed(), 1e-9, "the slower design limits the train");
        assertTrue(limits.acceleration() > 0, "the tank engine still has steam");
    }

    @Test
    void effortRisesWithSpeedAndMoreWhenAccelerating() {
        double cruising = TrainPower.effort(10, 10, 20);
        double accelerating = TrainPower.effort(10, 20, 20);
        assertEquals(0, TrainPower.effort(0, 0, 20), 1e-9, "standing still is idling");
        assertTrue(cruising > 0);
        assertTrue(accelerating > cruising);
        assertEquals(1, TrainPower.effort(20, 30, 20), 1e-9);
        assertEquals(cruising, TrainPower.effort(-10, -10, 20), 1e-9, "reversing counts the same");
    }

    @Test
    void fourChuffsForEveryTurnOfTheDrivingWheels() {
        assertEquals(0, TrainPower.chuffs(10, 80));
        assertEquals(1, TrainPower.chuffs(80, 100));
        assertEquals(4, TrainPower.chuffs(0, 360));
        assertEquals(3, TrainPower.chuffs(100, -100), "running backwards chuffs too: past 90, 0 and -90");
    }
}
