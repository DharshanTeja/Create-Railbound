package dev.railbound.steam;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import dev.railbound.network.LocoGaugesPayload;
import dev.railbound.trainset.design.PerformanceSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runs every steam loco's boiler with its train, server side, wherever the train is, and works out the speed limits
 * the train then runs under. Create's own speeds apply to trains without our locos.
 */
public final class LocoSteam {
    /** Used when a loco's design is not loaded: a modest tank engine. */
    static final PerformanceSpec FALLBACK = new PerformanceSpec(20, 10, 2);

    private LocoSteam() {}

    /** Ticks between gauge updates sent to watching players. */
    static final int GAUGE_EVERY = 10;

    public static void tick(Train train, long gameTime) {
        List<TrainPower.Loco> locos = new ArrayList<>();
        for (Carriage carriage : train.carriages) {
            TrainCargoManager storage = carriage.storage;
            if (storage == null) {
                continue;
            }
            for (Map.Entry<BlockPos, MountedItemStorage> entry : storage.getAllItemStorages().entrySet()) {
                if (!(entry.getValue() instanceof BunkerItemStorage bunker)) {
                    continue;
                }
                PerformanceSpec performance = performanceOf(bunker.designId());
                MountedFluidStorage fluid = storage.getFluids().storages.get(entry.getKey());
                if (fluid instanceof BunkerFluidStorage water) {
                    // Create's speeds are in blocks per tick; the design's in blocks per second
                    bunker.tick(TrainPower.effort(train.speed * 20, train.targetSpeed * 20, performance.topSpeed()), water);
                    CarriageContraptionEntity entity = carriage.anyAvailableEntity();
                    if (entity != null && gameTime % GAUGE_EVERY == 0) {
                        PacketDistributor.sendToPlayersTrackingEntity(entity,
                                new LocoGaugesPayload(entity.getId(), SteamGauges.of(bunker, water)));
                    }
                }
                locos.add(new TrainPower.Loco(performance.topSpeed(), performance.curveSpeed(),
                        performance.acceleration(), bunker.pull()));
            }
        }
        ((SteamTrain) train).railbound$setLimits(TrainPower.limits(locos).orElse(null));
    }

    /** Whether any carriage of the train, as loaded in this level, is one of our steam locos (it has a bunker). */
    public static boolean hasLoco(Train train, Level level) {
        for (Carriage carriage : train.carriages) {
            Carriage.DimensionalCarriageEntity dce = carriage.getDimensionalIfPresent(level.dimension());
            CarriageContraptionEntity entity = dce == null ? null : dce.entity.get();
            if (entity != null && entity.getContraption() != null && entity.getContraption().getBlocks().values().stream()
                    .anyMatch(info -> info.state().getBlock() instanceof BunkerBlock)) {
                return true;
            }
        }
        return false;
    }

    static PerformanceSpec performanceOf(@Nullable ResourceLocation designId) {
        return Optional.ofNullable(designId).flatMap(TrainsetDesigns::get)
                .flatMap(design -> design.design().performance())
                .orElse(FALLBACK);
    }
}
