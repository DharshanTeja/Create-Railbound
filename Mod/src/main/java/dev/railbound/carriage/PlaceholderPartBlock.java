package dev.railbound.carriage;

import dev.railbound.trainset.design.FrameShape;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlaceholderPartBlock extends CarriagePartBlock {
    /** True on the floor layer: the cell has a walkable floor. */
    public static final BooleanProperty FLOOR = BooleanProperty.create("floor");
    /** The outer wall a side-column part carries, so the carriage side stays closed. */
    public static final EnumProperty<PlaceholderSide> SIDE = EnumProperty.create("side", PlaceholderSide.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape MARKER = Block.box(6, 6, 6, 10, 10, 10);

    public PlaceholderPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FLOOR, true)
                .setValue(SIDE, PlaceholderSide.NONE)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOOR, SIDE, FACING);
    }

    /** Floor slab (floor layer) plus the outer wall (side columns). */
    public static VoxelShape collisionFor(boolean floor, PlaceholderSide side, Direction facing) {
        VoxelShape shape = floor ? FrameShapes.FLOOR_SLAB : Shapes.empty();
        return switch (side) {
            case LEFT -> Shapes.or(shape, FrameShapes.get(FrameShape.WALL_LEFT, facing));
            case RIGHT -> Shapes.or(shape, FrameShapes.get(FrameShape.WALL_RIGHT, facing));
            case NONE -> shape;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(MARKER, collisionFor(state.getValue(FLOOR), state.getValue(SIDE), state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionFor(state.getValue(FLOOR), state.getValue(SIDE), state.getValue(FACING));
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
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
                .setValue(SIDE, state.getValue(SIDE).mirrored());
    }
}
