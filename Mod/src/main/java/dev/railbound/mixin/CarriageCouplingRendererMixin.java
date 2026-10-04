package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import dev.railbound.client.CouplingRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Create skips a coupling with no anchor: where both carriages have knuckle couplers we draw those instead of the chain. */
@Mixin(value = CarriageCouplingRenderer.class, remap = false)
public abstract class CarriageCouplingRendererMixin {

    @ModifyExpressionValue(method = "renderAll", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/data/Couple;getSecond()Ljava/lang/Object;", ordinal = 0))
    private static Object railbound$hideChain(Object anchor, @Local(ordinal = 0) Carriage carriage,
                                              @Local(ordinal = 1) Carriage carriage2) {
        return CouplingRenderer.drawsOwn(Minecraft.getInstance().level, carriage, carriage2) ? null : anchor;
    }
}
