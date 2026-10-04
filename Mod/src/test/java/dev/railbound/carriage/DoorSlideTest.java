package dev.railbound.carriage;

import dev.railbound.trainset.design.DoorSlideDirection;
import dev.railbound.trainset.design.DoorSpec;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
    void sideDoorsSlideAlongTheCarriageUnlessTheDesignSaysOtherwise() {
        assertEquals(DoorSlideDirection.FRONT, DoorSlide.of(new DoorSpec("d", new BlockPos(-1, 1, 2)), 16));
        assertEquals(DoorSlideDirection.REAR, DoorSlide.of(new DoorSpec("d", new BlockPos(1, 1, 13)), 16));
    }

    @Test
    void aDoorCanSayWhichWayItSlides() {
        DoorSpec endDoor = new DoorSpec("d", new BlockPos(0, 1, 0), Optional.of(DoorSlideDirection.LEFT));
        assertEquals(DoorSlideDirection.LEFT, DoorSlide.of(endDoor, 16));
    }

    @Test
    void slideDirectionsMoveAlongTheDesignAxes() {
        // design x runs left (-) to right (+), design z front (-) to rear (+)
        assertArrayEquals(new int[] {-1, 0}, new int[] {DoorSlideDirection.LEFT.dx(), DoorSlideDirection.LEFT.dz()});
        assertArrayEquals(new int[] {1, 0}, new int[] {DoorSlideDirection.RIGHT.dx(), DoorSlideDirection.RIGHT.dz()});
        assertArrayEquals(new int[] {0, -1}, new int[] {DoorSlideDirection.FRONT.dx(), DoorSlideDirection.FRONT.dz()});
        assertArrayEquals(new int[] {0, 1}, new int[] {DoorSlideDirection.REAR.dx(), DoorSlideDirection.REAR.dz()});
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
