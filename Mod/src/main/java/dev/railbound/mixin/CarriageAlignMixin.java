package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.Carriage;
import dev.railbound.coupling.CarriageReversal;
import net.createmod.catnip.data.Couple;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Places a reversed carriage's blocks. Create puts a carriage's entity on its train-leading bogey, facing from there to
 * the trailing one; a reversed carriage is built around its own first bogey, the train-trailing one, and faces the
 * other way. The anchors themselves stay in train order, so the spacing Create keeps between carriages is untouched.
 */
@Mixin(targets = "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity", remap = false)
public abstract class CarriageAlignMixin {
    @Shadow
    @Final
    Carriage this$0;

    @Shadow
    public Couple<Vec3> rotationAnchors;

    @ModifyVariable(method = "alignEntity", at = @At("STORE"), name = "positionVec")
    private Vec3 railbound$faceFrom(Vec3 leading) {
        return CarriageReversal.reversed(this$0) ? rotationAnchors.getSecond() : leading;
    }

    @ModifyVariable(method = "alignEntity", at = @At("STORE"), name = "coupledVec")
    private Vec3 railbound$faceTo(Vec3 trailing) {
        return CarriageReversal.reversed(this$0) ? rotationAnchors.getFirst() : trailing;
    }

    @ModifyArg(method = "alignEntity", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;setPos(Lnet/minecraft/world/phys/Vec3;)V"))
    private Vec3 railbound$stand(Vec3 position) {
        return CarriageReversal.reversed(this$0) && this$0.isOnTwoBogeys() && rotationAnchors.getSecond() != null
                ? rotationAnchors.getSecond() : position;
    }
}
