package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParsedDesignTest {

    @Test
    void cachesSeatsAnchorAndLength() {
        ParsedDesign parsed = ParsedDesign.of(TestDesigns.sample());
        assertEquals(40, parsed.seatCount());
        assertEquals(new BlockPos(0, 3, 8), parsed.anchor());
        assertEquals(16, parsed.length());
        assertFalse(parsed.cells().isEmpty());
    }
}
