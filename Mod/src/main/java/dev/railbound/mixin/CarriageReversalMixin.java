package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import dev.railbound.coupling.CarriageReversal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A carriage loaded or synced while reversed in its train: its own first bogey (the one its blocks are built around)
 * is the train's second. Create's constructor always takes the first; the reversed mark travels in the bogey data.
 */
@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageReversalMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void railbound$ownFirstBogey(CarriageBogey bogey1, CarriageBogey bogey2, int bogeySpacing, CallbackInfo ci) {
        if (bogey2 != null && CarriageReversal.marked(bogey1)) {
            ((CarriageBogeyAccessor) bogey1).setLeading(false);
            ((CarriageBogeyAccessor) bogey2).setLeading(true);
        }
    }
}
