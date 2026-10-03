package dev.railbound.carriage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoorSlideTest {

    @Test
    void frontHalfDoorsSlideTowardsTheFront() {
        assertEquals(-1, DoorSlide.direction(2, 16));
        assertEquals(-1, DoorSlide.direction(7, 16));
    }

    @Test
    void rearHalfDoorsSlideTowardsTheRear() {
        assertEquals(1, DoorSlide.direction(13, 16));
        assertEquals(1, DoorSlide.direction(8, 16));
    }

    @Test
    void aClosedDoorDoesNotMoveAndAnOpenOneClearsTheDoorway() {
        assertEquals(0, DoorSlide.distance(0), 1e-6);
        assertEquals(DoorSlide.OPEN_DISTANCE, DoorSlide.distance(1), 1e-6);
    }

    @Test
    void theDoorEasesInAndOut() {
        assertTrue(DoorSlide.distance(0.1f) < 0.1f * DoorSlide.OPEN_DISTANCE, "starts slowly");
        assertTrue(DoorSlide.distance(0.9f) > 0.9f * DoorSlide.OPEN_DISTANCE, "ends slowly");
        assertEquals(DoorSlide.OPEN_DISTANCE / 2, DoorSlide.distance(0.5f), 1e-6);
    }
}
