package dev.railbound.steam;

import com.mojang.serialization.MapCodec;
import dev.railbound.registry.RailboundBlockEntities;
import com.simibubi.create.content.trains.track.ITrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.OptionalInt;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A trackside water crane. Pipe water into its base; its arm (FACING: towards the track) swings out and fills a
 * stopped steam loco whose tank filler is in reach of the hose. With no loco there it takes nothing, so pipes simply
 * back up and nothing spills.
 */
public class WaterCraneBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<WaterCraneBlock> CODEC = simpleCodec(WaterCraneBlock::new);
    /** The base and the lower column; the rest of the crane towers above, drawn by its renderer. */
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 24, 13);

    public WaterCraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** It turns its arm towards the nearest track within three blocks; with none near, the way the player faces. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = WaterCrane.towardsTrack(direction -> {
            for (int distance = 1; distance <= 3; distance++) {
                BlockPos ahead = pos.relative(direction, distance);
                if (level.getBlockState(ahead).getBlock() instanceof ITrackBlock
                        || level.getBlockState(ahead.below()).getBlock() instanceof ITrackBlock) {
                    return OptionalInt.of(distance);
                }
            }
            return OptionalInt.empty();
        }).orElse(context.getHorizontalDirection());
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterCraneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RailboundBlockEntities.WATER_CRANE.get()
                ? (lvl, pos, st, be) -> ((WaterCraneBlockEntity) be).tick() : null;
    }
}
