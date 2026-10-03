package dev.railbound.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import dev.railbound.carriage.DoorPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create rebuilds a moving train's whole client copy when any block changes, except its own sliding doors.
 * Our carriage doors get the same treatment, so their slide animation (and the train's lighting) survives.
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class AbstractContraptionEntityMixin {
    @Shadow
    protected Contraption contraption;

    @Inject(method = "handleBlockChange", at = @At("HEAD"), cancellable = true)
    private void railbound$keepDoorAnimation(BlockPos localPos, BlockState newState, CallbackInfo ci) {
        if (contraption == null || !(newState.getBlock() instanceof DoorPartBlock)) {
            return;
        }
        StructureBlockInfo info = contraption.getBlocks().get(localPos);
        if (info == null) {
            return;
        }
        contraption.getBlocks().put(localPos, new StructureBlockInfo(info.pos(), newState, info.nbt()));
        contraption.invalidateColliders();
        ci.cancel();
    }
}
