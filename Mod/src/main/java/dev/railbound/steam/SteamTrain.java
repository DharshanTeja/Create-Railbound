package dev.railbound.steam;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Added to Create's Train: the speed limits its steam locos set this tick, if it has any. */
public interface SteamTrain {
    Optional<TrainPower.Limits> railbound$limits();

    void railbound$setLimits(@Nullable TrainPower.Limits limits);
}
