package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.railbound.Railbound;
import dev.railbound.carriage.DoorSlide;
import dev.railbound.trainset.design.CarriageSize;
import dev.railbound.trainset.design.DoorSpec;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Draws a design's model in design block space: body, full-bright lamps and door leaves slid open by the given
 * progress. A design without a model gets a grey box.
 */
public final class CarriageDrawer {
    private static final ResourceLocation PLACEHOLDER = Railbound.rl("block/carriage_frame");
    private static final RenderType SOLID = RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    private CarriageDrawer() {}

    public static boolean hasModel(@Nullable ResourceLocation id) {
        return id != null && TrainsetModels.get(id, TrainsetModels.BODY).isPresent();
    }

    public static void draw(ResourceLocation id, ParsedDesign design, PoseStack pose, MultiBufferSource buffers,
                            int light, int overlay, ToDoubleFunction<DoorSpec> doorProgress) {
        VertexConsumer solid = buffers.getBuffer(SOLID);
        Optional<BakedModel> body = TrainsetModels.get(id, TrainsetModels.BODY);
        if (body.isPresent()) {
            renderModel(pose, solid, body.get(), light, overlay, SOLID);
        } else {
            if (WARNED.add(id)) {
                Railbound.LOGGER.warn("Trainset design {} has no model; drawing a placeholder box", id);
            }
            placeholderBox(pose, solid, design.design().size(), light, overlay);
        }
        TrainsetModels.get(id, TrainsetModels.LAMPS).ifPresent(lamps -> renderModel(pose,
                buffers.getBuffer(RenderType.cutout()), lamps, LightTexture.FULL_BRIGHT, overlay, RenderType.cutout()));

        int length = design.design().size().length();
        for (DoorSpec door : design.design().doors()) {
            Optional<BakedModel> leaf = TrainsetModels.get(id, door.part());
            if (leaf.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0, 0, DoorSlide.direction(door.pos().getZ(), length)
                    * DoorSlide.distance((float) doorProgress.applyAsDouble(door)));
            renderModel(pose, solid, leaf.get(), light, overlay, SOLID);
            pose.popPose();
        }
    }

    private static void renderModel(PoseStack pose, VertexConsumer consumer, BakedModel model, int light, int overlay,
                                    RenderType renderType) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model,
                1, 1, 1, light, overlay, ModelData.EMPTY, renderType);
    }

    /** A grey box over the design's bounds. */
    private static void placeholderBox(PoseStack pose, VertexConsumer consumer, CarriageSize size, int light, int overlay) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(PLACEHOLDER);
        float x0 = -1, y0 = 0.875f, z0 = 0, x1 = 2, y1 = size.height(), z1 = size.length();
        PoseStack.Pose last = pose.last();
        float[][][] faces = {
                {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}}, // up
                {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}}, // down
                {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}}, // north
                {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}}, // south
                {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}}, // east
                {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}}, // west
        };
        float[][] normals = {{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
        float[][] uv = {{sprite.getU0(), sprite.getV0()}, {sprite.getU0(), sprite.getV1()},
                {sprite.getU1(), sprite.getV1()}, {sprite.getU1(), sprite.getV0()}};
        for (int f = 0; f < faces.length; f++) {
            for (int v = 0; v < 4; v++) {
                float[] p = faces[f][v];
                consumer.addVertex(last, p[0], p[1], p[2]).setColor(0xFFB0B0B0).setUv(uv[v][0], uv[v][1])
                        .setOverlay(overlay).setLight(light).setNormal(last, normals[f][0], normals[f][1], normals[f][2]);
            }
        }
    }
}
