package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarriageProtectionTest {

    @Test
    void explosionsSkipBogeysThatCarryACarriage() {
        BlockPos carriageBogey = new BlockPos(0, 65, 0);
        BlockPos part = new BlockPos(0, 66, 0);
        BlockPos stone = new BlockPos(3, 64, 0);
        List<BlockPos> affected = new ArrayList<>(List.of(carriageBogey, part, stone));
        CarriageProtection.removeCarriageBogeys(affected, carriageBogey::equals);
        assertEquals(List.of(part, stone), affected);
    }
}
