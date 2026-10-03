package dev.railbound.trainset.placement;

import dev.railbound.trainset.design.HiddenPart;
import net.minecraft.core.BlockPos;

public record PlacedPart(BlockPos pos, HiddenPart part, BlockPos local) {
}
