package dev.railbound.trainset.load;

import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** The designs currently in effect: set by the server reload listener and by the client sync payload. */
public final class TrainsetDesigns {
    private static volatile Map<ResourceLocation, ParsedDesign> designs = Map.of();
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private TrainsetDesigns() {}

    public static Map<ResourceLocation, ParsedDesign> all() {
        return designs;
    }

    public static Optional<ParsedDesign> get(@Nullable ResourceLocation id) {
        return id == null ? Optional.empty() : Optional.ofNullable(designs.get(id));
    }

    /** The plain designs, for syncing to clients. */
    public static Map<ResourceLocation, TrainsetDesign> rawDesigns() {
        Map<ResourceLocation, TrainsetDesign> raw = new TreeMap<>();
        designs.forEach((id, parsed) -> raw.put(id, parsed.design()));
        return raw;
    }

    /** Only pass validated designs. */
    public static void replace(Map<ResourceLocation, TrainsetDesign> newDesigns) {
        Map<ResourceLocation, ParsedDesign> parsed = new TreeMap<>();
        newDesigns.forEach((id, design) -> parsed.put(id, ParsedDesign.of(design)));
        designs = Collections.unmodifiableMap(parsed);
        CHANGE_LISTENERS.forEach(Runnable::run);
    }

    /** Called after every {@link #replace}, e.g. to refresh client-side caches built from the designs. */
    public static void addChangeListener(Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }
}
