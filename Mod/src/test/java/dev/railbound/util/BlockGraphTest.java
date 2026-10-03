package dev.railbound.util;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockGraphTest {

    @Test
    void floodFillsConnectedMembersOnly() {
        Set<BlockPos> members = Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(1, 1, 0), new BlockPos(5, 0, 0));
        Set<BlockPos> found = BlockGraph.connected(new BlockPos(0, 0, 0), members::contains, 100);
        assertEquals(Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(1, 1, 0)), found);
    }

    @Test
    void nonMemberStartIsEmpty() {
        assertTrue(BlockGraph.connected(new BlockPos(9, 9, 9), p -> false, 100).isEmpty());
    }

    @Test
    void respectsTheLimit() {
        Set<BlockPos> found = BlockGraph.connected(BlockPos.ZERO, p -> p.getY() == 0 && p.getZ() == 0, 10);
        assertEquals(10, found.size());
    }
}
