package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A validated design with its layout parsed once. */
public record ParsedDesign(TrainsetDesign design, List<LayoutCell> cells, int seatCount, BlockPos anchor,
                           Map<BlockPos, HiddenPart> parts) {

    public static ParsedDesign of(TrainsetDesign design) {
        List<LayoutCell> cells = LayoutParser.parse(design);
        int seatBlocks = (int) cells.stream().filter(c -> c.part().type() == PartType.SEAT).count();
        BlockPos anchor = cells.stream()
                .filter(c -> c.part().type() == PartType.ANCHOR)
                .map(LayoutCell::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Design has no anchor: " + design.name()));
        Map<BlockPos, HiddenPart> parts = new HashMap<>();
        cells.forEach(cell -> parts.put(cell.pos(), cell.part()));
        return new ParsedDesign(design, cells, seatBlocks * TrainsetDesign.SEATS_PER_BLOCK, anchor, Map.copyOf(parts));
    }

    public int length() {
        return design.size().length();
    }

    /** The part at a local layout position, or empty for air and positions outside the layout. */
    public Optional<HiddenPart> partAt(BlockPos local) {
        return Optional.ofNullable(parts.get(local));
    }
}
