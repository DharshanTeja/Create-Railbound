package dev.railbound.steam;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoWhistleTest {
    private static final UUID STATION = UUID.randomUUID();
    private static final UUID OTHER_STATION = UUID.randomUUID();

    private static AutoWhistle.Seen stoppedAtStation() {
        return new AutoWhistle.Seen(true, false, true, false, null, 0);
    }

    private static AutoWhistle.Seen running(UUID destination, double distance) {
        return new AutoWhistle.Seen(true, true, false, false, destination, distance);
    }

    private static AutoWhistle.Seen heldAtSignal() {
        return new AutoWhistle.Seen(true, false, false, true, STATION, 300);
    }

    /** The same for so many ticks; how many whistles it gave. */
    private static int hold(AutoWhistle whistle, AutoWhistle.Seen seen, int ticks) {
        int whistles = 0;
        for (int i = 0; i < ticks; i++) {
            if (whistle.tick(seen) != AutoWhistle.Pattern.NONE) {
                whistles++;
            }
        }
        return whistles;
    }

    @Test
    void leavingAStationGivesTwoShortWhistles() {
        AutoWhistle whistle = new AutoWhistle();
        hold(whistle, stoppedAtStation(), AutoWhistle.SETTLE);
        assertEquals(AutoWhistle.Pattern.TWO_SHORT, whistle.tick(running(STATION, 400)));
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(running(STATION, 390)), "only once");
    }

    @Test
    void nearingTheStationGivesOneLongWhistleOnce() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(running(STATION, 150)));
        assertEquals(AutoWhistle.Pattern.LONG, whistle.tick(running(STATION, 99)));
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(running(STATION, 60)));
    }

    @Test
    void aShortTripThatStartsInsideTheWhistleDistanceDoesNotWhistleForTheApproach() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(stoppedAtStation());
        whistle.tick(running(STATION, 50));
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(running(STATION, 40)));
    }

    @Test
    void aNewDestinationGetsItsOwnApproachWhistle() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        whistle.tick(running(STATION, 90));
        whistle.tick(running(OTHER_STATION, 500));
        assertEquals(AutoWhistle.Pattern.LONG, whistle.tick(running(OTHER_STATION, 80)));
    }

    @Test
    void heldAtARedSignalGivesAShortWhistleAndAnotherWhenItMovesOff() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        assertEquals(0, hold(whistle, heldAtSignal(), AutoWhistle.SETTLE - 1), "not until it has really stopped");
        assertEquals(AutoWhistle.Pattern.SHORT, whistle.tick(heldAtSignal()));
        assertEquals(0, hold(whistle, heldAtSignal(), AutoWhistle.COOLDOWN), "only once while it waits");
        assertEquals(AutoWhistle.Pattern.SHORT, whistle.tick(running(STATION, 299)));
    }

    @Test
    void aTrainCreepingUpToASignalInFitsAndStartsDoesNotKeepWhistling() {
        // Create's trains edge forward and stop again while a signal clears: none of that is a real stop
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        int whistles = 0;
        for (int i = 0; i < 40; i++) {
            whistles += hold(whistle, heldAtSignal(), 3) + hold(whistle, running(STATION, 300), 2);
        }
        assertEquals(0, whistles);
    }

    @Test
    void aSignalFlickeringWhileTheTrainWaitsWhistlesOnce() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        AutoWhistle.Seen waiting = new AutoWhistle.Seen(true, false, false, false, STATION, 300);
        int whistles = 0;
        for (int i = 0; i < 100; i++) {
            whistles += hold(whistle, heldAtSignal(), 1) + hold(whistle, waiting, 1);
        }
        assertEquals(1, whistles);
    }

    @Test
    void movingOffRightAfterAWhistleStaysQuiet() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(running(STATION, 400));
        hold(whistle, heldAtSignal(), AutoWhistle.SETTLE);   // the held-at-signal whistle
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(running(STATION, 299)), "too soon after the last");
    }

    @Test
    void aPlayerAtTheControlsWhistlesForThemselves() {
        AutoWhistle whistle = new AutoWhistle();
        whistle.tick(new AutoWhistle.Seen(false, false, true, false, null, 0));
        assertEquals(AutoWhistle.Pattern.NONE, whistle.tick(new AutoWhistle.Seen(false, true, false, false, STATION, 50)));
    }

    @Test
    void theFirstLookNeverWhistles() {
        assertEquals(AutoWhistle.Pattern.NONE, new AutoWhistle().tick(running(STATION, 50)));
    }

    @Test
    void eachPatternIsAHornPressAndRelease() {
        // Create's horn: a press sounds it for a second, held presses keep it going, a release ends it shortly
        assertEquals(java.util.List.of(new AutoWhistle.Press(0, true), new AutoWhistle.Press(6, false)),
                AutoWhistle.Pattern.SHORT.presses());
        assertEquals(4, AutoWhistle.Pattern.TWO_SHORT.presses().size());
        assertEquals(false, AutoWhistle.Pattern.LONG.presses().get(AutoWhistle.Pattern.LONG.presses().size() - 1).honk());
    }
}
