package dev.railbound.mixin;

import com.simibubi.create.content.trains.station.StationBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether the train at a station arrived backwards, which Create keeps to itself. */
@Mixin(value = StationBlockEntity.class, remap = false)
public interface StationBlockEntityAccessor {
    @Accessor("trainBackwards")
    boolean railbound$trainBackwards();
}
