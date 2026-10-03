package dev.railbound.carriage;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.railbound.trainset.item.TrainsetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Optional;

/** Every hidden carriage block: no drops of its own; wrench or breaking removes the whole carriage. */
public abstract class CarriagePartBlock extends Block implements IWrenchable {

    protected CarriagePartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties partProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        // Same protection hook as Create's own wrench pickup, so claim mods can refuse it.
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
            return InteractionResult.SUCCESS;
        }
        Optional<ResourceLocation> design = CarriageRemover.removeCarriage(level, pos);
        design.ifPresent(id -> {
            ItemStack item = TrainsetItem.of(id);
            if (player != null) {
                player.getInventory().placeItemBackInInventory(item);
            } else {
                popResource(level, pos, item);
            }
        });
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** A player broke a part: remove the whole carriage here, dropping its item unless they are in creative. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide) {
            return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        playerWillDestroy(level, pos, state, player);
        Optional<ResourceLocation> design = CarriageRemover.removeCarriage(level, pos);
        if (!player.isCreative()) {
            design.ifPresent(id -> popResource(level, pos, TrainsetItem.of(id)));
        }
        return true;
    }

    /** Any other removal (explosion, command): remove the rest of the carriage and drop its item. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        ResourceLocation knownDesign = level.getBlockEntity(pos) instanceof AnchorBlockEntity anchor ? anchor.designId() : null;
        super.onRemove(state, level, pos, newState, movedByPiston);
        // Create moves carriages with UPDATE_MOVE_BY_PISTON: assembly and disassembly must not destroy anything.
        if (level.isClientSide || movedByPiston || newState.getBlock() instanceof CarriagePartBlock || CarriageRemover.isRemoving()) {
            return;
        }
        Optional<ResourceLocation> found = CarriageRemover.removeRemainderAround(level, pos);
        ResourceLocation design = knownDesign != null ? knownDesign : found.orElse(null);
        if (design != null) {
            popResource(level, pos, TrainsetItem.of(design));
        }
    }
}
