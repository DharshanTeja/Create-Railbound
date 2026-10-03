package dev.railbound.bogey;

import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlockEntity;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Create's small bogey with a half-height body: Railbound coaches have their floor half a block up the bogey layer,
 * so the bogey's top must be level with that floor instead of a full block bump in the aisle.
 */
public class CoachBogeyBlock extends StandardBogeyBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public CoachBogeyBlock(Properties properties) {
        super(properties, BogeySizes.SMALL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BogeyStyle getDefaultStyle() {
        return RailboundBogeyStyles.COACH;
    }

    @Override
    public BlockEntityType<? extends StandardBogeyBlockEntity> getBlockEntityType() {
        return RailboundBlockEntities.COACH_BOGEY.get();
    }
}
