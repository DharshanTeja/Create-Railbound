package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import dev.railbound.trainset.design.FrameShape;
import dev.railbound.trainset.design.HiddenPart;
import dev.railbound.trainset.design.PartType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InteriorPartsTest {

    private static boolean contains(VoxelShape shape, double x, double y, double z) {
        return shape.toAabbs().stream().anyMatch(box -> box.contains(x, y, z));
    }

    @Test
    void aSeatFacesTheControlsBehindIt() {
        // a cab's second driving position: sitting facing the bunker, its controls behind it in the carriage
        assertEquals(Direction.SOUTH, InteriorParts.seatFacing(Direction.NORTH, false, true));
        assertEquals(Direction.NORTH, InteriorParts.seatFacing(Direction.NORTH, true, false), "controls ahead");
        assertEquals(Direction.NORTH, InteriorParts.seatFacing(Direction.NORTH, false, false), "a passenger seat");
        assertEquals(Direction.NORTH, InteriorParts.seatFacing(Direction.NORTH, true, true), "controls both ways");
    }

    @Test
    void aSeatFacingBackStillHasItsWallOnTheCarriageSide() {
        // the right-hand seat facing back has the carriage side on its own left
        Direction facing = InteriorParts.seatFacing(Direction.NORTH, false, true);
        PlaceholderSide side = InteriorParts.seatSide(PlaceholderSide.RIGHT, facing != Direction.NORTH);
        assertEquals(PlaceholderSide.LEFT, side);
        assertEquals(Optional.of(InteriorParts.outward(Direction.NORTH, 1)), InteriorParts.seatOutward(facing, side));
        assertTrue(contains(InteriorParts.seatShape(facing, side), 0.95, 0.5, 0.5), "the wall is on the east (outer) edge");
        assertEquals(PlaceholderSide.RIGHT, InteriorParts.seatSide(PlaceholderSide.RIGHT, false));
    }

    @Test
    void aCabControlStandIsSlimAgainstTheOuterWallInFrontOfItsSeat() {
        // the tank engine's left stand: carriage facing north, its driver's seat behind it (south), so the stand faces
        // south and the carriage's left (west) is on its own right
        PlaceholderSide side = InteriorParts.seatSide(PlaceholderSide.LEFT, true);
        VoxelShape stand = InteriorParts.cabControlsShape(Direction.SOUTH, side);
        assertTrue(contains(stand, 0.05, 0.5, 0.5), "the carriage's outer wall");
        assertTrue(contains(stand, 0.4, 0.5, 0.8), "the stand, beside the wall, right in front of the seat");
        assertFalse(contains(stand, 0.9, 0.5, 0.8), "clear on the aisle side");
        assertFalse(contains(stand, 0.4, 0.5, 0.2), "nothing on the far side, which may be the cab's back wall");
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
    void sideDoorsFaceSidewaysAndEndDoorsFaceOutOfTheirEnd() {
        assertEquals(Direction.EAST, InteriorParts.doorOutward(Direction.NORTH, new BlockPos(1, 1, 2), 16));
        assertEquals(Direction.NORTH, InteriorParts.doorOutward(Direction.NORTH, new BlockPos(0, 1, 0), 16),
                "local z 0 is the carriage front");
        assertEquals(Direction.SOUTH, InteriorParts.doorOutward(Direction.NORTH, new BlockPos(0, 1, 15), 16));
        assertEquals(Direction.WEST, InteriorParts.doorOutward(Direction.EAST, new BlockPos(0, 2, 15), 16));
    }

    @Test
    void aCentreDoorAwayFromTheEndsHasNoOutside() {
        assertThrows(IllegalArgumentException.class,
                () -> InteriorParts.doorOutward(Direction.NORTH, new BlockPos(0, 1, 5), 16));
    }

    @Test
    void controlsFaceTheirConductorSeat() {
        // Create counts a seat as the conductor's when the controls next to it face back towards it
        assertEquals(Direction.NORTH, InteriorParts.controlsFacing(Direction.NORTH, true), "seat ahead (towards the front)");
        assertEquals(Direction.SOUTH, InteriorParts.controlsFacing(Direction.NORTH, false), "seat behind");
        assertEquals(Direction.WEST, InteriorParts.controlsFacing(Direction.EAST, false));
    }

    @Test
    void closedDoorIsAPanelOnTheOutwardFace() {
        List<AABB> boxes = InteriorParts.doorShape(Direction.EAST, false, DoubleBlockHalf.LOWER).toAabbs();
        assertEquals(1, boxes.size());
        assertEquals(14 / 16d, boxes.get(0).minX, 1e-9);
        assertEquals(1, boxes.get(0).maxX, 1e-9);
        assertEquals(1, boxes.get(0).maxY, 1e-9);
    }

    @Test
    void openDoorHasNoCollision() {
        assertTrue(InteriorParts.doorShape(Direction.EAST, true, DoubleBlockHalf.LOWER).isEmpty());
    }

    @Test
    void theUpperHalfOfADoorKeepsTheCeilingSoYouCannotJumpIntoTheRoof() {
        assertTrue(contains(InteriorParts.doorShape(Direction.EAST, true, DoubleBlockHalf.UPPER), 0.5, 0.8, 0.5));
        assertTrue(contains(InteriorParts.doorShape(Direction.EAST, false, DoubleBlockHalf.UPPER), 0.5, 0.8, 0.5));
        assertFalse(contains(InteriorParts.doorShape(Direction.EAST, true, DoubleBlockHalf.UPPER), 0.5, 0.4, 0.5),
                "open below the ceiling");
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
        VoxelShape step = InteriorParts.stepShape(Direction.EAST, false);
        assertTrue(contains(step, 0.95, 0.2, 0.5), "ladder on the outer face, below the floor, to climb from outside");
        assertFalse(contains(step, 0.05, 0.2, 0.5), "nothing on the inner side");
        assertFalse(contains(step, 0.95, 0.7, 0.5), "open above the floor: the doorway");
    }

    @Test
    void aCabStepClimbsAllTheWayToTheCabFloor() {
        // a loco cab floor is a full block up: its step's ladder runs nearly the whole block and its tread is level with it
        VoxelShape step = InteriorParts.stepShape(Direction.EAST, true);
        assertTrue(contains(step, 0.95, 0.8, 0.5), "ladder high up the outer face");
        assertTrue(contains(step, 0.5, 0.97, 0.5), "tread at the top, level with the cab floor");
        assertFalse(contains(step, 0.5, 0.5, 0.5), "open below the tread inside");
    }

    @Test
    void aStepIsHighBesideAFullHeightFloor() {
        assertTrue(InteriorParts.stepIsHigh(Optional.of(new HiddenPart(PartType.FRAME, Optional.of(FrameShape.FULL)))));
        assertFalse(InteriorParts.stepIsHigh(Optional.of(new HiddenPart(PartType.FRAME, Optional.of(FrameShape.FLOOR)))));
        assertFalse(InteriorParts.stepIsHigh(Optional.empty()));
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
        assertTrue(contains(InteriorParts.stepShape(Direction.EAST, false), 0.5, 0.47, 0.5), "floor slab level with the coach floor");
    }
}
