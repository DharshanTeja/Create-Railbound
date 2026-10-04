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

import java.util.List;

/**
 * The water crane above its block: two more lengths of column, then the cap and telescopic arm at
 * {@link WaterCrane#ARM_HEIGHT}, turned and slid out to its pose: parked along the track, or over a loco's filler with
 * the hose let down onto it. Every arm part pivots on the column's axis (x 8, z 8).
 */
public class WaterCraneRenderer implements BlockEntityRenderer<WaterCraneBlockEntity> {
    public static final ModelResourceLocation ARM = model("water_crane_arm");
    public static final ModelResourceLocation COLUMN = model("water_crane_column");
    /** The inner arm, modelled one block long from the pivot and stretched to the reach. */
    public static final ModelResourceLocation BOOM = model("water_crane_boom");
    public static final ModelResourceLocation TIP = model("water_crane_tip");
    /** The hose, modelled one block long down from the collar and stretched to the filler. */
    public static final ModelResourceLocation HOSE = model("water_crane_hose");
    public static final ModelResourceLocation NOZZLE = model("water_crane_nozzle");
    public static final List<ModelResourceLocation> MODELS = List.of(ARM, COLUMN, BOOM, TIP, HOSE, NOZZLE);

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(Railbound.rl("block/" + name));
    }

    public WaterCraneRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WaterCraneBlockEntity crane, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        for (int length = 2; length < 4; length++) {
            pose.pushPose();
            pose.translate(0, length, 0);
            draw(pose, buffers, crane, COLUMN, light, overlay);
            pose.popPose();
        }
        WaterCrane.Aim aim = crane.aim(partialTick);
        float facing = -crane.getBlockState().getValue(WaterCraneBlock.FACING).toYRot();
        pose.pushPose();
        pose.translate(0.5, WaterCrane.ARM_HEIGHT, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing + (float) aim.angle()));

        part(pose, buffers, crane, ARM, light, overlay, 0, 0, 1, 1);
        part(pose, buffers, crane, BOOM, light, overlay, 0, 0, 1, (float) aim.reach());
        part(pose, buffers, crane, TIP, light, overlay, aim.reach(), 0, 1, 1);
        part(pose, buffers, crane, HOSE, light, overlay, aim.reach(), -WaterCrane.COLLAR, (float) aim.hose(), 1);
        part(pose, buffers, crane, NOZZLE, light, overlay, aim.reach(), -WaterCrane.COLLAR - aim.hose(), 1, 1);
        pose.popPose();
    }

    /** An arm part moved out along the arm and down, and stretched down and along, about the pivot. */
    private static void part(PoseStack pose, MultiBufferSource buffers, WaterCraneBlockEntity crane, ModelResourceLocation model,
                             int light, int overlay, double out, double down, float stretchDown, float stretchOut) {
        pose.pushPose();
        pose.translate(0, down, out);
        pose.scale(1, stretchDown, stretchOut);
        pose.translate(-0.5, 0, -0.5);
        draw(pose, buffers, crane, model, light, overlay);
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, WaterCraneBlockEntity crane, ModelResourceLocation id,
                             int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), crane.getBlockState(), model, 1, 1, 1, light, overlay,
                ModelData.EMPTY, RenderType.cutout());
    }

    @Override
    public AABB getRenderBoundingBox(WaterCraneBlockEntity crane) {
        return new AABB(crane.getBlockPos()).inflate(4, 0, 4).expandTowards(0, 5, 0);
    }
}
