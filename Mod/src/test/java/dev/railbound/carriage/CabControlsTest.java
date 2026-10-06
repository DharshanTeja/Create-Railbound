package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CabControlsTest {
    // the steam loco's cab: Train Controls on the floor layer in front of the seat, drawn regulator on the backhead
    private static final BlockPos CONTROLS = new BlockPos(0, 1, 11);

    private static Optional<BlockPos> target(BlockPos clicked, BlockPos... controls) {
        Set<BlockPos> all = Set.of(controls);
        return CabControls.controlsFor(clicked, all::contains);
    }

    @Test
    void theBackheadAboveAndAheadOfTheControlsDrivesThem() {
        assertEquals(Optional.of(CONTROLS), target(new BlockPos(0, 2, 10), CONTROLS));
    }

    @Test
    void theCabBlocksBesideTheControlsDriveThem() {
        assertEquals(Optional.of(CONTROLS), target(new BlockPos(-1, 1, 11), CONTROLS));
        assertEquals(Optional.of(CONTROLS), target(new BlockPos(0, 2, 11), CONTROLS));
    }

    @Test
    void blocksFurtherAwayOrBelowTheControlsDoNot() {
        assertEquals(Optional.empty(), target(new BlockPos(0, 2, 8), CONTROLS));
        assertEquals(Optional.empty(), target(new BlockPos(0, 0, 11), CONTROLS), "the floor under the controls");
        assertEquals(Optional.empty(), target(new BlockPos(0, 3, 11), CONTROLS), "the roof two layers up");
    }

    @Test
    void withControlsAtBothEndsOfTheCabTheNearerOneIsDriven() {
        BlockPos rear = new BlockPos(0, 1, 13);
        assertEquals(Optional.of(rear), target(new BlockPos(0, 2, 14), CONTROLS, rear));
        assertEquals(Optional.of(CONTROLS), target(new BlockPos(0, 2, 10), CONTROLS, rear));
    }
}
