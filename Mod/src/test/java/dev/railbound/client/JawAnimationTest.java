package dev.railbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JawAnimationTest {
    private static final long SECOND = 1_000_000_000L;

    @Test
    void anOpenJawIsTurnedOutAndAShutOneIsStraight() {
        assertEquals(JawAnimation.OPEN_DEGREES, JawAnimation.degrees(0), 1e-6);
        assertEquals(0, JawAnimation.degrees(1), 1e-6);
        assertEquals(JawAnimation.OPEN_DEGREES / 2, JawAnimation.degrees(0.5f), 1e-4, "eases through the middle");
    }

    @Test
    void theJawEasesTowardsItsTargetAndNeverPastIt() {
        float closure = 0;
        float last = 0;
        for (int i = 0; i < 30; i++) {
            closure = JawAnimation.step(closure, true, 0.05f);
            assertTrue(closure >= last && closure <= 1, "closure " + closure);
            last = closure;
        }
        assertEquals(1, closure, 0.2, "most of the way after 1.5 s");
        assertEquals(1, JawAnimation.step(0.9995f, true, 0.05f), "it settles instead of creeping for ever");
    }

    @Test
    void aJawOpensAgainWhenTheCouplingIsReleased() {
        float closure = 1;
        for (int i = 0; i < 100; i++) {
            closure = JawAnimation.step(closure, false, 0.05f);
        }
        assertEquals(0, closure, 1e-6);
    }

    @Test
    void aCouplingSeenForTheFirstTimeStartsOpenAndClosesOverAFewTenthsOfASecond() {
        Object key = new Object();
        assertEquals(0, JawAnimation.closure(key, true, 10 * SECOND), 1e-6, "starts open");
        float closure = 0;
        long now = 10 * SECOND;
        for (int frame = 1; frame <= 10; frame++) {            // ten frames of 50 ms: half a second
            now += SECOND / 20;
            closure = JawAnimation.closure(key, true, now);
        }
        assertTrue(closure > 0.5f && closure < 1f, "half a second later: " + closure);
        for (int frame = 1; frame <= 40; frame++) {
            now += SECOND / 20;
            closure = JawAnimation.closure(key, true, now);
        }
        assertEquals(1, closure, 1e-3);
    }

    @Test
    void aLongPauseDoesNotSnapStraightToShut() {
        Object key = new Object();
        JawAnimation.closure(key, true, 0);
        // a frame after a freeze (a long dt) is limited, so the animation is still seen
        float after = JawAnimation.closure(key, true, 5 * SECOND);
        assertTrue(after < 1f, "a clamped step, not a jump: " + after);
    }

    @Test
    void eachCouplingKeepsItsOwnJaw() {
        Object a = new Object(), b = new Object();
        JawAnimation.closure(a, true, 0);
        JawAnimation.closure(a, true, SECOND);
        assertEquals(0, JawAnimation.closure(b, true, SECOND), 1e-6);
    }
}
