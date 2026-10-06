package dev.railbound.steam;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * When a loco driven by a mob on its schedule sounds its whistle, as a crew would: one long whistle nearing its next
 * station, two short ones leaving a station, a short one when held at a red signal and another when it moves off after
 * any other stop. Only real stops count (standing at least {@link #SETTLE} ticks), and short whistles are kept
 * {@link #COOLDOWN} ticks apart. A player at the controls whistles for themselves. One per train, looked at every tick.
 */
public final class AutoWhistle {
    /** How far before its station (blocks along the track) a train whistles for it. */
    static final double APPROACH = 100;
    /**
     * Ticks a train must stand before it counts as stopped: Create's trains edge forward and stop again while a signal
     * clears, and none of that is a stop to whistle for.
     */
    static final int SETTLE = 20;
    /** Ticks after a whistle starts before a short whistle may follow, so whistles never run into each other. */
    static final int COOLDOWN = 100;

    /** What the train is doing this tick. */
    public record Seen(boolean automatic, boolean moving, boolean atStation, boolean heldAtSignal,
                       @Nullable UUID destination, double distanceToDestination) {}

    /** One press or release of Create's horn, so many ticks into a whistle. */
    public record Press(int tick, boolean honk) {}

    public enum Pattern {
        NONE(List.of()),
        SHORT(List.of(new Press(0, true), new Press(6, false))),
        TWO_SHORT(List.of(new Press(0, true), new Press(6, false), new Press(16, true), new Press(22, false))),
        LONG(List.of(new Press(0, true), new Press(10, true), new Press(20, true), new Press(30, true), new Press(40, false)));

        private final List<Press> presses;

        Pattern(List<Press> presses) {
            this.presses = presses;
        }

        /** Create's horn sounds a second per press, held presses keep it going, and a release ends it shortly. */
        public List<Press> presses() {
            return presses;
        }
    }

    private boolean first = true;
    private int standing;
    private boolean whistledThisStop;
    private boolean stoppedAtStation;
    private int sinceWhistle = COOLDOWN;
    /** The destination last seen further away than {@link #APPROACH}, not yet whistled for. */
    @Nullable
    private UUID farDestination;

    public Pattern tick(Seen seen) {
        if (!seen.moving() && seen.atStation()) {
            stoppedAtStation = true;
        }
        if (seen.destination() == null) {
            farDestination = null;
        } else if (seen.distanceToDestination() >= APPROACH) {
            farDestination = seen.destination();
        }
        boolean approaching = seen.destination() != null && seen.distanceToDestination() < APPROACH
                && seen.destination().equals(farDestination);
        if (approaching) {
            farDestination = null;
        }
        boolean departing = seen.moving() && standing >= SETTLE;
        standing = seen.moving() ? 0 : standing + 1;
        boolean held = !seen.moving() && seen.heldAtSignal() && standing >= SETTLE && !whistledThisStop;
        boolean firstLook = first;
        first = false;
        sinceWhistle++;

        Pattern out = Pattern.NONE;
        if (seen.automatic() && !firstLook) {
            boolean rested = sinceWhistle > COOLDOWN;
            if (approaching) {
                out = Pattern.LONG;
            } else if (departing && rested) {
                out = stoppedAtStation ? Pattern.TWO_SHORT : Pattern.SHORT;
            } else if (held && rested) {
                out = Pattern.SHORT;
            }
        }
        if (out != Pattern.NONE) {
            sinceWhistle = 0;
            whistledThisStop = !seen.moving();
        }
        if (departing) {
            stoppedAtStation = false;
            whistledThisStop = false;
        }
        return out;
    }
}
