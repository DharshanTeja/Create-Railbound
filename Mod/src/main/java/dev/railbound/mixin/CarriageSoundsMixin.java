package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import dev.railbound.steam.LocoVoices;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Create sounds a whole train from its first carriage: its steam chuffing, and its whistle. On a train with locos of
 * ours, each loco chuffs from its own chimney instead (AnchorMovementBehaviour), so Create's chuff is muted, and the
 * whistle sounds from the loco nearest the listener: riding either loco of a push-pull train, you hear that one.
 * Create also leaves a carriage's whistle looping for good if another carriage takes over leading the train's sound
 * while it sounds (a carriage ahead comes into range, or a join reorders the train): a carriage that no longer leads
 * stops its own whistle.
 * Listed with the common mixins although only clients load CarriageSounds, so MixinsApplyTest checks it still fits.
 */
@Mixin(value = CarriageSounds.class, remap = false)
public abstract class CarriageSoundsMixin {
    private static final String PLAY_AT =
            "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFZ)V";
    private static final String SET_LOCATION =
            "Lcom/simibubi/create/content/trains/entity/CarriageSounds$LoopingSound;setLocation(Lnet/minecraft/world/phys/Vec3;)V";

    /** Create's whistle loop, of a type private to its package. */
    @Unique
    private static final Field RAILBOUND$HONK = railbound$honkField();

    @Shadow
    CarriageContraptionEntity entity;

    @Unique
    private static Field railbound$honkField() {
        try {
            Field field = CarriageSounds.class.getDeclaredField("sharedHonkSound");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Create's CarriageSounds changed", e);
        }
    }

    /** Create's path for a carriage that doesn't lead the train's sound: its own whistle must not keep looping. */
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/entity/CarriageSounds;finalizeSharedVolume(F)V", shift = At.Shift.AFTER))
    private void railbound$quietWhenNotLeading(CallbackInfo ci) {
        try {
            if (RAILBOUND$HONK.get(this) instanceof SoundInstance honk) {
                Minecraft.getInstance().getSoundManager().stop(honk);
                RAILBOUND$HONK.set(this, null);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = PLAY_AT, ordinal = 0))
    private boolean railbound$chuff(AllSoundEvents.SoundEntry sound, Level level, Vec3 at, float volume, float pitch, boolean fade) {
        return !railbound$ownVoices();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = PLAY_AT, ordinal = 1))
    private boolean railbound$beat(AllSoundEvents.SoundEntry sound, Level level, Vec3 at, float volume, float pitch, boolean fade) {
        return !railbound$ownVoices();
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = PLAY_AT, ordinal = 3), index = 1)
    private Vec3 railbound$whistleEnd(Vec3 at) {
        return railbound$whistle(at);
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = PLAY_AT, ordinal = 4), index = 1)
    private Vec3 railbound$whistleStart(Vec3 at) {
        return railbound$whistle(at);
    }

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = SET_LOCATION))
    private Vec3 railbound$whistleHeld(Vec3 at) {
        return railbound$whistle(at);
    }

    private boolean railbound$ownVoices() {
        return entity.trainId != null && LocoVoices.hasLoco(entity.trainId, entity.level().getGameTime());
    }

    private Vec3 railbound$whistle(Vec3 at) {
        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera == null || entity.trainId == null) {
            return at;
        }
        return LocoVoices.nearestWhistle(entity.trainId, camera.getEyePosition(), entity.level().getGameTime()).orElse(at);
    }
}
