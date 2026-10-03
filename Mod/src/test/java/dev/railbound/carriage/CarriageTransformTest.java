package dev.railbound.carriage;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarriageTransformTest {

    private static final ParsedDesign COACH = ParsedDesign.of(TestDesigns.sample());
    private static final BlockPos ANCHOR = new BlockPos(100, 64, -20);

    @Test
    void aCarriageFacingNorthNeedsNoTurn() {
        assertEquals(0, CarriageTransform.yRotation(Direction.NORTH));
    }

    @Test
    void turnsAQuarterClockwisePerStepFromNorth() {
        assertEquals(-90, CarriageTransform.yRotation(Direction.EAST));
        assertEquals(-180, CarriageTransform.yRotation(Direction.SOUTH));
        assertEquals(-270, CarriageTransform.yRotation(Direction.WEST));
    }

    @Test
    void everyCellCentreLandsOnTheBlockTheBuilderPlacesForIt() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (LayoutCell cell : COACH.cells()) {
                BlockPos local = cell.pos();
                Vec3 relative = CarriageTransform.toAnchorRelative(COACH, facing,
                        local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
                BlockPos world = CarriageFootprint.worldPos(COACH, ANCHOR, facing, local);
                assertEquals(Vec3.atCenterOf(world), relative.add(Vec3.atLowerCornerOf(ANCHOR)),
                        "cell " + local + " facing " + facing);
            }
        }
    }

    @Test
    void worldPosIsWhatTheFootprintIsMadeOf() {
        for (LayoutCell cell : COACH.cells()) {
            assertTrue(CarriageFootprint.positions(COACH, ANCHOR, Direction.EAST)
                    .contains(CarriageFootprint.worldPos(COACH, ANCHOR, Direction.EAST, cell.pos())));
        }
    }
}
