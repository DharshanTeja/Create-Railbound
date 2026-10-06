package dev.railbound.carriage;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A loco cab's driving controls: a slim control stand against the cab wall, drawn by the loco's own model, in place of
 * Create's Train Controls desk. It is Create's Train Controls in every way that matters (Create counts it as controls
 * through {@code CarriageControlsMixin}, drives with it through Create's own interaction, and pairs it with the
 * conductor seat it faces), and a carriage part like the seats. Sitting in that seat takes the controls.
 */
public class CabControlsBlock extends ControlsBlock implements CarriagePart {
    public static final MapCodec<CabControlsBlock> CODEC = simpleCodec(CabControlsBlock::new);
    public static final EnumProperty<PlaceholderSide> SIDE = PlaceholderPartBlock.SIDE;

    public CabControlsBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SIDE, PlaceholderSide.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(SIDE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return InteriorParts.cabControlsShape(state.getValue(FACING), state.getValue(SIDE));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return CarriageParts.sneakWrenched(state, context);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        playerWillDestroy(level, pos, state, player);
        CarriageParts.destroyedByPlayer(level, pos, player);
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        ResourceLocation knownDesign = CarriageParts.anchorDesign(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
        CarriageParts.removed(level, pos, newState, movedByPiston, knownDesign);
    }
}
