package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.railbound.carriage.StepClimbing;
import dev.railbound.carriage.StepPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Carriage steps are climbed from outside: touching a step's ladder (its outer face, below the floor) counts as being
 * on a ladder, though your feet are in the block beside it or below it, never inside it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityStepMixin {

    @ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
    private boolean railbound$climbTheStepLadder(boolean original) {
        Entity self = (Entity) (Object) this;
        if (original || self.isSpectator()) {
            return original;
        }
        BlockPos feet = self.blockPosition();
        for (int dy = 0; dy <= 1; dy++) {
            for (Direction towardsStep : Direction.Plane.HORIZONTAL) {
                BlockPos step = feet.above(dy).relative(towardsStep);
                Direction outward = towardsStep.getOpposite();
                BlockState state = self.level().getBlockState(step);
                if (StepPartBlock.faces(state, outward)
                        && StepClimbing.touchesLadder(self.getBoundingBox(), step, outward, state.getValue(StepPartBlock.HIGH))) {
                    return true;
                }
            }
        }
        return false;
    }
}
