package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LayoutParserTest {

    @Test
    void parsesFrameWithShape() {
        assertEquals(Optional.of(new HiddenPart(PartType.FRAME, Optional.of(FrameShape.FLOOR_WALL_LEFT))),
                HiddenPart.parse("frame:floor_wall_left"));
    }

    @Test
    void parsesBareTypes() {
        assertEquals(Optional.of(new HiddenPart(PartType.SEAT, Optional.empty())), HiddenPart.parse("seat"));
        assertEquals(Optional.of(new HiddenPart(PartType.CARGO_FLUID, Optional.empty())), HiddenPart.parse("cargo_fluid"));
    }

    @Test
    void rejectsBadParts() {
        assertTrue(HiddenPart.parse("frame").isEmpty(), "frame needs a shape");
        assertTrue(HiddenPart.parse("frame:triangle").isEmpty(), "unknown shape");
        assertTrue(HiddenPart.parse("seat:floor").isEmpty(), "only frames take a shape");
        assertTrue(HiddenPart.parse("rocket").isEmpty(), "unknown type");
        assertTrue(HiddenPart.parse("frame:floor:extra").isEmpty(), "too many parts");
    }

    @Test
    void mapsCharactersToPositions() {
        List<LayoutCell> cells = LayoutParser.parse(TestDesigns.sample());
        assertTrue(cells.contains(new LayoutCell(new BlockPos(-1, 0, 3), new HiddenPart(PartType.SEAT, Optional.empty()))),
                "left seat at row 3");
        assertTrue(cells.contains(new LayoutCell(new BlockPos(1, 0, 2), new HiddenPart(PartType.DOOR, Optional.empty()))),
                "right door at row 2");
        assertTrue(cells.contains(new LayoutCell(new BlockPos(0, 2, 8), new HiddenPart(PartType.ANCHOR, Optional.empty()))),
                "anchor in the roof layer, row 8");
    }

    @Test
    void skipsAir() {
        List<LayoutCell> cells = LayoutParser.parse(TestDesigns.sample());
        assertTrue(cells.stream().noneMatch(c -> c.part().type() == PartType.AIR));
        assertTrue(cells.stream().noneMatch(c -> c.pos().equals(new BlockPos(0, 1, 5))), "aisle above floor is air");
    }

    @Test
    void sampleHasFortySeats() {
        assertEquals(40, TestDesigns.sample().seatCount());
    }
}
