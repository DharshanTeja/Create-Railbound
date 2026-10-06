package dev.railbound.steam;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlumeTest {
    @Test
    void aDeadFireMakesNoSmoke() {
        assertEquals(Optional.empty(), Plume.of(false, true, 1, 0.5, false));
    }

    @Test
    void aLitFireSmokesEveryTickEvenStanding() {
        assertTrue(Plume.of(true, false, 0, 0, false).isPresent());
    }

    @Test
    void standingIdleTheSmokeIsThinAndShortLived() {
        Plume.Puff idle = Plume.of(true, false, 0, 0, false).orElseThrow();
        assertEquals(40, idle.life());           // 200 ticks x (1 + no effort) x the standing share 0.2
        assertEquals(0.25f, idle.alpha(), 1e-6);
    }

    @Test
    void workingHardAtSpeedTheSmokeIsThickAndLingers() {
        Plume.Puff hard = Plume.of(true, false, 1, 0.5, false).orElseThrow();
        assertEquals(400, hard.life());
        assertEquals(0.75f, hard.alpha(), 1e-6);
    }

    @Test
    void eachChuffBlowsABiggerFasterPuff() {
        Plume.Puff between = Plume.of(true, false, 0.5, 0.3, false).orElseThrow();
        Plume.Puff beat = Plume.of(true, true, 0.5, 0.3, false).orElseThrow();
        assertEquals(between.startSize() * 1.75f, beat.startSize(), 1e-6);
        assertEquals(between.rise() * 1.75, beat.rise(), 1e-9);
    }

    @Test
    void thePuffSpreadsToSeventeenTimesItsSize() {
        Plume.Puff puff = Plume.of(true, false, 0.5, 0.3, false).orElseThrow();
        assertEquals(puff.startSize() * 17, puff.endSize(), 1e-5);
    }

    @Test
    void justAfterFiringTheSmokeIsDarker() {
        assertTrue(Plume.of(true, false, 0.5, 0.3, true).orElseThrow().shade()
                < Plume.of(true, false, 0.5, 0.3, false).orElseThrow().shade());
    }
}
