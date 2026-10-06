package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import dev.railbound.carriage.CarriagePart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Clicks on a train are traced from the player's eye, and a block whose shape the eye is inside counts as hit. Sitting
 * in a loco cab puts your eye just inside the hidden roof layer, so every click went to the roof instead of the cab
 * controls in front of you. A hidden carriage block the eye is inside doesn't take the click; the trace goes on to
 * what you are looking at.
 */
@Mixin(value = ContraptionHandlerClient.class, remap = false)
public abstract class ContraptionClickMixin {

    @WrapOperation(method = "lambda$rayTraceContraption$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/VoxelShape;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult railbound$notFromInside(VoxelShape shape, Vec3 from, Vec3 to, BlockPos pos,
                                                          Operation<BlockHitResult> original, @Local BlockState state) {
        BlockHitResult hit = original.call(shape, from, to, pos);
        return hit != null && hit.isInside() && CarriagePart.is(state) ? null : hit;
    }
}
