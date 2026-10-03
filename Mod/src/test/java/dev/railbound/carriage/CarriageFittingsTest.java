package dev.railbound.carriage;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CarriageFittingsTest {

    private static final ParsedDesign COACH = ParsedDesign.of(TestDesigns.sample());
    private static final BlockPos ANCHOR = new BlockPos(10, 70, -4);

    @Test
    void worldToLocalUndoesTheFootprintMapping() {
        Set<BlockPos> cells = COACH.cells().stream().map(LayoutCell::pos).collect(Collectors.toSet());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Set<BlockPos> mappedBack = CarriageFootprint.positions(COACH, ANCHOR, facing).stream()
                    .map(world -> CarriageFittings.toLocal(world, ANCHOR, facing, COACH.anchor()))
                    .collect(Collectors.toSet());
            assertEquals(cells, mappedBack, "facing " + facing);
        }
    }

    @Test
    void worldToLocalPutsTheRightSideAtPositiveX() {
        BlockPos right = ANCHOR.relative(Direction.NORTH.getClockWise());
        assertEquals(COACH.anchor().east(), CarriageFittings.toLocal(right, ANCHOR, Direction.NORTH, COACH.anchor()));
    }

    @Test
    void fitsIntoASideWallFromOutside() {
        // Window band wall on the right side, clicked from outside (x = 2).
        assertTrue(CarriageFittings.canFitInto(COACH, new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), Direction.EAST));
        assertTrue(CarriageFittings.canFitInto(COACH, new BlockPos(-1, 2, 5), new BlockPos(-2, 2, 5), Direction.WEST));
    }

    @Test
    void fitsIntoTheRoofAndTheEnds() {
        assertTrue(CarriageFittings.canFitInto(COACH, new BlockPos(0, 3, 5), new BlockPos(0, 4, 5), Direction.UP));
        assertTrue(CarriageFittings.canFitInto(COACH, new BlockPos(1, 2, 0), new BlockPos(1, 2, -1), Direction.NORTH));
    }

    @Test
    void doesNotFitFromInsideTheCoach() {
        // Clicking the wall from the aisle: the cell in front is inside the coach.
        assertFalse(CarriageFittings.canFitInto(COACH, new BlockPos(1, 2, 5), new BlockPos(0, 2, 5), Direction.WEST));
    }

    @Test
    void doesNotFitUnderTheFloor() {
        assertFalse(CarriageFittings.canFitInto(COACH, new BlockPos(0, 0, 5), new BlockPos(0, -1, 5), Direction.DOWN));
    }

    @Test
    void doesNotReplaceSeatsDoorsOrTheAnchor() {
        assertFalse(CarriageFittings.canFitInto(COACH, new BlockPos(1, 0, 5), new BlockPos(2, 0, 5), Direction.EAST), "seat");
        assertFalse(CarriageFittings.canFitInto(COACH, new BlockPos(1, 1, 2), new BlockPos(2, 1, 2), Direction.EAST), "door");
        BlockPos anchor = COACH.anchor();
        assertFalse(CarriageFittings.canFitInto(COACH, anchor, anchor.above(), Direction.UP), "anchor");
    }

    @Test
    void doesNotFitIntoAnEmptyCell() {
        assertFalse(CarriageFittings.canFitInto(COACH, new BlockPos(0, 1, 5), new BlockPos(0, 1, 5).above(), Direction.UP));
    }
}
