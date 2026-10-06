package dev.railbound.carriage;

import dev.railbound.carriage.CarriageAttachment.Kind;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.PASS;
import static com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

class CarriageAttachmentTest {

    @Test
    void partsStickToNeighbouringParts() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(Kind.PART, Kind.PART));
    }

    @Test
    void partsAndBogeysStickToEachOther() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(Kind.PART, Kind.BOGEY));
        assertEquals(SUCCESS, CarriageAttachment.evaluate(Kind.BOGEY, Kind.PART));
    }

    @Test
    void createMayMoveCarriagePartsThoughPistonsMayNot() {
        // parts refuse pistons (PushReaction.BLOCK), which Create would also read as "not movable" without this
        assertEquals(SUCCESS, CarriageAttachment.movementAllowed(true));
        assertEquals(PASS, CarriageAttachment.movementAllowed(false));
    }

    @Test
    void pistonsCannotPushCarriageParts() throws ReflectiveOperationException {
        // a pushed part leaves a hole in the carriage and a stray hidden block: removal ignores piston moves
        java.lang.reflect.Field field = net.minecraft.world.level.block.state.BlockBehaviour.Properties.class
                .getDeclaredField("pushReaction");
        field.setAccessible(true);
        assertEquals(net.minecraft.world.level.material.PushReaction.BLOCK, field.get(CarriageParts.properties()));
    }

    @Test
    void partDoesNotStickToOrdinaryBlocks() {
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.PART, Kind.OTHER));
    }

    @Test
    void fittingsAndPartsStickToEachOther() {
        assertEquals(SUCCESS, CarriageAttachment.evaluate(Kind.PART, Kind.FITTING));
        assertEquals(SUCCESS, CarriageAttachment.evaluate(Kind.FITTING, Kind.PART));
    }

    @Test
    void fittingsDoNotPullInBogeysOrOtherBlocks() {
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.FITTING, Kind.BOGEY));
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.FITTING, Kind.OTHER));
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.FITTING, Kind.FITTING));
    }

    @Test
    void unrelatedBlocksAreLeftToCreate() {
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.OTHER, Kind.PART));
        assertEquals(PASS, CarriageAttachment.evaluate(Kind.BOGEY, Kind.OTHER));
    }

    @Test
    void aFittingCountsUnlessItsFrontFacesTheOtherBlock() {
        assertEquals(Kind.FITTING, CarriageAttachment.kind(false, false, true, Optional.of(Direction.EAST), Direction.WEST));
        assertEquals(Kind.FITTING, CarriageAttachment.kind(false, false, true, Optional.of(Direction.EAST), Direction.UP));
        // A station-side interface facing the train must stay behind.
        assertEquals(Kind.OTHER, CarriageAttachment.kind(false, false, true, Optional.of(Direction.EAST), Direction.EAST));
    }

    @Test
    void kindPrefersPartThenBogey() {
        assertEquals(Kind.PART, CarriageAttachment.kind(true, false, false, Optional.empty(), Direction.UP));
        assertEquals(Kind.BOGEY, CarriageAttachment.kind(false, true, false, Optional.empty(), Direction.UP));
        assertEquals(Kind.OTHER, CarriageAttachment.kind(false, false, false, Optional.empty(), Direction.UP));
    }

    @Test
    void emptyColliderPartsMustStillBeMoved() {
        assertEquals(SUCCESS, CarriageAttachment.movementNecessary(true));
        assertEquals(PASS, CarriageAttachment.movementNecessary(false));
    }
}
