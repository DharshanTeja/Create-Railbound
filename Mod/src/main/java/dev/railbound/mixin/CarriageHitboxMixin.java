package dev.railbound.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.carriage.AnchorBlock;
import dev.railbound.carriage.CarriageBounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Create gives every carriage a square hitbox as wide as the carriage is long, so it covers the carriage at any
 * angle. Our carriages instead get a box that follows them (see {@link CarriageBounds}); Create's own trains keep
 * theirs.
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class CarriageHitboxMixin {
    /** Each of our carriages' block extent; empty for contraptions that are not ours. */
    @Unique
    private static final Map<Contraption, Optional<AABB>> railbound$extents = new WeakHashMap<>();

    @Inject(method = "setPos", at = @At("TAIL"))
    private void railbound$fitHitbox(double x, double y, double z, CallbackInfo ci) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        Contraption contraption = self.getContraption();
        if (!(self instanceof CarriageContraptionEntity) || contraption == null) {
            return;
        }
        Optional<AABB> extent = railbound$extents.computeIfAbsent(contraption, c ->
                c.getBlocks().values().stream().anyMatch(info -> info.state().getBlock() instanceof AnchorBlock)
                        ? Optional.of(CarriageBounds.extent(c.getBlocks().keySet())) : Optional.empty());
        extent.ifPresent(local -> ((Entity) (Object) this).setBoundingBox(
                CarriageBounds.world(local, v -> self.toGlobalVector(v, 1))));
    }
}
