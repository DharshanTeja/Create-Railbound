package dev.railbound.steam;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.railbound.registry.RailboundBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

/**
 * A trackside water crane, five blocks tall. Pipe water into its base; its arm (FACING: towards the track) swings out
 * and fills a stopped steam loco whose tank filler is in reach. With no loco there it takes nothing, so pipes simply
 * back up and nothing spills. At rest the arm lies along the track so trains pass under it; Create's wrench turns it.
 * The base (section 0) holds the block entity and draws the crane; the column blocks above it (sections 1 to
 * {@link WaterCrane#TOP}) are invisible but solid, so the whole crane can be hit, bumped into and broken.
 */
public class WaterCraneBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
    public static final MapCodec<WaterCraneBlock> CODEC = simpleCodec(WaterCraneBlock::new);
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, WaterCrane.TOP);
    private static final VoxelShape COLUMN = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape BASE = Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(3, 5, 3, 13, 16, 13));
    /** The top of the column, the capital and turntable under the arm, and the king post above. */
    private static final VoxelShape TOP = Shapes.or(Block.box(3, 0, 3, 13, 8, 13), Block.box(2, 7, 2, 14, 11, 14),
            Block.box(6.5, 11, 6.5, 9.5, 16, 9.5));

    public WaterCraneBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SECTION, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SECTION);
    }

    /**
     * Needs room for the whole crane above. It turns its arm towards the nearest track within three blocks; with none
     * near, the way the player faces.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() + WaterCrane.TOP >= level.getMaxBuildHeight()) {
            return null;
        }
        for (int section = 1; section <= WaterCrane.TOP; section++) {
            if (!level.getBlockState(pos.above(section)).canBeReplaced(context)) {
                return null;
            }
        }
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

    /** Builds the column above the base. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        for (int section = 1; section <= WaterCrane.TOP; section++) {
            level.setBlock(pos.above(section), state.setValue(SECTION, section), Block.UPDATE_ALL);
        }
    }

    /** A crane block goes when the crane block below or above it does, so breaking any part brings the crane down. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis() != Direction.Axis.Y) {
            return state;
        }
        int section = state.getValue(SECTION);
        boolean below = isSection(level.getBlockState(pos.below()), section - 1);
        boolean above = isSection(level.getBlockState(pos.above()), section + 1);
        return WaterCrane.sectionStands(section, below, above) ? state : Blocks.AIR.defaultBlockState();
    }

    private boolean isSection(BlockState state, int section) {
        return state.is(this) && state.getValue(SECTION) == section;
    }

    /** The wrench turns the crane a quarter, whichever of its blocks or faces is clicked. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos base = context.getClickedPos().below(state.getValue(SECTION));
        if (!level.isClientSide) {
            Direction turned = state.getValue(FACING).getClockWise();
            for (int section = 0; section <= WaterCrane.TOP; section++) {
                BlockState part = level.getBlockState(base.above(section));
                if (part.is(this)) {
                    level.setBlock(base.above(section), part.setValue(FACING, turned), Block.UPDATE_CLIENTS);
                }
            }
            IWrenchable.playRotateSound(level, base);
        }
        return InteractionResult.SUCCESS;
    }

    /** Only the base is drawn as a block; its renderer draws the rest of the crane. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(SECTION) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int section = state.getValue(SECTION);
        return section == 0 ? BASE : section == WaterCrane.TOP ? TOP : COLUMN;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SECTION) == 0 ? new WaterCraneBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(SECTION) == 0 && type == RailboundBlockEntities.WATER_CRANE.get()
                ? (lvl, pos, st, be) -> ((WaterCraneBlockEntity) be).tick() : null;
    }
}
