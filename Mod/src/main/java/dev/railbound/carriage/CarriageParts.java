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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Behaviour shared by every {@link CarriagePart}: no drops of its own; wrench or breaking removes the whole carriage. */
public final class CarriageParts {
    private CarriageParts() {}

    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.5f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                // a pushed part would leave a hole and a stray hidden block; Create still moves them (CarriageAttachment)
                .pushReaction(PushReaction.BLOCK);
    }

    /** Sneak + wrench: pick the whole carriage up as its trainset item. */
    public static InteractionResult sneakWrenched(BlockState state, UseOnContext context) {
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
                Block.popResource(level, pos, item);
            }
        });
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Server side of a player breaking a part: remove the whole carriage, dropping its item unless in creative. */
    public static void destroyedByPlayer(Level level, BlockPos pos, Player player) {
        Optional<ResourceLocation> design = CarriageRemover.removeCarriage(level, pos);
        if (!player.isCreative()) {
            design.ifPresent(id -> Block.popResource(level, pos, TrainsetItem.of(id)));
        }
    }

    /** The design an anchor at pos remembers; call before the block entity is removed. */
    @Nullable
    public static ResourceLocation anchorDesign(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AnchorBlockEntity anchor ? anchor.designId() : null;
    }

    /** Any other removal (explosion, command): remove the rest of the carriage and drop its item. */
    public static void removed(Level level, BlockPos pos, BlockState newState, boolean movedByPiston,
                               @Nullable ResourceLocation knownDesign) {
        // Create moves carriages with UPDATE_MOVE_BY_PISTON: assembly and disassembly must not destroy anything.
        if (level.isClientSide || movedByPiston || CarriagePart.is(newState) || CarriageRemover.isRemoving()) {
            return;
        }
        Optional<ResourceLocation> found = CarriageRemover.removeRemainderAround(level, pos);
        ResourceLocation design = knownDesign != null ? knownDesign : found.orElse(null);
        if (design != null) {
            Block.popResource(level, pos, TrainsetItem.of(design));
        }
    }
}
