package dev.railbound.cargo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScrollWindowTest {
    /** The box van's hold: 160 stacks seen six rows of nine at a time, like a double chest. */
    private static final ScrollWindow HOLD = new ScrollWindow(160, 9, 6);

    @Test
    void oneHundredAndSixtySlotsMakeEighteenRowsTheLastPartFull() {
        assertEquals(18, HOLD.rows());
        assertEquals(12, HOLD.maxRow());
    }

    @Test
    void scrollingStaysWithinTheRows() {
        assertEquals(0, HOLD.clampRow(-3));
        assertEquals(12, HOLD.clampRow(99));
        assertEquals(5, HOLD.clampRow(5));
    }

    @Test
    void eachShownSlotIsTheHoldSlotThatManyRowsDown() {
        assertEquals(0, HOLD.slotAt(0, 0));
        assertEquals(53, HOLD.slotAt(0, 53));
        assertEquals(9 * 12 + 50, HOLD.slotAt(12, 50));
    }

    @Test
    void shownSlotsPastTheEndOfTheHoldAreEmptyPlaces() {
        // the last row has 160 - 17 * 9 = 7 slots
        assertEquals(159, HOLD.slotAt(12, 51));
        assertEquals(-1, HOLD.slotAt(12, 52));
        assertEquals(-1, HOLD.slotAt(12, 53));
    }

    @Test
    void aHoldThatFitsNeedsNoScrolling() {
        ScrollWindow small = new ScrollWindow(27, 9, 6);
        assertEquals(0, small.maxRow());
        assertEquals(-1, small.slotAt(0, 27));
    }
}
