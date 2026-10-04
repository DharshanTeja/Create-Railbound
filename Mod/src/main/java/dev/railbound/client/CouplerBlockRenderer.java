package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.railbound.Railbound;
import dev.railbound.carriage.CouplerBlock;
import dev.railbound.carriage.CouplerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The coupler block standing in the world. On a moving train Create draws block entities in its own copy of the
 * carriage; there this draws nothing, because the coupling renderer draws the knuckle reaching out to its neighbour.
 */
public class CouplerBlockRenderer implements BlockEntityRenderer<CouplerBlockEntity> {
    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(Railbound.rl("block/coupler"));

    public CouplerBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CouplerBlockEntity coupler, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                       int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (coupler.getLevel() != mc.level) {
            return;   // on a train
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-coupler.getBlockState().getValue(CouplerBlock.FACING).toYRot()));
        pose.translate(-0.5, 0, -0.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                coupler.getBlockState(), mc.getModelManager().getModel(MODEL), 1, 1, 1, light, overlay, ModelData.EMPTY,
                RenderType.cutout());
        pose.popPose();
    }
}
