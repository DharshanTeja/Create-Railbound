package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.railbound.Railbound;
import dev.railbound.steam.WaterCrane;
import dev.railbound.steam.WaterCraneBlock;
import dev.railbound.steam.WaterCraneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The water crane above its block: two more lengths of column, then the cap and arm with its hose at
 * {@link WaterCrane#ARM_HEIGHT}, turned to the crane's facing and swung out over the track while it fills. The arm
 * model pivots on the column's axis (x 8, z 8).
 */
public class WaterCraneRenderer implements BlockEntityRenderer<WaterCraneBlockEntity> {
    public static final ModelResourceLocation ARM = ModelResourceLocation.standalone(Railbound.rl("block/water_crane_arm"));
    public static final ModelResourceLocation COLUMN = ModelResourceLocation.standalone(Railbound.rl("block/water_crane_column"));

    public WaterCraneRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WaterCraneBlockEntity crane, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BakedModel arm = Minecraft.getInstance().getModelManager().getModel(ARM);
        BakedModel column = Minecraft.getInstance().getModelManager().getModel(COLUMN);
        for (int length = 2; length < 4; length++) {
            pose.pushPose();
            pose.translate(0, length, 0);
            draw(pose, buffers, crane, column, light, overlay);
            pose.popPose();
        }
        float facing = -crane.getBlockState().getValue(WaterCraneBlock.FACING).toYRot();
        pose.pushPose();
        pose.translate(0.5, WaterCrane.ARM_HEIGHT, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing + (float) crane.armAngle(partialTick)));
        pose.translate(-0.5, 0, -0.5);
        draw(pose, buffers, crane, arm, light, overlay);
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, WaterCraneBlockEntity crane, BakedModel model,
                             int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), crane.getBlockState(), model, 1, 1, 1, light, overlay,
                ModelData.EMPTY, RenderType.cutout());
    }

    @Override
    public AABB getRenderBoundingBox(WaterCraneBlockEntity crane) {
        return new AABB(crane.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 4, 0);
    }
}
