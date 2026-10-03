package dev.railbound.trainset.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public record PlacementPlan(
        Direction facing,
        int firstOffset,
        int lastOffset,
        List<PlacedPart> parts,
        List<BlockPos> bogeys,
        List<Integer> bogeyOffsets,
        BlockPos anchor) {
}
