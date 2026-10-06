package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import net.createmod.catnip.data.Couple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A bogey's two wheel points (in the train's direction of travel) and whether it is its carriage's own first bogey,
 * the one the carriage's blocks are built around: turned round with the carriage when a train is reversed.
 */
@Mixin(value = CarriageBogey.class, remap = false)
public interface CarriageBogeyAccessor {
    @Accessor("points")
    Couple<TravellingPoint> getPoints();

    @Accessor("points")
    void setPoints(Couple<TravellingPoint> points);

    @Accessor("isLeading")
    boolean isLeading();

    @Accessor("isLeading")
    void setLeading(boolean leading);
}
