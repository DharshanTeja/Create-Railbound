package dev.railbound.trainset.design;

import net.minecraft.core.BlockPos;

import java.util.List;

/** A validated design with its layout parsed once. */
public record ParsedDesign(TrainsetDesign design, List<LayoutCell> cells, int seatCount, BlockPos anchor) {

    public static ParsedDesign of(TrainsetDesign design) {
        List<LayoutCell> cells = LayoutParser.parse(design);
        int seatBlocks = (int) cells.stream().filter(c -> c.part().type() == PartType.SEAT).count();
        BlockPos anchor = cells.stream()
                .filter(c -> c.part().type() == PartType.ANCHOR)
                .map(LayoutCell::pos)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Design has no anchor: " + design.name()));
        return new ParsedDesign(design, cells, seatBlocks * TrainsetDesign.SEATS_PER_BLOCK, anchor);
    }

    public int length() {
        return design.size().length();
    }
}
