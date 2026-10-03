package dev.railbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.railbound.carriage.AnchorBlock;
import dev.railbound.carriage.AnchorBlockEntity;
import dev.railbound.carriage.CarriageFootprint;
import dev.railbound.carriage.CarriageTransform;
import dev.railbound.carriage.DoorPartBlockEntity;
import dev.railbound.trainset.design.CarriageSize;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Draws the whole carriage from its anchor, turned to the carriage's facing. The same renderer runs for a parked
 * carriage and inside Create's moving train, where the door block entities are the contraption's copies.
 */
public class AnchorRenderer implements BlockEntityRenderer<AnchorBlockEntity> {

    public AnchorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AnchorBlockEntity anchor, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        Optional<ParsedDesign> found = TrainsetDesigns.get(anchor.designId());
        if (found.isEmpty()) {
            return;
        }
        ParsedDesign design = found.get();
        Direction facing = anchor.getBlockState().getValue(AnchorBlock.FACING);
        BlockPos anchorLocal = design.anchor();
        Level level = anchor.getLevel();

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(CarriageTransform.yRotation(facing)));
        pose.translate(-anchorLocal.getX() - 0.5, -anchorLocal.getY(), -anchorLocal.getZ() - 0.5);
        CarriageDrawer.draw(anchor.designId(), design, pose, buffers, light, overlay, door -> {
            BlockPos doorPos = CarriageFootprint.worldPos(design, anchor.getBlockPos(), facing, door.pos());
            return level != null && level.getBlockEntity(doorPos) instanceof DoorPartBlockEntity state
                    ? state.progress(partialTick) : 0;
        });
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AnchorBlockEntity anchor) {
        Optional<ParsedDesign> design = TrainsetDesigns.get(anchor.designId());
        if (design.isEmpty()) {
            return new AABB(anchor.getBlockPos());
        }
        CarriageSize size = design.get().design().size();
        Direction facing = anchor.getBlockState().getValue(AnchorBlock.FACING);
        Vec3 a = CarriageTransform.toAnchorRelative(design.get(), facing, -1, 0, -0.5);
        Vec3 b = CarriageTransform.toAnchorRelative(design.get(), facing, 2, size.height(), size.length() + 0.5);
        return new AABB(a, b).move(Vec3.atLowerCornerOf(anchor.getBlockPos())).inflate(0.5);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
