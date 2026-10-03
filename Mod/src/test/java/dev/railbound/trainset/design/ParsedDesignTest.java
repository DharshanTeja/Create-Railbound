package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParsedDesignTest {

    @Test
    void cachesSeatsAnchorAndLength() {
        ParsedDesign parsed = ParsedDesign.of(TestDesigns.sample());
        assertEquals(20, parsed.seatCount());
        assertEquals(new BlockPos(0, 3, 8), parsed.anchor());
        assertEquals(16, parsed.length());
        assertFalse(parsed.cells().isEmpty());
    }

    @Test
    void looksUpPartsByPosition() {
        ParsedDesign parsed = ParsedDesign.of(TestDesigns.sample());
        assertEquals(java.util.Optional.of(PartType.SEAT), parsed.partAt(new BlockPos(-1, 1, 3)).map(HiddenPart::type));
        assertEquals(java.util.Optional.of(PartType.DOOR), parsed.partAt(new BlockPos(1, 2, 2)).map(HiddenPart::type));
        assertTrue(parsed.partAt(new BlockPos(0, 1, 3)).isEmpty(), "aisle is air");
    }
}
