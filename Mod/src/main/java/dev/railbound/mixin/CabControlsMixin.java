package dev.railbound.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import dev.railbound.carriage.CabControls;
import dev.railbound.carriage.FrameBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * On a train, clicking the hidden cab blocks a loco draws its controls on (regulator, gauges, backhead) drives the
 * train: the click goes to the Train Controls beside them (see {@link CabControls}). Client and server both redirect,
 * so Create's interaction packet, which carries the clicked block, ends up at the controls too.
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class CabControlsMixin {
    @Shadow
    protected Contraption contraption;

    @Shadow
    public abstract boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand hand);

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void railbound$cabDrivesControls(Player player, BlockPos localPos, Direction side, InteractionHand hand,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!(contraption instanceof CarriageContraption)) {
            return;
        }
        StructureBlockInfo clicked = contraption.getBlocks().get(localPos);
        if (clicked == null || !(clicked.state().getBlock() instanceof FrameBlock)) {
            return;
        }
        Optional<BlockPos> controls = CabControls.controlsFor(localPos, pos -> {
            StructureBlockInfo info = contraption.getBlocks().get(pos);
            return info != null && (AllBlocks.TRAIN_CONTROLS.has(info.state())
                    || info.state().getBlock() instanceof dev.railbound.carriage.CabControlsBlock);
        });
        controls.ifPresent(pos -> cir.setReturnValue(handlePlayerInteraction(player, pos, side, hand)));
    }
}
