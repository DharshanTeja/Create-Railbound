package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.railbound.carriage.StepPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create lets you climb a moving train's climbable block only while your feet are in it. Carriage steps are climbed
 * from outside, so a step beside your feet (or beside the block above them) whose ladder faces you counts instead.
 */
@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class ContraptionColliderStepMixin {

    @ModifyExpressionValue(method = "collideEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;",
            ordinal = 0))
    private static BlockPos railbound$climbTheStepLadder(BlockPos feet,
                                                         @Local(argsOnly = true) AbstractContraptionEntity entity) {
        Contraption contraption = entity.getContraption();
        if (contraption == null || contraption.getBlocks().containsKey(feet)) {
            return feet;
        }
        for (int dy = 0; dy <= 1; dy++) {
            for (Direction towardsStep : Direction.Plane.HORIZONTAL) {
                BlockPos step = feet.above(dy).relative(towardsStep);
                StructureBlockInfo info = contraption.getBlocks().get(step);
                if (info != null && StepPartBlock.faces(info.state(), towardsStep.getOpposite())) {
                    return step;
                }
            }
        }
        return feet;
    }
}
