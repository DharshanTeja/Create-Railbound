package dev.railbound.client;

import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Draws a Railbound bogey when Flywheel is off (parked or on a train): frame plus two spinning wheelsets. */
public record TrainsetBogeyRenderer(TrainsetBogeyModels models) implements BogeyRenderer {
    private static final RenderType TYPE = RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);

    @Override
    public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay, boolean inContraption) {
        // Create's carriage renderer hands its entity light over as "overlay"; a bad overlay turns the bogey black
        // under shader packs (Flywheel switches off with shaders, so this renderer draws assembled bogeys then)
        overlay = OverlayTexture.NO_OVERLAY;
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        draw(poseStack, consumer, models.frame().get(), light, overlay);
        for (float z : new float[] {-models.axleOffset(), models.axleOffset()}) {
            poseStack.pushPose();
            poseStack.translate(0, TrainsetBogeyModels.AXLE_HEIGHT, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(wheelAngle));
            draw(poseStack, consumer, models.wheels().get(), light, overlay);
            poseStack.popPose();
        }
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, BakedModel model, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model,
                1, 1, 1, light, overlay, ModelData.EMPTY, TYPE);
    }
}
