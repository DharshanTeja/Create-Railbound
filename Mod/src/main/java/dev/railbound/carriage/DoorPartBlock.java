package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A carriage door half. It carries the vanilla door properties, which is all Create's sliding-door
 * movement behaviour needs to open it at stations. FACING points out of the carriage.
 */
public class DoorPartBlock extends CarriagePartBlock {
    public static final DirectionProperty FACING = DoorBlock.FACING;
    public static final BooleanProperty OPEN = DoorBlock.OPEN;
    public static final EnumProperty<DoubleBlockHalf> HALF = DoorBlock.HALF;
    /** Thin posts at both edges of the doorway, so an open door can still be clicked shut. */
    private static final VoxelShape OPEN_FRAME = Shapes.or(
            Block.box(0, 0, 0, 2, 16, 2), Block.box(14, 0, 0, 16, 16, 2));

    public DoorPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return FrameShapes.rotate(OPEN_FRAME, state.getValue(FACING));
        }
        return InteriorParts.doorShape(state.getValue(FACING), false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return InteriorParts.doorShape(state.getValue(FACING), state.getValue(OPEN));
    }

    /** Opens or closes both halves of a standing carriage's door. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        boolean open = !state.getValue(OPEN);
        BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        BlockState other = level.getBlockState(otherPos);
        if (other.getBlock() == this && other.getValue(HALF) != state.getValue(HALF)) {
            level.setBlock(otherPos, other.setValue(OPEN, open), Block.UPDATE_ALL);
        }
        level.playSound(player, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1, 1);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
