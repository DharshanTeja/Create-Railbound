package dev.railbound.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import dev.railbound.cargo.CargoAccess;
import dev.railbound.carriage.CarriagePart;
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
 * On a train, sneaking with an empty hand on any part of a goods wagon opens its hold or tank, as it does on a parked
 * wagon: the click goes to the hold block hidden inside. Client and server both redirect, so Create's interaction
 * packet, which carries the clicked block, ends up at the hold too.
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class CargoClickMixin {
    @Shadow
    protected Contraption contraption;

    @Shadow
    public abstract boolean handlePlayerInteraction(Player player, BlockPos localPos, Direction side, InteractionHand hand);

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void railbound$sneakOpensTheHold(Player player, BlockPos localPos, Direction side, InteractionHand hand,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!(contraption instanceof CarriageContraption) || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        StructureBlockInfo clicked = contraption.getBlocks().get(localPos);
        if (clicked == null || !CarriagePart.is(clicked.state())) {
            return;
        }
        Optional<BlockPos> hold = CargoAccess.holdOnTrain(contraption);
        if (hold.isPresent() && !hold.get().equals(localPos)) {
            cir.setReturnValue(handlePlayerInteraction(player, hold.get(), side, hand));
        }
    }
}
