package dev.railbound.trainset.load;

import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** The designs currently in effect: set by the server reload listener and by the client sync payload. */
public final class TrainsetDesigns {
    private static volatile Map<ResourceLocation, TrainsetDesign> designs = Map.of();
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private TrainsetDesigns() {}

    public static Map<ResourceLocation, TrainsetDesign> all() {
        return designs;
    }

    public static Optional<TrainsetDesign> get(ResourceLocation id) {
        return Optional.ofNullable(designs.get(id));
    }

    public static void replace(Map<ResourceLocation, TrainsetDesign> newDesigns) {
        designs = Collections.unmodifiableMap(new TreeMap<>(newDesigns));
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    /** Called after every {@link #replace}, e.g. to refresh client-side caches built from the designs. */
    public static void addChangeListener(Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }
}
