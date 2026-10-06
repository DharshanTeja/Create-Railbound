package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.coupling.CarriageFlip;
import dev.railbound.coupling.CarriageReversal;
import net.createmod.catnip.data.Couple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Drivers in a reversed carriage: its conductor seats and controls face back along the train, so a conductor there
 * drives the train backwards, and a player at its controls moves the train the way they face.
 */
@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class CarriageDriverMixin {
    @Shadow
    public abstract Carriage getCarriage();

    @ModifyReturnValue(method = "checkConductors", at = @At("RETURN"))
    private Couple<Boolean> railbound$conductorsForTrain(Couple<Boolean> own) {
        Carriage carriage = getCarriage();
        return carriage != null && CarriageReversal.reversed(carriage)
                ? CarriageFlip.forTrain(own.getFirst(), own.getSecond(), true) : own;
    }

    @ModifyVariable(method = "control", at = @At(value = "LOAD", ordinal = 0), name = "inverted")
    private boolean railbound$controlsForTrain(boolean inverted) {
        Carriage carriage = getCarriage();
        return carriage != null && CarriageReversal.reversed(carriage) ? !inverted : inverted;
    }
}
