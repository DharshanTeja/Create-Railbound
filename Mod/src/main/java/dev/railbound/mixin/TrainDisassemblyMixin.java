package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.coupling.TrainCoupling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A train too long for the station's straight still disassembles: Create allows it when at least the carriage at the
 * station fits, and disassembling first uncouples the carriages that don't fit (on a curve, round a corner), which stay
 * behind as their own train. A train that fits whole disassembles as Create always did.
 */
@Mixin(value = Train.class, remap = false)
public abstract class TrainDisassemblyMixin {
    @ModifyReturnValue(method = "canDisassemble", at = @At("RETURN"))
    private boolean railbound$whatFitsMayGo(boolean whole) {
        return whole || TrainCoupling.fitAtStation((Train) (Object) this) > 0;
    }

    @Inject(method = "disassemble", at = @At("HEAD"))
    private void railbound$leaveTheRestBehind(Direction assemblyDirection, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        TrainCoupling.leaveBehindWhatDoesNotFit((Train) (Object) this);
    }
}
