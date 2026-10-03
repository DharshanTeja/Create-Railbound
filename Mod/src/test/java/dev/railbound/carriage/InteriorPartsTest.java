package dev.railbound.carriage;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InteriorPartsTest {

    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        return shape.toAabbs().stream().anyMatch(box -> box.contains(x, y, z));
    }

    @Test
    void doorHalfComesFromTheCellBelow() {
        assertEquals(DoubleBlockHalf.LOWER, InteriorParts.doorHalf(false));
        assertEquals(DoubleBlockHalf.UPPER, InteriorParts.doorHalf(true));
    }

    @Test
    void outwardPointsAwayFromTheAisle() {
        assertEquals(Direction.EAST, InteriorParts.outward(Direction.NORTH, 1));
        assertEquals(Direction.WEST, InteriorParts.outward(Direction.NORTH, -1));
        assertEquals(Direction.SOUTH, InteriorParts.outward(Direction.EAST, 1));
        assertEquals(Direction.NORTH, InteriorParts.outward(Direction.EAST, -1));
    }

    @Test
    void centreColumnHasNoOutside() {
        assertThrows(IllegalArgumentException.class, () -> InteriorParts.outward(Direction.NORTH, 0));
    }

    @Test
    void closedDoorIsAPanelOnTheOutwardFace() {
        List<AABB> boxes = InteriorParts.doorShape(Direction.EAST, false).toAabbs();
        assertEquals(1, boxes.size());
        assertEquals(14 / 16d, boxes.get(0).minX, 1e-9);
        assertEquals(1, boxes.get(0).maxX, 1e-9);
        assertEquals(1, boxes.get(0).maxY, 1e-9);
    }

    @Test
    void openDoorHasNoCollision() {
        assertTrue(InteriorParts.doorShape(Direction.EAST, true).isEmpty());
    }

    @Test
    void seatCarriesTheOuterWallOnItsSide() {
        VoxelShape left = InteriorParts.seatShape(Direction.NORTH, PlaceholderSide.LEFT);
        VoxelShape right = InteriorParts.seatShape(Direction.NORTH, PlaceholderSide.RIGHT);
        VoxelShape middle = InteriorParts.seatShape(Direction.NORTH, PlaceholderSide.NONE);
        // below the floor only the outer wall is there
        assertTrue(contains(left, 0.05, 0.3, 0.5));
        assertTrue(contains(right, 0.95, 0.3, 0.5));
        assertFalse(contains(middle, 0.05, 0.3, 0.5));
    }

    @Test
    void seatHasACushionToSitOn() {
        // seats sit on the floor layer, whose floor is half a block up: the cushion is on top of it
        VoxelShape seat = InteriorParts.seatShape(Direction.EAST, PlaceholderSide.NONE);
        assertTrue(contains(seat, 0.5, 0.7, 0.5));
        assertFalse(contains(seat, 0.5, 0.97, 0.5), "the space above the cushion stays free");
        assertFalse(contains(seat, 0.5, 0.3, 0.5), "nothing under the floor");
    }

    @Test
    void stepIsALadderOnTheOuterFaceBelowTheFloor() {
        // a step on the right of a north-facing carriage looks out east: the ladder is on its east (outer) face
        VoxelShape step = InteriorParts.stepShape(Direction.EAST);
        assertTrue(contains(step, 0.95, 0.2, 0.5), "ladder on the outer face, below the floor, to climb from outside");
        assertFalse(contains(step, 0.05, 0.2, 0.5), "nothing on the inner side");
        assertFalse(contains(step, 0.95, 0.7, 0.5), "open above the floor: the doorway");
    }

    @Test
    void aSideSeatsOutsideIsTheCarriageSide() {
        assertEquals(java.util.Optional.of(Direction.EAST), InteriorParts.seatOutward(Direction.NORTH, PlaceholderSide.RIGHT));
        assertEquals(java.util.Optional.of(Direction.WEST), InteriorParts.seatOutward(Direction.NORTH, PlaceholderSide.LEFT));
        assertEquals(java.util.Optional.of(Direction.NORTH), InteriorParts.seatOutward(Direction.WEST, PlaceholderSide.RIGHT));
        assertEquals(java.util.Optional.empty(), InteriorParts.seatOutward(Direction.NORTH, PlaceholderSide.NONE));
    }

    @Test
    void theStepAlwaysHasFloorSoNobodyFallsThrough() {
        assertTrue(contains(InteriorParts.stepShape(Direction.EAST), 0.5, 0.47, 0.5), "floor slab level with the coach floor");
    }
}
