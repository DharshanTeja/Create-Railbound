package dev.railbound.carriage;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Base for hidden carriage blocks; the shared rules live in {@link CarriageParts}. */
public abstract class CarriagePartBlock extends Block implements CarriagePart, IWrenchable {

    protected CarriagePartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties partProperties() {
        return CarriageParts.properties();
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
