package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.Train;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A train's stress between neighbouring carriages, one per gap: resized when a train is split or joined. */
@Mixin(value = Train.class, remap = false)
public interface TrainAccessor {
    @Accessor("stress")
    void setStress(double[] stress);
}
