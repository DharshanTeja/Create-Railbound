package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import dev.railbound.coupling.CarriageReversal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A reversed carriage's bogeys are drawn turned with their carriage. Create aims a bogey from its train-leading wheel
 * to its trailing one, which for a reversed carriage is backwards, so its wheels would spin against the motion.
 */
@Mixin(value = CarriageBogey.class, remap = false)
public abstract class CarriageBogeyAnglesMixin {
    @Shadow
    public Carriage carriage;

    @ModifyArg(method = "updateAngles", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/LerpedFloat;setValue(D)V", ordinal = 1))
    private double railbound$pitch(double pitch) {
        return carriage != null && CarriageReversal.reversed(carriage) ? -pitch : pitch;
    }

    @ModifyArg(method = "updateAngles", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/LerpedFloat;setValue(D)V", ordinal = 2))
    private double railbound$yaw(double yaw) {
        return carriage != null && CarriageReversal.reversed(carriage) ? yaw + 180 : yaw;
    }
}
