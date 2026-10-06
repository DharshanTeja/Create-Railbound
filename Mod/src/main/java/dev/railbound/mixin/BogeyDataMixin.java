package dev.railbound.mixin;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import dev.railbound.coupling.CarriageReversal;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A bogey put back as a block forgets that its carriage ran reversed, so the next train built on it starts fresh. */
@Mixin(value = AbstractBogeyBlockEntity.class, remap = false)
public abstract class BogeyDataMixin {
    @ModifyVariable(method = "setBogeyData", at = @At("HEAD"), argsOnly = true)
    private CompoundTag railbound$forgetReversal(CompoundTag data) {
        return CarriageReversal.unmarked(data);
    }
}
