package dev.railbound.carriage;

import dev.railbound.steam.BunkerBlockEntity;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.railbound.trainset.design.BogeySpec;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.placement.PlacedPart;
import dev.railbound.trainset.placement.PlacementPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class CarriageBuilder {
    private CarriageBuilder() {}

    /**
     * Places bogeys (as Create does for Railway Casing) using each design bogey's style and that style's block, then
     * the hidden blocks, then stamps the anchor.
     */
    public static void build(Level level, PlacementPlan plan, ParsedDesign design, ResourceLocation designId) {
        List<BogeySpec> specs = design.design().bogeys();
        for (int i = 0; i < plan.bogeys().size(); i++) {
            BlockPos bogeyPos = plan.bogeys().get(i);
            BlockPos trackPos = bogeyPos.below();
            BlockState trackState = level.getBlockState(trackPos);
            if (!(trackState.getBlock() instanceof ITrackBlock track)) {
                continue;
            }
            BlockState bogey = track.getBogeyAnchor(level, trackPos, trackState);
            BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(specs.get(i).style());
            if (bogey.getBlock() instanceof AbstractBogeyBlock<?> anchorBlock) {
                if (style != null && style.validSizes().contains(anchorBlock.getSize())) {
                    bogey = style.getBlockForSize(anchorBlock.getSize()).withPropertiesOf(bogey);
                }
                if (bogey.getBlock() instanceof AbstractBogeyBlock<?> bogeyBlock) {
                    bogey = bogeyBlock.getVersion(bogey, false);
                }
            }
            level.setBlock(bogeyPos, bogey, Block.UPDATE_ALL);
            if (style != null && bogey.getBlock() instanceof AbstractBogeyBlock<?> bogeyBlock
                    && style.validSizes().contains(bogeyBlock.getSize())
                    && level.getBlockEntity(bogeyPos) instanceof AbstractBogeyBlockEntity bogeyEntity) {
                bogeyEntity.setBogeyStyle(style);
            }
        }
        for (PlacedPart part : plan.parts()) {
            level.setBlock(part.pos(), CarriageBlocks.stateFor(design, part.part(), plan.facing(), part.local()), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(plan.anchor()) instanceof AnchorBlockEntity anchor) {
            anchor.setDesignId(designId);
        }
        for (PlacedPart part : plan.parts()) {
            if (level.getBlockEntity(part.pos()) instanceof BunkerBlockEntity bunker) {
                bunker.setDesignId(designId);
            }
        }
    }
}
