package dev.railbound.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * How far a knuckle's jaw is open. A free coupler rests with its jaw turned out, ready to take the other's hand; a
 * joined one has it shut. Each coupling's jaw eases towards where it should be, so a join is seen as the jaw swinging
 * shut and a split as it swinging open, instead of the model changing between two frames. A coupling seen for the
 * first time starts open, so every new join shows the jaw closing.
 */
public final class JawAnimation {
    /** How far the jaw is turned out of line when open (degrees, about the pin). */
    public static final float OPEN_DEGREES = 28f;
    /** The share of the remaining way covered per second, as an exponent: about a fifth of a second to cover most of it. */
    static final float RATE = 5f;
    /** Closer than this to its target, the jaw has arrived. */
    private static final float SETTLE = 0.002f;
    /** A frame after a stall counts as at most this long, so the swing is still seen. */
    private static final float MAX_STEP_SECONDS = 0.25f;
    private static final long FORGET_NANOS = 5_000_000_000L;
    private static final int PRUNE_EVERY = 512;

    private static final Map<Object, State> STATES = new HashMap<>();
    private static int calls;

    private static final class State {
        float closure;
        long at;
    }

    private JawAnimation() {}

    /** The jaw's closure, 0 open to 1 shut, after {@code seconds} of heading for shut or open. */
    static float step(float closure, boolean closed, float seconds) {
        float target = closed ? 1 : 0;
        float next = closure + (target - closure) * (1 - (float) Math.exp(-RATE * seconds));
        return Math.abs(target - next) < SETTLE ? target : next;
    }

    /** The jaw's turn (degrees) at a closure: eased so it starts and stops gently. */
    public static float degrees(float closure) {
        float c = Math.max(0, Math.min(1, closure));
        float eased = c * c * (3 - 2 * c);
        return OPEN_DEGREES * (1 - eased);
    }

    /** The closure of one coupling's jaw now, moving it on from when it was last asked. */
    public static float closure(Object key, boolean closed, long nowNanos) {
        State state = STATES.get(key);
        if (state == null) {
            state = new State();
            state.at = nowNanos;
            STATES.put(key, state);
            prune(nowNanos);
            return 0;
        }
        float seconds = Math.max(0, Math.min(MAX_STEP_SECONDS, (nowNanos - state.at) / 1e9f));
        state.closure = step(state.closure, closed, seconds);
        state.at = nowNanos;
        if (++calls % PRUNE_EVERY == 0) {
            prune(nowNanos);
        }
        return state.closure;
    }

    /** Forgets couplings nobody has drawn for a while (a train that left or was picked up). */
    private static void prune(long nowNanos) {
        Iterator<State> it = STATES.values().iterator();
        while (it.hasNext()) {
            if (nowNanos - it.next().at > FORGET_NANOS) {
                it.remove();
            }
        }
    }
}
