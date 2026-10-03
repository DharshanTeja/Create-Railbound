package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.railbound.trainset.design.CarriageSize;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.item.TrainsetItem;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Draws a trainset item as a scaled 3D copy of its design (only used when the design has a model). */
public class TrainsetItemRenderer extends BlockEntityWithoutLevelRenderer {
    /** The longest side of the carriage fills this many blocks before the item display scale. */
    private static final float FIT = 2f;

    public TrainsetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        ResourceLocation id = TrainsetItem.designId(stack);
        Optional<ParsedDesign> design = TrainsetDesigns.get(id);
        if (design.isEmpty()) {
            return;
        }
        CarriageSize size = design.get().design().size();
        float scale = FIT / Math.max(size.length(), Math.max(size.height(), 3));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -(0.875 + size.height()) / 2, -size.length() / 2.0);
        CarriageDrawer.draw(id, design.get(), pose, buffers, light, overlay, door -> 0);
        pose.popPose();
    }
}
