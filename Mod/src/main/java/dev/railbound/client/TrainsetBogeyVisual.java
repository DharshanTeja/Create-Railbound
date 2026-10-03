package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** The Flywheel (instanced) version of {@link TrainsetBogeyRenderer}. */
public class TrainsetBogeyVisual implements BogeyVisual {
    private final TransformedInstance frame;
    private final TransformedInstance frontWheels;
    private final TransformedInstance rearWheels;

    public TrainsetBogeyVisual(VisualizationContext ctx, TrainsetBogeyModels models) {
        // Create's own bogeys use smooth-lit models; plain ones come out black inside a moving train's light volume
        frame = ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(models.frame())).createInstance();
        var wheels = ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(models.wheels()));
        frontWheels = wheels.createInstance();
        rearWheels = wheels.createInstance();
    }

    private List<TransformedInstance> all() {
        return List.of(frame, frontWheels, rearWheels);
    }

    @Override
    public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
        frame.setTransform(poseStack).setChanged();
        frontWheels.setTransform(poseStack)
                .translate(0, TrainsetBogeyModels.AXLE_HEIGHT, -TrainsetBogeyModels.AXLE_OFFSET)
                .rotateXDegrees(wheelAngle)
                .setChanged();
        rearWheels.setTransform(poseStack)
                .translate(0, TrainsetBogeyModels.AXLE_HEIGHT, TrainsetBogeyModels.AXLE_OFFSET)
                .rotateXDegrees(wheelAngle)
                .setChanged();
    }

    @Override
    public void hide() {
        all().forEach(instance -> instance.setZeroTransform().setChanged());
    }

    @Override
    public void updateLight(int packedLight) {
        all().forEach(instance -> instance.light(packedLight).setChanged());
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        all().forEach(consumer);
    }

    @Override
    public void delete() {
        all().forEach(TransformedInstance::delete);
    }
}
