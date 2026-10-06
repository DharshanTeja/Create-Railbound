package dev.railbound.mixin;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.AbstractStationScreen;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.railbound.coupling.TrainCoupling;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The station's Disassemble button says when only part of the train will go: the carriages that fit the straight are
 * disassembled and the rest stay behind as their own train (see {@code TrainDisassemblyMixin}).
 * Listed with the common mixins although only clients load the screen, so MixinsApplyTest checks it still fits.
 */
@Mixin(value = StationScreen.class, remap = false)
public abstract class StationScreenMixin extends AbstractStationScreen {
    @Shadow
    private IconButton disassembleTrainButton;

    private StationScreenMixin(StationBlockEntity be, GlobalStation station) {
        super(be, station);
    }

    @Inject(method = "updateAssemblyTooltip", at = @At("TAIL"))
    private void railbound$sayWhatFits(String key, CallbackInfo ci) {
        Train train = displayedTrain == null ? null : displayedTrain.get();
        if (key != null || train == null) {
            return;
        }
        int fit = TrainCoupling.fitAtStation(train, ((StationBlockEntityAccessor) blockEntity).railbound$trainBackwards());
        if (fit > 0 && fit < train.carriages.size()) {
            disassembleTrainButton.getToolTip().add(Component.translatable("railbound.station.partial", fit,
                    train.carriages.size()).withStyle(ChatFormatting.GRAY));
        }
    }
}
