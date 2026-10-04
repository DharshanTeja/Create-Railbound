package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boarding step under a door: climbable (block tag {@code minecraft:climbable}, which Create also honours on
 * moving trains). Its ladder is on the outer face below the floor, climbed from outside ({@link StepClimbing}),
 * and it carries the floor, so the doorway above is open and nobody falls through. FACING points out of the carriage.
 * HIGH marks a cab step beside a loco's full-height floor, whose ladder climbs right up to that floor.
 */
public class StepPartBlock extends CarriagePartBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HIGH = BooleanProperty.create("high");

    public StepPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HIGH, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HIGH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return InteriorParts.stepShape(state.getValue(FACING), state.getValue(HIGH));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return InteriorParts.stepShape(state.getValue(FACING), state.getValue(HIGH));
    }

    /** Whether this state is a step whose ladder faces {@code outward} (towards someone standing on that side). */
    public static boolean faces(BlockState state, Direction outward) {
        return state.getBlock() instanceof StepPartBlock && state.getValue(FACING) == outward;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
