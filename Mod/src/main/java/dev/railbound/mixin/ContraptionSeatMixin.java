package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import dev.railbound.carriage.SeatPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Our seats on Create's moving trains (client and server alike):
 * <ul>
 * <li>Create seats you on any click on a seat block, whichever face you hit. Our seat blocks also carry the
 * carriage's outer wall, so a click on that outer face is ignored.</li>
 * <li>Create seats passengers at a fixed height above the seat block; ours sit {@link SeatPartBlock#SIT_OFFSET}
 * higher because the floor is half a block up the seat's layer.</li>
 * </ul>
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class ContraptionSeatMixin {
    @Shadow
    protected Contraption contraption;

    @Inject(method = "handlePlayerInteraction", at = @At("HEAD"), cancellable = true)
    private void railbound$noSittingThroughTheWall(Player player, BlockPos localPos, Direction side, InteractionHand hand,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (contraption == null) {
            return;
        }
        StructureBlockInfo info = contraption.getBlocks().get(localPos);
        if (info != null && SeatPartBlock.clickedFromOutside(info.state(), side)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyReturnValue(method = "getPassengerPosition", at = @At("RETURN"))
    private Vec3 railbound$sitOnTheCushion(Vec3 original, Entity passenger, float partialTicks) {
        if (original == null || contraption == null) {
            return original;
        }
        BlockPos seat = contraption.getSeatOf(passenger.getUUID());
        StructureBlockInfo info = seat == null ? null : contraption.getBlocks().get(seat);
        return info != null && info.state().getBlock() instanceof SeatPartBlock ? original.add(0, SeatPartBlock.SIT_OFFSET, 0) : original;
    }
}
