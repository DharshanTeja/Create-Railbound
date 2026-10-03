package dev.railbound.carriage;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.placement.PlacedPart;
import dev.railbound.trainset.placement.PlacementPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CarriageBuilder {
    private CarriageBuilder() {}

    /** Places bogeys (as Create does for Railway Casing), then the hidden blocks, then stamps the anchor. */
    public static void build(Level level, PlacementPlan plan, ParsedDesign design, ResourceLocation designId) {
        for (BlockPos bogeyPos : plan.bogeys()) {
            BlockPos trackPos = bogeyPos.below();
            BlockState trackState = level.getBlockState(trackPos);
            if (trackState.getBlock() instanceof ITrackBlock track) {
                BlockState bogey = track.getBogeyAnchor(level, trackPos, trackState);
                if (bogey.getBlock() instanceof AbstractBogeyBlock<?> bogeyBlock) {
                    bogey = bogeyBlock.getVersion(bogey, false);
                }
                level.setBlock(bogeyPos, bogey, Block.UPDATE_ALL);
            }
        }
        for (PlacedPart part : plan.parts()) {
            level.setBlock(part.pos(), CarriageBlocks.stateFor(design, part.part(), plan.facing(), part.local()), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(plan.anchor()) instanceof AnchorBlockEntity anchor) {
            anchor.setDesignId(designId);
        }
    }
}
