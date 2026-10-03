package dev.railbound.trainset.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.railbound.Railbound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads data/<namespace>/railbound/trainsets/*.json on every datapack (re)load. */
public final class TrainsetDesignManager extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "railbound/trainsets";

    public TrainsetDesignManager() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        DesignLoader.LoadResult result = DesignLoader.load(jsons);
        result.errors().forEach(error -> Railbound.LOGGER.error("Skipping trainset design {}", error));
        TrainsetDesigns.replace(result.designs());
        Railbound.LOGGER.info("Loaded {} trainset design(s)", result.designs().size());
    }
}
