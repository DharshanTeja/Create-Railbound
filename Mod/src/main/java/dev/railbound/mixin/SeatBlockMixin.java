package dev.railbound.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import dev.railbound.carriage.SeatPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Sitting down on a parked carriage seat (and being re-seated after disassembly): half a block higher. */
@Mixin(value = SeatBlock.class, remap = false)
public abstract class SeatBlockMixin {

    @ModifyArg(method = "sitDown", index = 1, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/contraptions/actors/seat/SeatEntity;setPos(DDD)V"))
    private static double railbound$sitOnTheCushion(double y, @Local(argsOnly = true) Level level,
                                                    @Local(argsOnly = true) BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof SeatPartBlock ? y + SeatPartBlock.SIT_OFFSET : y;
    }
}
