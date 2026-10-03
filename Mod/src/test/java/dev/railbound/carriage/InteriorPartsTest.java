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
        assertTrue(contains(left, 0.05, 0.9, 0.5));
        assertTrue(contains(right, 0.95, 0.9, 0.5));
        assertFalse(contains(middle, 0.05, 0.9, 0.5));
    }

    @Test
    void seatHasACushionToSitOn() {
        VoxelShape seat = InteriorParts.seatShape(Direction.EAST, PlaceholderSide.NONE);
        assertTrue(contains(seat, 0.5, 0.2, 0.5));
        assertFalse(contains(seat, 0.5, 0.9, 0.5), "the space above the cushion stays free");
    }
}
