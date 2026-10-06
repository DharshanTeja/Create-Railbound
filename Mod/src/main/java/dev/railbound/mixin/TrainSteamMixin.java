package dev.railbound.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.steam.LocoSteam;
import dev.railbound.steam.LocoWhistle;
import dev.railbound.steam.SteamTrain;
import dev.railbound.steam.TrainPower;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Steam locos set their train's speeds: their boilers tick with the train, and Create's top speed, curve speed and
 * acceleration (blocks per tick: Create's config values divided by 20 and 400) come from them. Such a train burns
 * its locos' own coal, never Create's fuel from cargo.
 */
@Mixin(value = Train.class, remap = false)
public abstract class TrainSteamMixin implements SteamTrain {
    @Unique
    @Nullable
    private TrainPower.Limits railbound$limits;

    @Override
    public Optional<TrainPower.Limits> railbound$limits() {
        return Optional.ofNullable(railbound$limits);
    }

    @Override
    public void railbound$setLimits(@Nullable TrainPower.Limits limits) {
        railbound$limits = limits;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void railbound$tickBoilers(Level level, CallbackInfo ci) {
        if (!level.isClientSide) {
            LocoSteam.tick((Train) (Object) this, level.getGameTime());
            LocoWhistle.tick((Train) (Object) this, level);
        }
    }

    @ModifyReturnValue(method = "maxSpeed", at = @At("RETURN"))
    private float railbound$steamTopSpeed(float original) {
        return railbound$limits == null ? original : (float) (railbound$limits.topSpeed() / 20);
    }

    @ModifyReturnValue(method = "maxTurnSpeed", at = @At("RETURN"))
    private float railbound$steamCurveSpeed(float original) {
        return railbound$limits == null ? original : (float) (railbound$limits.curveSpeed() / 20);
    }

    @ModifyReturnValue(method = "acceleration", at = @At("RETURN"))
    private float railbound$steamAcceleration(float original) {
        return railbound$limits == null ? original : (float) (railbound$limits.acceleration() / 400);
    }

    /** Create only sounds the horn for trains carrying a Steam Whistle block; a steam loco has its own whistle. */
    @Inject(method = "determineHonk", at = @At("TAIL"))
    private void railbound$locoWhistle(Level level, CallbackInfo ci) {
        Train train = (Train) (Object) this;
        if (train.lowHonk == null && LocoSteam.hasLoco(train, level)) {
            train.lowHonk = false;
            train.honkPitch = 0;
        }
    }

    @Inject(method = "burnFuel", at = @At("HEAD"), cancellable = true)
    private void railbound$noCargoFuel(CallbackInfo ci) {
        if (railbound$limits != null) {
            ci.cancel();
        }
    }
}
