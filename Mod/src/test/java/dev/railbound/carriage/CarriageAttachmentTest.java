package dev.railbound.carriage;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.PASS;
import static com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

class CarriageAttachmentTest {

    @Test
    void partsStickToNeighbouringParts() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(true, false, true, false, Direction.EAST));
    }

    @Test
    void partSticksToTheBogeyBelow() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(true, false, false, true, Direction.DOWN));
    }

    @Test
    void bogeySticksToThePartAbove() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(false, true, true, false, Direction.UP));
    }

    @Test
    void floorLayerPartsStickToTheBogeyBesideThem() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(true, false, false, true, Direction.NORTH));
        assertEquals(SUCCESS, CarriageAttachment.evaluate(false, true, true, false, Direction.EAST));
    }

    @Test
    void partDoesNotStickToOrdinaryBlocks() {
        assertEquals(PASS, CarriageAttachment.evaluate(true, false, false, false, Direction.UP));
    }

    @Test
    void emptyColliderPartsMustStillBeMoved() {
        assertEquals(SUCCESS, CarriageAttachment.movementNecessary(true));
        assertEquals(PASS, CarriageAttachment.movementNecessary(false));
    }

    @Test
    void unrelatedBlocksAreLeftToCreate() {
        assertEquals(PASS, CarriageAttachment.evaluate(false, false, true, false, Direction.UP));
        assertEquals(PASS, CarriageAttachment.evaluate(false, true, false, false, Direction.UP));
    }
}
