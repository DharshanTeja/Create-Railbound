package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.coupling.CarriageFlip;
import dev.railbound.coupling.CarriageReversal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Trains of ours with a cab at each end: one driver anywhere drives the train from whichever end has controls facing
 * the way it goes (each loco still fires its own boiler). A reversed carriage goes back to blocks at a station from its
 * own first bogey.
 */
@Mixin(value = Train.class, remap = false)
public abstract class TrainReversalMixin {
    @ModifyReturnValue(method = "hasForwardConductor", at = @At("RETURN"))
    private boolean railbound$sharedDriverForwards(boolean original) {
        Train train = (Train) (Object) this;
        return original || CarriageReversal.sharesDriver(train)
                && CarriageFlip.canDrive(false, CarriageReversal.anyDriver(train), CarriageReversal.controlsFacing(train, true));
    }

    @ModifyReturnValue(method = "hasBackwardConductor", at = @At("RETURN"))
    private boolean railbound$sharedDriverBackwards(boolean original) {
        Train train = (Train) (Object) this;
        return original || CarriageReversal.sharesDriver(train)
                && CarriageFlip.canDrive(false, CarriageReversal.anyDriver(train), CarriageReversal.controlsFacing(train, false));
    }

    @ModifyArg(method = "disassemble", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;I)Lnet/minecraft/core/BlockPos;"),
            index = 1)
    private int railbound$fromOwnFirstBogey(int distance, @Local(name = "carriage") Carriage carriage,
                                            @Local(name = "backwards") boolean backwards) {
        return CarriageFlip.disassemblyDistance(distance, carriage.bogeySpacing, backwards, CarriageReversal.reversed(carriage));
    }
}
