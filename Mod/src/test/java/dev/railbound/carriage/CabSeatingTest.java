package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CabSeatingTest {
    private static final BlockPos SEAT = new BlockPos(-1, 1, 12);

    private static Optional<BlockPos> controlsFor(Map<BlockPos, Direction> controls) {
        return controlsFor(controls, java.util.Set.of());
    }

    /** Controls (with their facing) and other blocks; everything else is open. */
    private static Optional<BlockPos> controlsFor(Map<BlockPos, Direction> controls, java.util.Set<BlockPos> blocks) {
        return CabSeating.controlsFor(SEAT, pos -> Optional.ofNullable(controls.get(pos)),
                pos -> !controls.containsKey(pos) && !blocks.contains(pos));
    }

    @Test
    void aSeatDrivesTheControlsThatFaceIt() {
        // as Create pairs a conductor seat: controls right beside it, facing back at it
        BlockPos ahead = SEAT.north();
        assertEquals(Optional.of(ahead), controlsFor(Map.of(ahead, Direction.SOUTH)));
        BlockPos behind = SEAT.south();
        assertEquals(Optional.of(behind), controlsFor(Map.of(behind, Direction.NORTH)));
    }

    @Test
    void controlsFacingAnotherWayBelongToAnotherSeat() {
        assertEquals(Optional.empty(), controlsFor(Map.of(SEAT.north(), Direction.NORTH)));
        assertEquals(Optional.empty(), controlsFor(Map.of(SEAT.north().north().north(), Direction.SOUTH)), "too far");
    }

    @Test
    void aSeatAlsoDrivesControlsAcrossAnOpenDoorway() {
        // the tank engine's bunker-first seat: its controls after the doorway, one open cell behind it
        BlockPos acrossTheDoorway = SEAT.south().south();
        assertEquals(Optional.of(acrossTheDoorway), controlsFor(Map.of(acrossTheDoorway, Direction.NORTH)));
        assertEquals(Optional.empty(), controlsFor(Map.of(acrossTheDoorway, Direction.NORTH), java.util.Set.of(SEAT.south())),
                "not through a wall");
    }

    @Test
    void aSeatWithNoControlsIsAPassengerSeat() {
        assertEquals(Optional.empty(), controlsFor(Map.of()));
    }
}
