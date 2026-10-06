package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.railbound.carriage.CabControlsBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import dev.railbound.carriage.CabSeating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

/**
 * Create recognises Train Controls by their block in exactly two places: counting a carriage's forward and backward
 * controls as it assembles, and pairing a conductor seat with the controls it faces. Our cab controls count as
 * Train Controls in both; everywhere else Create goes by the controls' facing, which ours share. A seat also pairs
 * with our cab controls across one open cell (a doorway), as {@link CabSeating} allows.
 */
@Mixin(value = CarriageContraption.class, remap = false)
public abstract class CarriageControlsMixin {
    private static final String HAS = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z";

    @WrapOperation(method = "capture", at = @At(value = "INVOKE", target = HAS))
    private boolean railbound$countCabControls(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof CabControlsBlock;
    }

    @WrapOperation(method = "inControl", at = @At(value = "INVOKE", target = HAS))
    private boolean railbound$pairCabControls(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return original.call(entry, state) || state.getBlock() instanceof CabControlsBlock;
    }

    @Inject(method = "inControl", at = @At("RETURN"), cancellable = true)
    private void railbound$pairAcrossDoorway(BlockPos seat, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        Map<BlockPos, StructureBlockInfo> blocks = ((Contraption) (Object) this).getBlocks();
        Optional<BlockPos> controls = CabSeating.controlsToward(seat, direction, pos -> {
            StructureBlockInfo info = blocks.get(pos);
            return info != null && info.state().getBlock() instanceof CabControlsBlock
                    ? Optional.of(info.state().getValue(ControlsBlock.FACING)) : Optional.empty();
        }, pos -> !blocks.containsKey(pos));
        if (controls.isPresent()) {
            cir.setReturnValue(true);
        }
    }
}
