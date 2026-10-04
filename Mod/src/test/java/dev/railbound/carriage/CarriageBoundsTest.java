package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CarriageBoundsTest {

    @Test
    void theBlocksExtentCoversEveryBlockWhole() {
        AABB extent = CarriageBounds.extent(List.of(new BlockPos(-1, 0, 0), new BlockPos(1, 3, 15)));
        assertEquals(new AABB(-1, 0, 0, 2, 4, 16), extent);
    }

    @Test
    void anUnturnedCarriageGetsItsOwnBlocksWithAMargin() {
        AABB box = CarriageBounds.world(new AABB(-1, 0, 0, 2, 4, 16), v -> v.add(100, 64, 200));
        assertEquals(new AABB(99 - CarriageBounds.MARGIN, 64 - CarriageBounds.MARGIN, 200 - CarriageBounds.MARGIN,
                102 + CarriageBounds.MARGIN, 68 + CarriageBounds.MARGIN, 216 + CarriageBounds.MARGIN), box);
    }

    @Test
    void aTurnedCarriageGetsATightBoxNotACarriageLengthSquare() {
        // a quarter turn about y: the 3 x 16 footprint lies across x now
        AABB box = CarriageBounds.world(new AABB(-1, 0, 0, 2, 4, 16), v -> new Vec3(v.z, v.y, -v.x));
        assertEquals(16 + 2 * CarriageBounds.MARGIN, box.getXsize(), 1e-9);
        assertEquals(3 + 2 * CarriageBounds.MARGIN, box.getZsize(), 1e-9);
    }
}
